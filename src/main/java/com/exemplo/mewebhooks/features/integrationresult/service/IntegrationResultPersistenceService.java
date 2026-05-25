package com.exemplo.mewebhooks.features.integrationresult.service;

import com.exemplo.mewebhooks.features.integrationresult.model.IntegrationResultRecord;

public interface IntegrationResultPersistenceService {
    void persist(String resource, IntegrationResultRecord record);
    boolean supports(String resource);
}
