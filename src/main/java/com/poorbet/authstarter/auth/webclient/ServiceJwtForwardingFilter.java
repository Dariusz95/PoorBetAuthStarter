package com.poorbet.authstarter.auth.webclient;

import com.poorbet.authstarter.auth.token.ServiceTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ServiceJwtForwardingFilter implements ExchangeFilterFunction {

    public static final String SKIP_AUTH = "SKIP_AUTH";

    private final ServiceTokenProvider serviceTokenProvider;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        boolean skip = request.attribute(SKIP_AUTH)
                .map(Boolean.class::cast)
                .orElse(false);

        if (skip) {
            return next.exchange(request);
        }

        String token = serviceTokenProvider.getServiceToken();

        ClientRequest newRequest = ClientRequest.from(request)
                .headers(headers -> headers.setBearerAuth(token))
                .build();

        return next.exchange(newRequest);
    }
}
