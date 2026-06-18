package com.exemplo.mewebhooks.features.integrationresult.strategy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderIntegrationResultStrategy extends AbstractJdbcIntegrationResultStrategy {

    public OrderIntegrationResultStrategy(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "order", "SOLUS.LEVE_ME_API_ORDER");
    }
}

