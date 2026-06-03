package com.poorbet.authstarter.auth.webclient;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Optional;

public class JwtForwardingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        String token = resolveUserToken()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing user JWT in SecurityContext for API call"
                ));

        request.getHeaders().setBearerAuth(token);

        return execution.execute(request, body);
    }

    private Optional<String> resolveUserToken() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt.getTokenValue());
        }

        return java.util.Optional.empty();
    }
}