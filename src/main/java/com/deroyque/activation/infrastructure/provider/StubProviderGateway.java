package com.deroyque.activation.infrastructure.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "provider.stub", havingValue = "true")
public class StubProviderGateway implements ProviderGateway {

    private static final Logger log = LoggerFactory.getLogger(StubProviderGateway.class);

    @Override
    public ProviderResult activate(ActivateProductCommand command) {
        log.debug("Stub do provedor aceitou externalId={}", command.externalId());
        return new ProviderResult.Success();
    }
}
