package com.deroyque.activation.application;

import com.deroyque.activation.domain.ActivationRequest;
import com.deroyque.activation.domain.ActivationStatus;
import com.deroyque.activation.infrastructure.observability.ActivationMetrics;
import com.deroyque.activation.infrastructure.persistence.ActivationRequestRepository;
import com.deroyque.activation.infrastructure.provider.ProviderProperties;
import com.deroyque.activation.infrastructure.provider.ProviderResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessActivationServiceTest {

    @Mock
    private ActivationRequestRepository repository;

    private ProcessActivationService service;
    private ActivationRequest request;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T14:03:11Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        ProviderProperties properties = new ProviderProperties("http://localhost", Duration.ofSeconds(1), Duration.ofSeconds(1), 3, true);
        ActivationMetrics metrics = new ActivationMetrics(new SimpleMeterRegistry(), repository);
        service = new ProcessActivationService(repository, properties, clock, metrics);
        request = ActivationRequest.create("EXT-1", "CUST-1", "INTERNET-500", "system-a", clock);
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));
    }

    @Test
    void successMarksCompleted() {
        service.claimForProcessing(request.getId());

        ActivationStatus status = service.applyProviderResult(request.getId(), new ProviderResult.Success());

        assertThat(status).isEqualTo(ActivationStatus.COMPLETED);
    }

    @Test
    void http400MarksFailed() {
        service.claimForProcessing(request.getId());

        ActivationStatus status = service.applyProviderResult(request.getId(), new ProviderResult.ClientError(400));

        assertThat(status).isEqualTo(ActivationStatus.FAILED);
    }

    @Test
    void timeoutReturnsToPendingWhenAttemptsRemain() {
        service.claimForProcessing(request.getId());

        ActivationStatus status = service.applyProviderResult(request.getId(), new ProviderResult.Timeout());

        assertThat(status).isEqualTo(ActivationStatus.PENDING);
        assertThat(request.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void timeoutMarksFailedWhenAttemptsExhausted() {
        request.markProcessing(clock);
        request.markPendingForRetry(clock);
        request.markProcessing(clock);
        request.markPendingForRetry(clock);
        request.markProcessing(clock);

        ActivationStatus status = service.applyProviderResult(request.getId(), new ProviderResult.Timeout());

        assertThat(status).isEqualTo(ActivationStatus.FAILED);
    }
}
