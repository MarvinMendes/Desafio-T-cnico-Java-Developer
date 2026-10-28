package com.deroyque.activation.application;

import com.deroyque.activation.domain.ActivationRequest;
import com.deroyque.activation.domain.ActivationStatus;
import com.deroyque.activation.infrastructure.provider.ActivateProductCommand;
import com.deroyque.activation.infrastructure.provider.ProviderGateway;
import com.deroyque.activation.infrastructure.provider.ProviderProperties;
import com.deroyque.activation.infrastructure.provider.ProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActivationProcessingDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ActivationProcessingDispatcher.class);

    private final ProcessActivationService processActivationService;
    private final ProviderGateway providerGateway;
    private final ProviderProperties providerProperties;

    public ActivationProcessingDispatcher(
            ProcessActivationService processActivationService,
            ProviderGateway providerGateway,
            ProviderProperties providerProperties
    ) {
        this.processActivationService = processActivationService;
        this.providerGateway = providerGateway;
        this.providerProperties = providerProperties;
    }

    @Async
    public void dispatchAsync(UUID requestId) {
        dispatch(requestId);
    }

    public void dispatch(UUID requestId) {
        int maxAttempts = providerProperties.maxAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ActivationStatus status = runAttempt(requestId);
                if (status == null || status.isTerminal()) {
                    return;
                }
            } catch (OptimisticLockingFailureException ex) {
                log.warn("Lock otimista ao processar requestId={}, tentativa {}", requestId, attempt);
            }
        }
    }

    private ActivationStatus runAttempt(UUID requestId) {
        ActivationRequest claimed = processActivationService.claimForProcessing(requestId).orElse(null);
        if (claimed == null) {
            return ActivationStatus.COMPLETED;
        }
        ProviderResult result = providerGateway.activate(new ActivateProductCommand(
                claimed.getId(),
                claimed.getExternalId(),
                claimed.getCustomerId(),
                claimed.getProductCode()
        ));
        return processActivationService.applyProviderResult(requestId, result);
    }
}
