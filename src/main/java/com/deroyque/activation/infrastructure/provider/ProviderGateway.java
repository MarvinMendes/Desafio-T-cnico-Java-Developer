package com.deroyque.activation.infrastructure.provider;

public interface ProviderGateway {

    ProviderResult activate(ActivateProductCommand command);
}
