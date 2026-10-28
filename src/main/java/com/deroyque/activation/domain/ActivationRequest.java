package com.deroyque.activation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "activation_request",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_activation_request_owner_external",
                columnNames = {"owner_client_id", "external_id"}
        )
)
public class ActivationRequest {

    private static final Set<ActivationStatus> FROM_PENDING = EnumSet.of(ActivationStatus.PROCESSING);
    private static final Set<ActivationStatus> FROM_PROCESSING = EnumSet.of(
            ActivationStatus.COMPLETED,
            ActivationStatus.FAILED,
            ActivationStatus.PENDING
    );

    @Id
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "owner_client_id", nullable = false, length = 64)
    private String ownerClientId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ActivationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActivationRequest() {
    }

    public static ActivationRequest create(
            String externalId,
            String customerId,
            String productCode,
            String ownerClientId,
            Clock clock
    ) {
        ActivationRequest request = new ActivationRequest();
        Instant now = clock.instant();
        request.id = UUID.randomUUID();
        request.externalId = externalId;
        request.customerId = customerId;
        request.productCode = productCode;
        request.ownerClientId = ownerClientId;
        request.status = ActivationStatus.PENDING;
        request.attemptCount = 0;
        request.createdAt = now;
        request.updatedAt = now;
        return request;
    }

    public void markProcessing(Clock clock) {
        transitionTo(ActivationStatus.PROCESSING, clock);
        attemptCount++;
    }

    public void markCompleted(Clock clock) {
        transitionTo(ActivationStatus.COMPLETED, clock);
    }

    public void markFailed(Clock clock) {
        transitionTo(ActivationStatus.FAILED, clock);
    }

    public void markPendingForRetry(Clock clock) {
        transitionTo(ActivationStatus.PENDING, clock);
    }

    public boolean hasSamePayload(String customerId, String productCode) {
        return this.customerId.equals(customerId) && this.productCode.equals(productCode);
    }

    public boolean canRetry(int maxAttempts) {
        return attemptCount < maxAttempts && !status.isTerminal();
    }

    private void transitionTo(ActivationStatus target, Clock clock) {
        if (!allowedTargets().contains(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        this.status = target;
        this.updatedAt = clock.instant();
    }

    private Set<ActivationStatus> allowedTargets() {
        return switch (status) {
            case PENDING -> FROM_PENDING;
            case PROCESSING -> FROM_PROCESSING;
            case COMPLETED, FAILED -> EnumSet.noneOf(ActivationStatus.class);
        };
    }

    public UUID getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getOwnerClientId() {
        return ownerClientId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProductCode() {
        return productCode;
    }

    public ActivationStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
