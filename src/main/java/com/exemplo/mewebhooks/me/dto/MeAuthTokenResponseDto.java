package com.exemplo.mewebhooks.me.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MeAuthTokenResponseDto(
        @JsonAlias({"accessToken", "token", "access_token"})
        String accessToken,
        @JsonAlias({"expiresIn", "expires_in"})
        Long expiresIn,
        @JsonAlias({"tokenType", "token_type"})
        String tokenType
) {
}
