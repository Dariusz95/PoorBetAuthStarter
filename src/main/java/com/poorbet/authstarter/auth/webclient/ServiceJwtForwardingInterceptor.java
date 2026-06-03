package com.poorbet.authstarter.auth.webclient;

import com.poorbet.authstarter.auth.token.ServiceTokenProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@RequiredArgsConstructor
public class ServiceJwtForwardingInterceptor implements ClientHttpRequestInterceptor {

    private final ServiceTokenProvider tokenProvider;


    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        if (shouldSkip(request)) {
            return execution.execute(request, body);
        }

        request.getHeaders().setBearerAuth(tokenProvider.getServiceToken());
        return execution.execute(request, body);
    }

    private boolean shouldSkip(HttpRequest request) {
        return request.getHeaders().containsKey("X-Skip-Auth");
    }
}