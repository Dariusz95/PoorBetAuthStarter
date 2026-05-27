package com.poorbet.authstarter.auth.config;

import com.poorbet.authstarter.auth.token.ServiceTokenProvider;
import com.poorbet.authstarter.auth.webclient.JwtForwardingFilter;
import com.poorbet.authstarter.auth.webclient.ServiceJwtForwardingFilter;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(prefix = "auth.service", name = "url")
@EnableConfigurationProperties(AuthServiceProperties.class)
public class AuthWebClientConfiguration {

    @Bean(name = "authWebClient")
    @ConditionalOnMissingBean(name = "authWebClient")
    public WebClient authWebClient(AuthServiceProperties authServiceProperties) {
        return WebClient.builder()
                .baseUrl(authServiceProperties.url())
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(authServiceProperties.timeout().read())
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) authServiceProperties.timeout().connect().toMillis())
                ))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtForwardingFilter jwtForwardingFilter() {
        return new JwtForwardingFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceJwtForwardingFilter serviceJwtForwardingFilter(ServiceTokenProvider serviceTokenProvider) {
        return new ServiceJwtForwardingFilter(serviceTokenProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceTokenProvider serviceTokenProvider(@Qualifier("authWebClient") WebClient authWebClient,
                                                     AuthServiceProperties authServiceProperties) {
        return new ServiceTokenProvider(authWebClient, authServiceProperties);
    }
}
