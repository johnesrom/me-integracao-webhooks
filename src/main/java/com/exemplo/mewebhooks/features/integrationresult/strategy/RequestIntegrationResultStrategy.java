package com.exemplo.mewebhooks.features.integrationresult.strategy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RequestIntegrationResultStrategy extends AbstractJdbcIntegrationResultStrategy {

    public RequestIntegrationResultStrategy(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "request", "SOLUS.LEVE_ME_API_REQUEST");
    }
}
