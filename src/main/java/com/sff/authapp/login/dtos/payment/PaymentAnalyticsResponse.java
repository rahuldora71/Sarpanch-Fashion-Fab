package com.sff.authapp.login.dtos.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

public record PaymentAnalyticsResponse(
        @JsonProperty("todayTotalAmount")
        BigDecimal todayTotalAmount,

        @JsonProperty("todayPaymentCount")
        long todayPaymentCount,

        @JsonProperty("rangeTotalAmount")
        BigDecimal rangeTotalAmount,

        @JsonProperty("rangePaymentCount")
        long rangePaymentCount,

        @JsonProperty("paymentMethodBreakdown")
        Map<String, BigDecimal> paymentMethodBreakdown,

        @JsonProperty("deliveredUnpaidCount")
        long deliveredUnpaidCount,

        @JsonProperty("deliveredUnpaidTotalBalance")
        BigDecimal deliveredUnpaidTotalBalance
) {
    @JsonProperty("todayTotal")
    public BigDecimal todayTotal() {
        return todayTotalAmount;
    }

    @JsonProperty("todayCount")
    public long todayCount() {
        return todayPaymentCount;
    }

    @JsonProperty("rangeTotal")
    public BigDecimal rangeTotal() {
        return rangeTotalAmount;
    }

    @JsonProperty("rangeCount")
    public long rangeCount() {
        return rangePaymentCount;
    }

    @JsonProperty("methodBreakdown")
    public Map<String, BigDecimal> methodBreakdown() {
        return paymentMethodBreakdown;
    }
}
