package com.poorbet.authstarter.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@AutoConfiguration
@ConditionalOnClass({HttpSecurity.class, NimbusJwtDecoder.class})
@ConditionalOnProperty(prefix = "poorbet.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableMethodSecurity
@EnableConfigurationProperties(PoorbetSecurityProperties.class)
public class PoorbetSecurityAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = "internalSecurityFilterChain")
    public SecurityFilterChain internalSecurityFilterChain(HttpSecurity http,
                                                           PoorbetSecurityProperties properties,
                                                           JwtAuthenticationConverter jwtAuthenticationConverter,
                                                           AuthorizationManager<RequestAuthorizationContext> internalAuthorizationManager) throws Exception {
        http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    if (!properties.getUnprotectedPaths().isEmpty()) {
                        auth.requestMatchers(properties.getUnprotectedPaths().toArray(new String[0])).permitAll();
                    }
                    auth.requestMatchers("/internal/**").access(internalAuthorizationManager);
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );

        return http.build();
    }

    @Bean(name = "apiSecurityFilterChain")
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    @ConditionalOnMissingBean(name = "apiSecurityFilterChain")
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      PoorbetSecurityProperties properties,
                                                      JwtAuthenticationConverter jwtAuthenticationConverter,
                                                      AuthorizationManager<RequestAuthorizationContext> apiAuthorizationManager) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    if (!properties.getUnprotectedPaths().isEmpty()) {
                        auth.requestMatchers(properties.getUnprotectedPaths().toArray(new String[0])).permitAll();
                    }
                    auth.requestMatchers("/api/**").access(apiAuthorizationManager);
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );

        return http.build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    public SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean(name = "apiAuthorizationManager")
    @ConditionalOnMissingBean(name = "apiAuthorizationManager")
    public AuthorizationManager<RequestAuthorizationContext> apiAuthorizationManager() {
        return (authentication, context) -> new AuthorizationDecision(hasTokenType(authentication, PoorbetTokenTypes.USER));
    }

    @Bean(name = "internalAuthorizationManager")
    @ConditionalOnMissingBean(name = "internalAuthorizationManager")
    public AuthorizationManager<RequestAuthorizationContext> internalAuthorizationManager(PoorbetSecurityProperties properties) {
        return (authentication, context) -> new AuthorizationDecision(
                hasTokenType(authentication, PoorbetTokenTypes.SERVICE)
                        && hasAudience(authentication, properties.getInternalAudience())
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(PoorbetSecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(properties.getIssuer());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new PermissionClaimConverter());
        return converter;
    }

    private static boolean hasTokenType(Supplier<Authentication> authenticationSupplier, String expectedType) {
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        return expectedType.equals(jwt.getClaimAsString("token_type"));
    }

    private static boolean hasAudience(Supplier<Authentication> authenticationSupplier, List<String> expectedAudience) {
        if (expectedAudience == null || expectedAudience.isEmpty()) {
            return true;
        }

        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        return jwt.getAudience().stream().anyMatch(expectedAudience::contains);
    }

    static class PermissionClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            List<GrantedAuthority> authorities = new ArrayList<>();

            addAuthorities(authorities, jwt.getClaimAsStringList("permissions"));
            addAuthorities(authorities, resolveScopes(jwt));

            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.stream()
                        .map(PermissionClaimConverter::normalizeRole)
                        .map(role -> "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }

            return authorities;
        }

        private static List<String> resolveScopes(Jwt jwt) {
            List<String> scopes = jwt.getClaimAsStringList("scope");
            if (scopes != null) {
                return scopes;
            }

            String scope = jwt.getClaimAsString("scope");
            if (!StringUtils.hasText(scope)) {
                return List.of();
            }

            return List.of(scope.split(" "));
        }

        private static void addAuthorities(List<GrantedAuthority> authorities, List<String> values) {
            if (values != null) {
                values.stream()
                        .filter(StringUtils::hasText)
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }
        }

        private static String normalizeRole(String role) {
            if (role != null && role.startsWith("ROLE_")) {
                return role.substring("ROLE_".length());
            }
            return role;
        }
    }
}
