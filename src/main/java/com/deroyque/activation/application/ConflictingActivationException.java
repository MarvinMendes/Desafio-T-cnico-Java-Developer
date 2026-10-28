package com.deroyque.activation.application;

public class ConflictingActivationException extends RuntimeException {

    public ConflictingActivationException(String externalId) {
        super("Já existe uma solicitação com externalId '%s' e conteúdo diferente".formatted(externalId));
    }
}
