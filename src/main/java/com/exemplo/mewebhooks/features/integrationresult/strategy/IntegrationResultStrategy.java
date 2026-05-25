package com.exemplo.mewebhooks.features.integrationresult.strategy;

import com.exemplo.mewebhooks.features.integrationresult.model.IntegrationResultRecord;

public interface IntegrationResultStrategy {
    String resource();
    void persist(IntegrationResultRecord record);
}
