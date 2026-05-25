package com.exemplo.mewebhooks.features.integrationresult.strategy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductIntegrationResultStrategy extends AbstractJdbcIntegrationResultStrategy {

    public ProductIntegrationResultStrategy(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "product", "SOLUS.LEVE_ME_API_PRODUCT");
    }
}
