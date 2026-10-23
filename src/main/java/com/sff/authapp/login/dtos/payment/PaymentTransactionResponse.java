package com.sff.authapp.login.dtos.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sff.authapp.login.entities.PaymentMethod;
import com.sff.authapp.login.entities.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        Long paymentId,
        Long orderId,
        Long customerId,
        String customerName,
        String customerPhone,
        String customerCode,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentType paymentType,
        LocalDateTime paidAt,
        String note,
        @JsonProperty("receivedByUserId")
        Long receivedByUserId,
        @JsonProperty("receivedByUserName")
        String receivedByUserName,
        LocalDateTime createdAt
) {
    @JsonProperty("receivedById")
    public Long receivedById() {
        return receivedByUserId;
    }

    @JsonProperty("receivedByName")
    public String receivedByName() {
        return receivedByUserName;
    }
}
