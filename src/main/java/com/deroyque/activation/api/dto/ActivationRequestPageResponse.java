package com.deroyque.activation.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ActivationRequestPageResponse(
        List<ActivationRequestResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ActivationRequestPageResponse from(Page<ActivationRequestResponse> page) {
        return new ActivationRequestPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
