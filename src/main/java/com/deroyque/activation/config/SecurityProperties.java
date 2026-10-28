package com.deroyque.activation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        int rateLimitPerMinute,
        int maxBodyBytes
) {
    public SecurityProperties {
        if (rateLimitPerMinute <= 0) {
            rateLimitPerMinute = 60;
        }
        if (maxBodyBytes <= 0) {
            maxBodyBytes = 16_384;
        }
    }
}
