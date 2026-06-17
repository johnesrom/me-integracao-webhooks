package com.exemplo.mewebhooks.features.integrationresult.service;

import com.exemplo.mewebhooks.exception.InvalidWebhookPayloadException;
import com.exemplo.mewebhooks.features.integrationresult.dto.IntegrationResultWebhookDto;
import com.exemplo.mewebhooks.features.integrationresult.model.IntegrationResultRecord;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IntegrationResultServiceImpl implements IntegrationResultService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationResultServiceImpl.class);
    private static final Pattern CLIENT_ORDER_ID_PATTERN = Pattern.compile("clientorderid\\s+([0-9]+)", Pattern.CASE_INSENSITIVE);

    private final IntegrationResultPersistenceService integrationResultPersistenceService;

    public IntegrationResultServiceImpl(
            IntegrationResultPersistenceService integrationResultPersistenceService
    ) {
        this.integrationResultPersistenceService = integrationResultPersistenceService;
    }

    @Override
    public void process(IntegrationResultWebhookDto webhookDto) {
        String topic = webhookDto.getTopic();
        if (!"integration.result".equalsIgnoreCase(topic)) {
            throw new InvalidWebhookPayloadException("Topico invalido para esta rota: " + topic);
        }

        IntegrationResultWebhookDto.DataPayload data = webhookDto.getData();
        log.info(
                "Webhook integration.result recebido. topic={} resource={} correlationId={} identifier={} statusCode={} status={}",
                topic,
                data.getResource(),
                data.getCorrelationId(),
                data.getIdentifier(),
                data.getStatusCode(),
                data.getStatus()
        );

        String normalizedResource = normalizeResource(data.getResource());
        String idSolusForPersistence = resolveIdSolusForPersistence(normalizedResource, data);
        processRequestHappyPathIfApplicable(normalizedResource, data, idSolusForPersistence);

        IntegrationResultRecord record = new IntegrationResultRecord(
                idSolusForPersistence,
                data.getIdentifier(),
                data.getStatusCode(),
                data.getStatus(),
                data.getMessage(),
                null,
                Instant.now()
        );

//        if (integrationResultPersistenceService.supports(normalizedResource)) {
            integrationResultPersistenceService.persist(normalizedResource, record);
            return;
//        }

//        throw new InvalidWebhookPayloadException("Resource nao suportado: " + data.getResource());
    }

    private boolean isSuccessStatusCode(Integer statusCode) {
        return statusCode != null && (statusCode == 200 || statusCode == 201);
    }

    private void processRequestHappyPathIfApplicable(
            String normalizedResource,
            IntegrationResultWebhookDto.DataPayload data,
            String idSolusForPersistence
    ) {
        if (!"request".equals(normalizedResource) || !isSuccessStatusCode(data.getStatusCode())) {
            return;
        }

        if (StringUtils.hasText(idSolusForPersistence)) {
            return;
        }
        log.warn(
                "Webhook de request com sucesso recebido sem ID_SOLUS numerico. Processamento de geracao de pedido sera ignorado. correlationId={} identifier={}",
                data.getCorrelationId(),
                data.getIdentifier()
        );
    }

    private String normalizeResource(String resource) {
        if (!StringUtils.hasText(resource)) {
            return "";
        }
        String normalized = resource.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "request", "requests" -> "request";
            case "order", "orders" -> "order";
            case "pre-order", "pre-orders", "preorder", "preorders" -> "pre-order";
            default -> normalized;
        };
    }

    private String resolveIdSolusForPersistence(String normalizedResource, IntegrationResultWebhookDto.DataPayload data) {
        String correlationId = trimToNull(data.getCorrelationId());
        if (isNumeric(correlationId)) {
            return correlationId;
        }

        if ("request".equals(normalizedResource)) {
            String identifier = trimToNull(data.getIdentifier());
            if (isNumeric(identifier)) {
                log.warn(
                        "CorrelationId nao numerico para request; usando identifier como ID_SOLUS. correlationId={} identifier={}",
                        correlationId,
                        identifier
                );
                return identifier;
            }
        }

        if ("order".equals(normalizedResource)) {
            String idFromMessage = extractOrderIdFromMessage(data.getMessage());
            if (isNumeric(idFromMessage)) {
                log.warn(
                        "CorrelationId nao numerico para order; usando ClientOrderId extraido da mensagem como ID_SOLUS. correlationId={} message={} extractedId={}",
                        correlationId,
                        data.getMessage(),
                        idFromMessage
                );
                return idFromMessage;
            }
        }

        return correlationId;
    }

    private String extractOrderIdFromMessage(String message) {
        String normalized = trimToNull(message);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        Matcher matcher = CLIENT_ORDER_ID_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isNumeric(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
