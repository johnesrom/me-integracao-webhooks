package com.exemplo.mewebhooks.me.service;

import com.exemplo.mewebhooks.me.client.MeRequestClient;
import com.exemplo.mewebhooks.me.dto.MePreOrderStatusUpdateRequestDto;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class MePreOrderService {

    private static final String MODULE = "pre-orders";

    private final MeRequestClient meRequestClient;

    public MePreOrderService(MeRequestClient meRequestClient) {
        this.meRequestClient = meRequestClient;
    }

    public String updatePreOrderStatus(String preOrderId,
                                       Integer status,
                                       String clientOrderId,
                                       String clientSupplierId,
                                       String approvalDate,
                                       String meCorrelationId,
                                       String clientObservation) {
        MePreOrderStatusUpdateRequestDto payload = new MePreOrderStatusUpdateRequestDto(
                approvalDate,
                status,
                clientOrderId,
                clientSupplierId,
                clientObservation
        );

        return meRequestClient.send(
                MODULE,
                HttpMethod.PUT,
                "/v1/pre-orders/{preOrderId}/status",
                payload,
                meCorrelationId,
                preOrderId
        );
    }
}
