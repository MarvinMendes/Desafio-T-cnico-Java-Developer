package com.deroyque.activation.application;

import com.deroyque.activation.domain.ActivationRequest;
import com.deroyque.activation.domain.ActivationStatus;
import com.deroyque.activation.infrastructure.persistence.ActivationRequestRepository;
import com.deroyque.activation.infrastructure.observability.ActivationMetrics;
import com.deroyque.activation.infrastructure.provider.ProviderProperties;
import com.deroyque.activation.infrastructure.provider.ProviderResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessActivationService {

    private final ActivationRequestRepository repository;
    private final ProviderProperties providerProperties;
    private final Clock clock;
    private final ActivationMetrics metrics;

    public ProcessActivationService(
            ActivationRequestRepository repository,
            ProviderProperties providerProperties,
            Clock clock,
            ActivationMetrics metrics
    ) {
        this.repository = repository;
        this.providerProperties = providerProperties;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public Optional<ActivationRequest> claimForProcessing(UUID requestId) {
        Optional<ActivationRequest> found = repository.findById(requestId);
        if (found.isEmpty() || found.get().getStatus().isTerminal()) {
            return Optional.empty();
        }
        ActivationRequest request = found.get();
        if (request.getStatus() == ActivationStatus.PENDING) {
            request.markProcessing(clock);
            repository.flush();
        }
        return Optional.of(request);
    }

    @Transactional
    public ActivationStatus applyProviderResult(UUID requestId, ProviderResult result) {
        ActivationRequest request = repository.findById(requestId).orElse(null);
        if (request == null || request.getStatus().isTerminal()) {
            return request == null ? null : request.getStatus();
        }
        switch (result) {
            case ProviderResult.Success ignored -> request.markCompleted(clock);
            case ProviderResult.ClientError ignored -> request.markFailed(clock);
            case ProviderResult.ServerError ignored -> handleRetryable(request);
            case ProviderResult.RateLimited ignored -> handleRetryable(request);
            case ProviderResult.Timeout ignored -> handleRetryable(request);
        }
        repository.flush();
        metrics.recordStatus(request.getStatus());
        return request.getStatus();
    }

    private void handleRetryable(ActivationRequest request) {
        if (request.getAttemptCount() >= providerProperties.maxAttempts()) {
            request.markFailed(clock);
            return;
        }
        request.markPendingForRetry(clock);
    }
}
