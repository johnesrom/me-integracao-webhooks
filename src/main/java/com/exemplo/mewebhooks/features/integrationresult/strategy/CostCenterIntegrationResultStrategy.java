package com.exemplo.mewebhooks.features.integrationresult.strategy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CostCenterIntegrationResultStrategy extends AbstractJdbcIntegrationResultStrategy {

    public CostCenterIntegrationResultStrategy(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "costcenter", "SOLUS.LEVE_ME_API_COSTCENTER");
    }
}
