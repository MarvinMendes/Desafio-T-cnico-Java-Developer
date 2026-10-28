package com.deroyque.activation.application;

import java.util.UUID;

public record ActivationRequestedEvent(UUID requestId) {
}
