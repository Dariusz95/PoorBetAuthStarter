package com.poorbet.authstarter.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth.service")
public record AuthServiceProperties(
        String url,
        String clientId,
        String clientSecret,
        Timeout timeout
) {
    public record Timeout(Duration connect, Duration read) {
    }
}
