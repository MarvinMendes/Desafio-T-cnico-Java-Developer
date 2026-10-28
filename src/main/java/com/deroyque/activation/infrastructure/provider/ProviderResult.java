package com.deroyque.activation.infrastructure.provider;

public sealed interface ProviderResult {

    record Success() implements ProviderResult {
    }

    record ClientError(int status) implements ProviderResult {
    }

    record ServerError(int status) implements ProviderResult {
    }

    record RateLimited(int status) implements ProviderResult {
    }

    record Timeout() implements ProviderResult {
    }
}
