package com.poorbet.authstarter.auth.token;

import com.poorbet.authstarter.auth.config.AuthServiceProperties;
import com.poorbet.authstarter.auth.dto.ClientCredentialsTokenRequest;
import com.poorbet.authstarter.auth.dto.TokenResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

public class ServiceTokenProvider {

    private final WebClient authWebClient;
    private final AuthServiceProperties authServiceProperties;

    private volatile String token;
    private volatile Instant expiresAt;

    public ServiceTokenProvider(WebClient authWebClient, AuthServiceProperties authServiceProperties) {
        this.authWebClient = authWebClient;
        this.authServiceProperties = authServiceProperties;
    }

    public synchronized String getServiceToken() {
        if (token == null || expiresAt == null || Instant.now().isAfter(expiresAt)) {
            TokenResponse response = authWebClient.post()
                    .uri("/oauth/token")
                    .bodyValue(new ClientCredentialsTokenRequest(
                            "client_credentials",
                            authServiceProperties.clientId(),
                            authServiceProperties.clientSecret()
                    ))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();

            if (response == null || response.accessToken() == null || response.expiresIn() <= 0) {
                throw new IllegalStateException("Auth service did not return valid token response");
            }

            token = response.accessToken();
            expiresAt = Instant.now().plusSeconds(Math.max(30, response.expiresIn() - 30));
        }

        return token;
    }
}
