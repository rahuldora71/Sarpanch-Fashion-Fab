package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.payment.PaymentRequestDto;
import com.sff.authapp.login.dtos.payment.PaymentResponse;
import com.sff.authapp.login.dtos.payment.PaymentSummaryResponse;
import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.Order;
import com.sff.authapp.login.entities.Payment;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.AdditionalChargeRepository;
import com.sff.authapp.login.repositories.OrderRepository;
import com.sff.authapp.login.repositories.PaymentRepository;
import com.sff.authapp.login.services.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final OrderRepository orderRepository;

    private final AdditionalChargeRepository
            additionalChargeRepository;


    // ============================================================
    // CREATE PAYMENT
    // ============================================================

    @Override
    public PaymentResponse createPayment(
            PaymentRequestDto request
    ) {

        Order order =
                orderRepository.findById(request.orderId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found with id: "
                                                + request.orderId()
                                )
                        );


        BigDecimal grandTotal =
                calculateGrandTotal(order);


        BigDecimal totalPaid =
                calculateTotalPaid(order.getId());


        BigDecimal balanceDue =
                grandTotal.subtract(totalPaid);


        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "This order has no outstanding balance"
            );
        }


        if (request.amount().compareTo(balanceDue) > 0) {

            throw new IllegalArgumentException(
                    "Payment amount cannot exceed balance due. "
                            + "Balance due: ₹"
                            + balanceDue
            );
        }


        Payment payment =
                Payment.builder()
                        .order(order)
                        .amount(request.amount())
                        .paymentMethod(
                                request.paymentMethod()
                        )
                        .paymentType(
                                request.paymentType()
                        )
                        .paidAt(
                                request.paidAt() != null
                                        ? request.paidAt()
                                        : LocalDateTime.now()
                        )
                        .note(request.note())
                        .build();


        Payment saved =
                paymentRepository.save(payment);


        return toResponse(saved);
    }


    // ============================================================
    // GET PAYMENT
    // ============================================================

    @Override
    @Transactional
    public PaymentResponse getPaymentById(
            Long paymentId
    ) {

        Payment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found with id: "
                                                + paymentId
                                )
                        );

        return toResponse(payment);
    }


    // ============================================================
    // GET ORDER PAYMENTS
    // ============================================================

    @Override
    @Transactional
    public List<PaymentResponse> getOrderPayments(
            Long orderId
    ) {

        validateOrder(orderId);

        return paymentRepository
                .findByOrderIdOrderByPaidAtAscIdAsc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // GET PAYMENT SUMMARY
    // ============================================================

    @Override
    @Transactional
    public PaymentSummaryResponse getOrderPaymentSummary(
            Long orderId
    ) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found with id: "
                                                + orderId
                                )
                        );


        BigDecimal subtotal =
                calculateSubtotal(order);


        BigDecimal additionalCharges =
                calculateAdditionalCharges(orderId);


        BigDecimal grandTotal =
                subtotal.add(additionalCharges);


        BigDecimal totalPaid =
                calculateTotalPaid(orderId);


        BigDecimal balanceDue =
                grandTotal.subtract(totalPaid);


        if (balanceDue.compareTo(BigDecimal.ZERO) < 0) {
            balanceDue = BigDecimal.ZERO;
        }


        return new PaymentSummaryResponse(
                orderId,
                subtotal,
                additionalCharges,
                grandTotal,
                totalPaid,
                balanceDue
        );
    }


    // ============================================================
    // DELETE PAYMENT
    // ============================================================

    @Override
    public void deletePayment(
            Long paymentId
    ) {

        Payment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found with id: "
                                                + paymentId
                                )
                        );

        paymentRepository.delete(payment);
    }


    // ============================================================
    // CALCULATE SUBTOTAL
    // ============================================================

    private BigDecimal calculateSubtotal(
            Order order
    ) {

        if (order.getGarments() == null
                || order.getGarments().isEmpty()) {

            return BigDecimal.ZERO;
        }


        return order.getGarments()
                .stream()
                .map(Garment::getPrice)
                .filter(price -> price != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }


    // ============================================================
    // CALCULATE ADDITIONAL CHARGES
    // ============================================================

    private BigDecimal calculateAdditionalCharges(
            Long orderId
    ) {

        return additionalChargeRepository
                .findByOrderIdOrderByCreatedAtAscIdAsc(orderId)
                .stream()
                .map(charge -> charge.getAmount())
                .filter(amount -> amount != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }


    // ============================================================
    // CALCULATE GRAND TOTAL
    // ============================================================

    private BigDecimal calculateGrandTotal(
            Order order
    ) {

        BigDecimal subtotal =
                calculateSubtotal(order);


        BigDecimal additionalCharges =
                calculateAdditionalCharges(
                        order.getId()
                );


        return subtotal.add(
                additionalCharges
        );
    }


    // ============================================================
    // CALCULATE TOTAL PAID
    // ============================================================

    private BigDecimal calculateTotalPaid(
            Long orderId
    ) {

        return paymentRepository
                .findByOrderIdOrderByPaidAtAscIdAsc(orderId)
                .stream()
                .map(Payment::getAmount)
                .filter(amount -> amount != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }


    // ============================================================
    // VALIDATE ORDER
    // ============================================================

    private void validateOrder(
            Long orderId
    ) {

        if (!orderRepository.existsById(orderId)) {

            throw new ResourceNotFoundException(
                    "Order not found with id: "
                            + orderId
            );
        }
    }


    // ============================================================
    // RESPONSE MAPPER
    // ============================================================

    private PaymentResponse toResponse(
            Payment payment
    ) {

        return new PaymentResponse(

                payment.getId(),

                payment.getOrder() != null
                        ? payment.getOrder().getId()
                        : null,

                payment.getAmount(),

                payment.getPaymentMethod(),

                payment.getPaymentType(),

                payment.getPaidAt(),

                payment.getNote(),

                payment.getReceivedBy() != null
                        ? payment.getReceivedBy().getId()
                        : null,

                payment.getCreatedAt()
        );
    }
}