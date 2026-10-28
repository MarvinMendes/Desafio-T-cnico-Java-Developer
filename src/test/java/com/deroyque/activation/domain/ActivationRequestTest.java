package com.deroyque.activation.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivationRequestTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T14:03:11Z"), ZoneOffset.UTC);

    @Test
    void createStartsAsPending() {
        ActivationRequest request = ActivationRequest.create("EXT-1", "CUST-1", "INTERNET-500", "system-a", clock);

        assertThat(request.getStatus()).isEqualTo(ActivationStatus.PENDING);
        assertThat(request.getAttemptCount()).isZero();
        assertThat(request.getCreatedAt()).isEqualTo(Instant.parse("2026-10-26T14:03:11Z"));
    }

    @Test
    void allowsPendingToProcessingToCompleted() {
        ActivationRequest request = ActivationRequest.create("EXT-1", "CUST-1", "INTERNET-500", "system-a", clock);

        request.markProcessing(clock);
        request.markCompleted(clock);

        assertThat(request.getStatus()).isEqualTo(ActivationStatus.COMPLETED);
        assertThat(request.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void allowsRetryFromProcessingBackToPending() {
        ActivationRequest request = ActivationRequest.create("EXT-1", "CUST-1", "INTERNET-500", "system-a", clock);
        request.markProcessing(clock);

        request.markPendingForRetry(clock);

        assertThat(request.getStatus()).isEqualTo(ActivationStatus.PENDING);
        assertThat(request.canRetry(3)).isTrue();
    }

    @Test
    void rejectsCompletedBackToProcessing() {
        ActivationRequest request = ActivationRequest.create("EXT-1", "CUST-1", "INTERNET-500", "system-a", clock);
        request.markProcessing(clock);
        request.markCompleted(clock);

        assertThatThrownBy(() -> request.markProcessing(clock))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void rejectsFailedBackToProcessing() {
        ActivationRequest request = ActivationRequest.create("EXT-1", "CUST-1", "INTERNET-500", "system-a", clock);
        request.markProcessing(clock);
        request.markFailed(clock);

        assertThatThrownBy(() -> request.markProcessing(clock))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
