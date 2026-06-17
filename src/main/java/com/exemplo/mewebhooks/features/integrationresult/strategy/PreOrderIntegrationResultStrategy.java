package com.exemplo.mewebhooks.features.integrationresult.strategy;

import com.exemplo.mewebhooks.exception.InvalidWebhookPayloadException;
import com.exemplo.mewebhooks.features.integrationresult.model.IntegrationResultRecord;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PreOrderIntegrationResultStrategy implements IntegrationResultStrategy {

    private static final Logger log = LoggerFactory.getLogger(PreOrderIntegrationResultStrategy.class);
    private static final String RESOURCE = "pre-order";
    private static final DateTimeFormatter WEBHOOK_RESPONSE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String UPDATE_SQL = """
            UPDATE SOLUS.LEVE_ME_PREORDER_FLOW
               SET ERROR_MESSAGE = CASE
                       WHEN ERROR_MESSAGE IS NULL OR DBMS_LOB.GETLENGTH(ERROR_MESSAGE) = 0
                           THEN TO_CLOB(?)
                       ELSE ERROR_MESSAGE || TO_CLOB(' |' || CHR(10) || ?)
                   END,
                   UPDATED_AT = SYSTIMESTAMP
             WHERE PRE_ORDER_ID_ME = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PreOrderIntegrationResultStrategy(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String resource() {
        return RESOURCE;
    }

    @Override
    public void persist(IntegrationResultRecord record) {
        BigDecimal preOrderIdMe = toRequiredNumber(record.identifier(), "identifier");
        String webhookMessage = buildWebhookMessage(record.message());

        int updatedRows = jdbcTemplate.update(
                UPDATE_SQL,
                new Object[]{webhookMessage, webhookMessage, preOrderIdMe},
                new int[]{Types.VARCHAR, Types.VARCHAR, Types.NUMERIC}
        );

        if (updatedRows == 0) {
            log.warn("Pre-order integration.result nao encontrou registro. preOrderIdMe={}", preOrderIdMe);
            return;
        }

        log.info("Pre-order integration.result persistido. preOrderIdMe={} statusCode={}",
                preOrderIdMe, record.statusCode());
    }

    private BigDecimal toRequiredNumber(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidWebhookPayloadException("Campo obrigatorio ausente no payload: " + fieldName);
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new InvalidWebhookPayloadException("Campo " + fieldName + " invalido para coluna NUMBER: " + value);
        }
    }

    private String buildWebhookMessage(String message) {
        String timestamp = LocalDateTime.now().format(WEBHOOK_RESPONSE_TIMESTAMP_FORMATTER);
        if (!StringUtils.hasText(message)) {
            return "whk-response  " + timestamp + " :";
        }
        return "whk-response  " + timestamp + " : " + message.trim();
    }
}
