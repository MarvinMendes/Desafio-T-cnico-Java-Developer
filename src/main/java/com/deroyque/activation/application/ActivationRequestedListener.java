package com.deroyque.activation.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "activation.processing.async", havingValue = "true", matchIfMissing = true)
public class ActivationRequestedListener {

    private final ActivationProcessingDispatcher dispatcher;

    public ActivationRequestedListener(ActivationProcessingDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActivationRequested(ActivationRequestedEvent event) {
        dispatcher.dispatchAsync(event.requestId());
    }
}
