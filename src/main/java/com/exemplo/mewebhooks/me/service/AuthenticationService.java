package com.exemplo.mewebhooks.me.service;

public interface AuthenticationService {

    String getValidBearerToken();

    String refreshBearerToken();
}
