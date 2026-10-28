package com.deroyque.activation.infrastructure.persistence;

import com.deroyque.activation.domain.ActivationRequest;
import com.deroyque.activation.domain.ActivationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ActivationRequestRepository extends JpaRepository<ActivationRequest, UUID>,
        JpaSpecificationExecutor<ActivationRequest> {

    Optional<ActivationRequest> findByOwnerClientIdAndExternalId(String ownerClientId, String externalId);

    Optional<ActivationRequest> findByIdAndOwnerClientId(UUID id, String ownerClientId);

    long countByStatus(ActivationStatus status);
}
