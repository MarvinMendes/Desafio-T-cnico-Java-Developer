package com.deroyque.activation.application;

public record CreateActivationCommand(
        String externalId,
        String customerId,
        String productCode,
        String ownerClientId
) {
}
