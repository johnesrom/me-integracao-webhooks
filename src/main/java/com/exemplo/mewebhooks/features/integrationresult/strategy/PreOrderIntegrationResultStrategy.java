package com.exemplo.mewebhooks.features.integrationresult.strategy;

import com.exemplo.mewebhooks.exception.InvalidWebhookPayloadException;
import com.exemplo.mewebhooks.features.integrationresult.model.IntegrationResultRecord;
import com.exemplo.mewebhooks.me.service.MePreOrderService;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PreOrderIntegrationResultStrategy implements IntegrationResultStrategy {

    private static final Logger log = LoggerFactory.getLogger(PreOrderIntegrationResultStrategy.class);
    private static final String RESOURCE = "pre-order";
    private static final int STATUS = 123;
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
    private static final String SELECT_FLOW_SQL = """
            SELECT PEDIDO_ID_SOLUS, CLIENT_SUPPLIER_ID_ME
              FROM SOLUS.LEVE_ME_PREORDER_FLOW
             WHERE PRE_ORDER_ID_ME = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MePreOrderService mePreOrderService;

    public PreOrderIntegrationResultStrategy(JdbcTemplate jdbcTemplate, MePreOrderService mePreOrderService) {
        this.jdbcTemplate = jdbcTemplate;
        this.mePreOrderService = mePreOrderService;
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
        } else {
            log.info("Pre-order integration.result persistido. preOrderIdMe={} statusCode={}",
                    preOrderIdMe, record.statusCode());
        }

        notifyRejectedStatusIfApplicable(record, preOrderIdMe);
    }

    private void notifyRejectedStatusIfApplicable(IntegrationResultRecord record, BigDecimal preOrderIdMe) {
        Integer statusCode = record.statusCode();
        if (statusCode == null || statusCode < 400) {
            return;
        }

        String preOrderId = trimToNull(record.identifier());
        if (!StringUtils.hasText(preOrderId)) {
            return;
        }

        String clientObservation = trimToNull(record.message());
        String meCorrelationId = trimToNull(record.correlationId());

        PreOrderFlowData flowData = loadFlowData(preOrderIdMe);
        String clientOrderId = flowData == null ? null : flowData.clientOrderId();
        String clientSupplierId = flowData == null ? null : flowData.clientSupplierId();
        String approvalDate = OffsetDateTime.now().toString();

        try {
            String response = mePreOrderService.updatePreOrderStatus(
                    preOrderId,
                    STATUS,
                    clientOrderId,
                    clientSupplierId,
                    approvalDate,
                    meCorrelationId,
                    clientObservation
            );
            log.info("Pre-order integration.result rejeicao notificada ao ME. preOrderIdMe={} statusCode={} response={}",
                    preOrderId, statusCode, response);
        } catch (Exception ex) {
            log.error("Falha ao notificar rejeicao do pre-order ao ME. preOrderIdMe={} statusCode={} reason={}",
                    preOrderId, statusCode, ex.getMessage(), ex);
        }
    }

    private PreOrderFlowData loadFlowData(BigDecimal preOrderIdMe) {
        List<PreOrderFlowData> rows = jdbcTemplate.query(
                SELECT_FLOW_SQL,
                (rs, rowNum) -> new PreOrderFlowData(
                        toPlainStringOrNull(rs.getBigDecimal("PEDIDO_ID_SOLUS")),
                        rs.getString("CLIENT_SUPPLIER_ID_ME")
                ),
                preOrderIdMe
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String toPlainStringOrNull(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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

    private record PreOrderFlowData(String clientOrderId, String clientSupplierId) {
    }
}
