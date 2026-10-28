package com.deroyque.activation.application;

import com.deroyque.activation.domain.ActivationRequest;
import com.deroyque.activation.infrastructure.persistence.ActivationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetActivationRequestService {

    private final ActivationRequestRepository repository;

    public GetActivationRequestService(ActivationRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ActivationRequest getById(UUID id, String ownerClientId) {
        return repository.findByIdAndOwnerClientId(id, ownerClientId)
                .orElseThrow(() -> new ActivationRequestNotFoundException(id));
    }
}
