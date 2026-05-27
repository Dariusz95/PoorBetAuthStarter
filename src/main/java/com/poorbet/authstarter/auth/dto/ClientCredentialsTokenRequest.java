package com.poorbet.authstarter.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ClientCredentialsTokenRequest(
        @JsonProperty("grant_type") @NotBlank String grantType,
        @JsonProperty("client_id") @NotBlank String clientId,
        @JsonProperty("client_secret") @NotBlank String clientSecret
) {
}
