package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.payment.PaymentRequestDto;
import com.sff.authapp.login.dtos.payment.PaymentResponse;
import com.sff.authapp.login.dtos.payment.PaymentSummaryResponse;

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
}