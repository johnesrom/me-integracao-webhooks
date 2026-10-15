package com.exemplo.mewebhooks.me.service;

import com.exemplo.mewebhooks.me.dto.MeAuthTokenRequestDto;
import com.exemplo.mewebhooks.me.dto.MeAuthTokenResponseDto;
import com.exemplo.mewebhooks.exception.MeApiClientException;
import com.exemplo.mewebhooks.me.properties.MeApiProperties;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private static final long DEFAULT_TOKEN_TTL_SECONDS = 300L;

    private final RestTemplate restTemplate;
    private final MeApiProperties meApiProperties;

    private volatile CachedToken cachedToken;

    public AuthenticationServiceImpl(RestTemplate restTemplate, MeApiProperties meApiProperties) {
        this.restTemplate = restTemplate;
        this.meApiProperties = meApiProperties;
    }

    @Override
    public String getValidBearerToken() {
        CachedToken tokenSnapshot = this.cachedToken;
        if (tokenSnapshot != null && Instant.now().isBefore(tokenSnapshot.expiresAt())) {
            return tokenSnapshot.bearerToken();
        }

        synchronized (this) {
            tokenSnapshot = this.cachedToken;
            if (tokenSnapshot != null && Instant.now().isBefore(tokenSnapshot.expiresAt())) {
                return tokenSnapshot.bearerToken();
            }
            return requestAndCacheToken();
        }
    }

    @Override
    public synchronized String refreshBearerToken() {
        return requestAndCacheToken();
    }

    private String requestAndCacheToken() {
        String env = meApiProperties.getEnvironment() == null
                ? "STG"
                : meApiProperties.getEnvironment().toUpperCase(Locale.ROOT);
        String url = meApiProperties.resolveBaseUrl() + meApiProperties.getOauthPath();

        MeAuthTokenRequestDto request = new MeAuthTokenRequestDto(
                meApiProperties.getClientId(),
                meApiProperties.getClientSecret()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            log.info("me.auth.token.renew.start environment={} url={}", env, url);
            ResponseEntity<MeAuthTokenResponseDto> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(request, headers),
                    MeAuthTokenResponseDto.class
            );

            MeAuthTokenResponseDto body = response.getBody();
            if (body == null || !StringUtils.hasText(body.accessToken())) {
                throw new IllegalStateException("Invalid ME auth response: token not returned");
            }

            long expiresIn = body.expiresIn() == null ? DEFAULT_TOKEN_TTL_SECONDS : body.expiresIn();
            long adjustedExpiresIn = Math.max(expiresIn - meApiProperties.getTokenSkewSeconds(), 1L);
            Instant expiresAt = Instant.now().plusSeconds(adjustedExpiresIn);

            String bearerToken = normalizeBearerToken(body.tokenType(), body.accessToken());
            this.cachedToken = new CachedToken(bearerToken, expiresAt);

            log.info("me.auth.token.renew.success environment={} expiresInSeconds={}", env, adjustedExpiresIn);
            return bearerToken;
        } catch (RestClientResponseException ex) {
            log.error("me.auth.token.renew.http_error environment={} status={} body={}",
                    env, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new MeApiClientException(ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("me.auth.token.renew.failure environment={} reason={}", env, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to authenticate with Mercado Eletronico API", ex);
        }
    }

    private String normalizeBearerToken(String tokenType, String accessToken) {
        if (accessToken.startsWith("Bearer ")) {
            return accessToken;
        }

        if (StringUtils.hasText(tokenType) && !"Bearer".equalsIgnoreCase(tokenType)) {
            return tokenType + " " + accessToken;
        }

        return "Bearer " + accessToken;
    }

    private record CachedToken(String bearerToken, Instant expiresAt) {
    }
}
