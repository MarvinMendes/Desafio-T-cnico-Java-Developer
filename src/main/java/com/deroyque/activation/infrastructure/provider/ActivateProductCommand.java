package com.deroyque.activation.infrastructure.provider;

import java.util.UUID;

public record ActivateProductCommand(
        UUID activationRequestId,
        String externalId,
        String customerId,
        String productCode
) {
}
