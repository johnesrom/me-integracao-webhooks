package com.exemplo.mewebhooks.me.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "me.api")
public class MeApiProperties {

    private String environment = "STG";
    private String stgUrl = "https://stg.api.mercadoe.com";
    private String prodUrl = "https://api.mercadoe.com";
    private String clientId;
    private String clientSecret;
    private long tokenSkewSeconds = 30;
    private String oauthPath = "/v1/auth/tokens";

    public String resolveBaseUrl() {
        return "PRD".equalsIgnoreCase(environment) ? prodUrl : stgUrl;
    }
}
