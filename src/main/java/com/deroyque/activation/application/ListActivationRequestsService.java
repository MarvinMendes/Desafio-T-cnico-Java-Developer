package com.deroyque.activation.application;

import com.deroyque.activation.domain.ActivationRequest;
import com.deroyque.activation.domain.ActivationStatus;
import com.deroyque.activation.infrastructure.persistence.ActivationRequestRepository;
import com.deroyque.activation.infrastructure.persistence.ActivationRequestSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListActivationRequestsService {

    private final ActivationRequestRepository repository;

    public ListActivationRequestsService(ActivationRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<ActivationRequest> list(
            String ownerClientId,
            ActivationStatus status,
            String customerId,
            Pageable pageable
    ) {
        return repository.findAll(
                ActivationRequestSpecifications.withFilters(ownerClientId, status, customerId),
                pageable
        );
    }
}
