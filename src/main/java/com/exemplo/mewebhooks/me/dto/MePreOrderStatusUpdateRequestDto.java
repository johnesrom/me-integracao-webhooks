package com.exemplo.mewebhooks.me.dto;

public record MePreOrderStatusUpdateRequestDto(
        String approvalDate,
        Integer status,
        String clientOrderId,
        String clientSupplierId,
        String clientObservation
) {
}
