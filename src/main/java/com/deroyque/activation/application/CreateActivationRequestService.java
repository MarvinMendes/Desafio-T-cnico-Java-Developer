package com.deroyque.activation.application;

import com.deroyque.activation.domain.ActivationRequest;
import com.deroyque.activation.infrastructure.persistence.ActivationRequestRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

@Service
public class CreateActivationRequestService {

    private final ActivationRequestRepository repository;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactionTemplate;

    public CreateActivationRequestService(
            ActivationRequestRepository repository,
            Clock clock,
            ApplicationEventPublisher events,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.clock = clock;
        this.events = events;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public CreateActivationResult create(CreateActivationCommand command) {
        return repository.findByOwnerClientIdAndExternalId(command.ownerClientId(), command.externalId())
                .map(existing -> reuseOrConflict(existing, command))
                .orElseGet(() -> insertOrReuse(command));
    }

    private CreateActivationResult insertOrReuse(CreateActivationCommand command) {
        try {
            return transactionTemplate.execute(status -> persistNew(command));
        } catch (RuntimeException ex) {
            if (!isDuplicateKey(ex)) {
                throw ex;
            }
            ActivationRequest existing = repository
                    .findByOwnerClientIdAndExternalId(command.ownerClientId(), command.externalId())
                    .orElseThrow(() -> ex);
            return reuseOrConflict(existing, command);
        }
    }

    private CreateActivationResult persistNew(CreateActivationCommand command) {
        ActivationRequest request = ActivationRequest.create(
                command.externalId(),
                command.customerId(),
                command.productCode(),
                command.ownerClientId(),
                clock
        );
        ActivationRequest saved = repository.saveAndFlush(request);
        events.publishEvent(new ActivationRequestedEvent(saved.getId()));
        return new CreateActivationResult(saved, true);
    }

    private CreateActivationResult reuseOrConflict(
            ActivationRequest existing,
            CreateActivationCommand command
    ) {
        if (!existing.hasSamePayload(command.customerId(), command.productCode())) {
            throw new ConflictingActivationException(command.externalId());
        }
        return new CreateActivationResult(existing, false);
    }

    private boolean isDuplicateKey(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DataIntegrityViolationException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("uk_activation_request_owner_external")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
