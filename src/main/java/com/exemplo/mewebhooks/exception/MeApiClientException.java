package com.exemplo.mewebhooks.exception;

import org.springframework.http.HttpStatusCode;

public class MeApiClientException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public MeApiClientException(HttpStatusCode statusCode, String responseBody) {
        super("ME API returned error status: " + statusCode.value());
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
