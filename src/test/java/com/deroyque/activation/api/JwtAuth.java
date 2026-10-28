package com.deroyque.activation.api;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

final class JwtAuth {

    private JwtAuth() {
    }

    static RequestPostProcessor fullAccess(String clientId) {
        return jwt().authorities(
                new SimpleGrantedAuthority("SCOPE_requests:read"),
                new SimpleGrantedAuthority("SCOPE_requests:write")
        ).jwt(token -> token.claim("azp", clientId).subject(clientId));
    }

    static RequestPostProcessor readOnly(String clientId) {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_requests:read"))
                .jwt(token -> token.claim("azp", clientId).subject(clientId));
    }

    static RequestPostProcessor writeOnly(String clientId) {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_requests:write"))
                .jwt(token -> token.claim("azp", clientId).subject(clientId));
    }
}
