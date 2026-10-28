package com.deroyque.activation.application;

import com.deroyque.activation.domain.ActivationRequest;

public record CreateActivationResult(ActivationRequest request, boolean created) {
}
