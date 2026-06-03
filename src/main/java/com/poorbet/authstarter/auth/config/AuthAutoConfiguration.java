package com.poorbet.authstarter.auth.config;

import com.poorbet.authstarter.auth.token.ServiceTokenProvider;
import com.poorbet.authstarter.auth.webclient.JwtForwardingInterceptor;
import com.poorbet.authstarter.auth.webclient.ServiceJwtForwardingInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@ConditionalOnProperty(prefix = "auth.service", name = "url")
@EnableConfigurationProperties(AuthServiceProperties.class)
public class AuthAutoConfiguration {

    @Bean(name = "authRestClient")
    @ConditionalOnMissingBean(name = "authRestClient")
    public RestClient authRestClient(AuthServiceProperties properties) {

        return RestClient.builder()
                .baseUrl(properties.url())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtForwardingInterceptor jwtForwardingInterceptor() {
        return new JwtForwardingInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceJwtForwardingInterceptor serviceJwtForwardingInterceptor(ServiceTokenProvider serviceTokenProvider) {
        return new ServiceJwtForwardingInterceptor(serviceTokenProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceTokenProvider serviceTokenProvider(
            @Qualifier("authRestClient") RestClient authRestClient,
            AuthServiceProperties properties) {

        return new ServiceTokenProvider(authRestClient, properties);
    }
}
