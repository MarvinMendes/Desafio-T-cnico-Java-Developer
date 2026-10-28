package com.deroyque.activation.api.dto;

import com.deroyque.activation.application.CreateActivationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateActivationRequest(
        @NotBlank(message = "externalId é obrigatório")
        @Size(max = 64, message = "externalId deve ter no máximo 64 caracteres")
        @Pattern(regexp = ID_PATTERN, message = "externalId deve conter apenas letras, números, ponto, hífen ou underscore")
        String externalId,

        @NotBlank(message = "customerId é obrigatório")
        @Size(max = 64, message = "customerId deve ter no máximo 64 caracteres")
        @Pattern(regexp = ID_PATTERN, message = "customerId deve conter apenas letras, números, ponto, hífen ou underscore")
        String customerId,

        @NotBlank(message = "productCode é obrigatório")
        @Size(max = 64, message = "productCode deve ter no máximo 64 caracteres")
        @Pattern(regexp = ID_PATTERN, message = "productCode deve conter apenas letras, números, ponto, hífen ou underscore")
        String productCode
) {
    static final String ID_PATTERN = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$";

    public CreateActivationCommand toCommand(String ownerClientId) {
        return new CreateActivationCommand(externalId, customerId, productCode, ownerClientId);
    }
}
