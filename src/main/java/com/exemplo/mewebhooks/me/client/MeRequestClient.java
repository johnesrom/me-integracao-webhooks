package com.exemplo.mewebhooks.me.client;

import com.exemplo.mewebhooks.exception.MeApiClientException;
import com.exemplo.mewebhooks.me.properties.MeApiProperties;
import com.exemplo.mewebhooks.me.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class MeRequestClient {

    private static final Logger log = LoggerFactory.getLogger(MeRequestClient.class);

    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;
    private final MeApiProperties meApiProperties;

    public MeRequestClient(
            RestTemplate restTemplate,
            AuthenticationService authenticationService,
            MeApiProperties meApiProperties
    ) {
        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
        this.meApiProperties = meApiProperties;
    }

    public String send(String module,
                       HttpMethod method,
                       String uri,
                       Object payload,
                       String meCorrelationId,
                       Object... uriVariables) {
        try {
            String token = authenticationService.getValidBearerToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(meCorrelationId)) {
                headers.set("X-ME-CORRELATION-ID", meCorrelationId);
            }

            HttpEntity<?> requestEntity = new HttpEntity<>(payload, headers);
            String url = meApiProperties.resolveBaseUrl() + uri;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    method,
                    requestEntity,
                    String.class,
                    uriVariables
            );

            return response.getBody();
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("me.request.http_error module={} method={} uri={} status={} body={}",
                    module, method, uri, ex.getStatusCode().value(), body);
            throw new MeApiClientException(ex.getStatusCode(), body);
        } catch (MeApiClientException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("me.request.unexpected_error module={} method={} uri={} reason={}",
                    module, method, uri, ex.getMessage(), ex);
            throw new IllegalStateException("Unexpected error on ME request", ex);
        }
    }
}
