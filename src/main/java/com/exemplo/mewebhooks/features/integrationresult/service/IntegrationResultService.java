package com.exemplo.mewebhooks.features.integrationresult.service;

import com.exemplo.mewebhooks.features.integrationresult.dto.IntegrationResultWebhookDto;

public interface IntegrationResultService {
    void process(IntegrationResultWebhookDto webhookDto);
}
