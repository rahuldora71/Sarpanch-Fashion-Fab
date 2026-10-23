package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.payment.*;
import com.sff.authapp.login.entities.PaymentMethod;
import com.sff.authapp.login.entities.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(
            PaymentRequestDto request
    );

    PaymentResponse getPaymentById(
            Long paymentId
    );

    List<PaymentResponse> getOrderPayments(
            Long orderId
    );

    PaymentSummaryResponse getOrderPaymentSummary(
            Long orderId
    );

    void deletePayment(
            Long paymentId
    );

    Page<PaymentTransactionResponse> getPaymentTransactions(
            LocalDate startDate,
            LocalDate endDate,
            PaymentMethod method,
            PaymentType type,
            String keyword,
            Pageable pageable
    );

    default PaymentAnalyticsResponse getPaymentAnalytics(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getPaymentAnalytics(startDate, endDate, null, null, null);
    }

    PaymentAnalyticsResponse getPaymentAnalytics(
            LocalDate startDate,
            LocalDate endDate,
            PaymentMethod method,
            PaymentType type,
            String keyword
    );

    Page<DeliveredUnpaidOrderResponse> getDeliveredUnpaidOrders(
            String keyword,
            Pageable pageable
    );
}