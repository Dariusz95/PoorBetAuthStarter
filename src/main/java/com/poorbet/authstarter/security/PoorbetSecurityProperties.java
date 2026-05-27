package com.poorbet.authstarter.security;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "poorbet.security")
public class PoorbetSecurityProperties {

    private boolean enabled = true;
    private String issuer = "poorbet-auth-service";
    private String jwkSetUri;
    private List<String> unprotectedPaths = new ArrayList<>();
    private List<String> internalAudience = new ArrayList<>();
}
