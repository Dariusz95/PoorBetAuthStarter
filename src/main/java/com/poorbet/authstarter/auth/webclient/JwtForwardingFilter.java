package com.poorbet.authstarter.auth.webclient;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class JwtForwardingFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String token = resolveUserToken()
                .orElseThrow(() -> new IllegalStateException("Missing user JWT in SecurityContext for API WebClient call"));

        ClientRequest newRequest = ClientRequest.from(request)
                .headers(headers -> headers.setBearerAuth(token))
                .build();

        return next.exchange(newRequest);
    }

    private java.util.Optional<String> resolveUserToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return java.util.Optional.of(jwt.getTokenValue());
        }
        return java.util.Optional.empty();
    }
}
