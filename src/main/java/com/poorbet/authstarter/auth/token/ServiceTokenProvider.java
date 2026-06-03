package com.poorbet.authstarter.auth.token;

import com.poorbet.authstarter.auth.config.AuthServiceProperties;
import com.poorbet.authstarter.auth.dto.ClientCredentialsTokenRequest;
import com.poorbet.authstarter.auth.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@RequiredArgsConstructor
public class ServiceTokenProvider {

    private final RestClient authClient;
    private final AuthServiceProperties authServiceProperties;

    private volatile String token;
    private volatile Instant expiresAt;

    public synchronized String getServiceToken() {
        if (token == null || expiresAt == null || Instant.now().isAfter(expiresAt)) {
            TokenResponse response = authClient.post()
                    .uri("/oauth/token")
                    .body(new ClientCredentialsTokenRequest(
                            "client_credentials",
                            authServiceProperties.clientId(),
                            authServiceProperties.clientSecret()
                    ))
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.accessToken() == null || response.expiresIn() <= 0) {
                throw new IllegalStateException("Auth service did not return valid token response");
            }

            token = response.accessToken();
            expiresAt = Instant.now().plusSeconds(Math.max(30, response.expiresIn() - 30));
        }

        return token;
    }
}
