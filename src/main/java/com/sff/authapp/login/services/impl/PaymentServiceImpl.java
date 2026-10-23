package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.payment.*;
import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.Order;
import com.sff.authapp.login.entities.Payment;
import com.sff.authapp.login.entities.PaymentMethod;
import com.sff.authapp.login.entities.PaymentType;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.AdditionalChargeRepository;
import com.sff.authapp.login.repositories.OrderRepository;
import com.sff.authapp.login.repositories.PaymentRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.services.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;
    private final AdditionalChargeRepository
            additionalChargeRepository;


    // ============================================================
    // CREATE PAYMENT
    // ============================================================

    @Override
    public PaymentResponse createPayment(
            PaymentRequestDto request
    ) {

//        print request for debugging
        System.out.println("PaymentRequestDto: " + request);
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
        System.out.println("Grand Total: " + grandTotal);


        BigDecimal totalPaid =
                calculateTotalPaid(order.getId());
        System.out.println("Total Paid: " + totalPaid);

        BigDecimal balanceDue =
                grandTotal.subtract(totalPaid);

        System.out.println("Balance Due: " + balanceDue);

        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "This order has no outstanding balance"
            );
        }


        if (request.amount().compareTo(balanceDue) > 0) {
            System.out.println("Payment amount cannot exceed balance due. "
                            + "Balance due: ₹"
                            + balanceDue);

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
                        .receivedBy(
                                request.receivedById() != null
                                        ? userRepository
                                        .findUserById(
                                                request.receivedById()
                                        )
                                        .orElseThrow(() ->
                                                new ResourceNotFoundException(
                                                        "User not found with id: "
                                                                + request.receivedById()
                                                )
                                        )
                                        : null
                        )
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

    private PaymentTransactionResponse toTransactionResponse(Payment payment) {
        Order o = payment.getOrder();
        User c = o != null ? o.getCustomer() : null;
        User u = payment.getReceivedBy();

        return new PaymentTransactionResponse(
                payment.getId(),
                o != null ? o.getId() : null,
                c != null ? c.getId() : null,
                c != null ? c.getName() : "Unknown",
                c != null ? c.getPhone() : null,
                c != null ? c.getCustomerCode() : null,
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentType(),
                payment.getPaidAt(),
                payment.getNote(),
                u != null ? u.getId() : null,
                u != null ? u.getName() : null,
                payment.getCreatedAt()
        );
    }

    // ============================================================
    // BUILD PAYMENT SPECIFICATION
    // ============================================================

    private org.springframework.data.jpa.domain.Specification<Payment> buildPaymentSpecification(
            LocalDate startDate,
            LocalDate endDate,
            PaymentMethod method,
            PaymentType type,
            String keyword
    ) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paidAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paidAt"), endDate.atTime(java.time.LocalTime.MAX)));
            }
            if (method != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), method));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("paymentType"), type));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Join<Payment, Order> orderJoin = root.join("order", jakarta.persistence.criteria.JoinType.LEFT);
                jakarta.persistence.criteria.Join<Order, User> customerJoin = orderJoin.join("customer", jakarta.persistence.criteria.JoinType.LEFT);

                jakarta.persistence.criteria.Predicate nameLike = cb.like(cb.lower(customerJoin.get("name")), pattern);
                jakarta.persistence.criteria.Predicate phoneLike = cb.like(cb.lower(customerJoin.get("phone")), pattern);
                jakarta.persistence.criteria.Predicate noteLike = cb.like(cb.lower(root.get("note")), pattern);

                try {
                    Long num = Long.parseLong(keyword.trim());
                    jakarta.persistence.criteria.Predicate orderIdMatch = cb.equal(orderJoin.get("id"), num);
                    predicates.add(cb.or(nameLike, phoneLike, noteLike, orderIdMatch));
                } catch (NumberFormatException e) {
                    predicates.add(cb.or(nameLike, phoneLike, noteLike));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // ============================================================
    // ADVANCED PAYMENT TRANSACTIONS SEARCH
    // ============================================================

    @Override
    @Transactional
    public Page<PaymentTransactionResponse> getPaymentTransactions(
            LocalDate startDate,
            LocalDate endDate,
            PaymentMethod method,
            PaymentType type,
            String keyword,
            Pageable pageable
    ) {
        org.springframework.data.jpa.domain.Specification<Payment> spec =
                buildPaymentSpecification(startDate, endDate, method, type, keyword);

        return paymentRepository.findAll(spec, pageable).map(this::toTransactionResponse);
    }

    // ============================================================
    // PAYMENT ANALYTICS
    // ============================================================

    @Override
    @Transactional
    public PaymentAnalyticsResponse getPaymentAnalytics(
            LocalDate startDate,
            LocalDate endDate,
            PaymentMethod method,
            PaymentType type,
            String keyword
    ) {
        LocalDate today = LocalDate.now();

        // 1. Today's stats (matching active method/type/keyword filters)
        org.springframework.data.jpa.domain.Specification<Payment> todaySpec =
                buildPaymentSpecification(today, today, method, type, keyword);
        List<Payment> todayPayments = paymentRepository.findAll(todaySpec);
        long todayCount = todayPayments.size();
        BigDecimal todayTotal = todayPayments.stream()
                .map(Payment::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Selected period stats (matching active filters)
        org.springframework.data.jpa.domain.Specification<Payment> rangeSpec =
                buildPaymentSpecification(startDate, endDate, method, type, keyword);
        List<Payment> rangePayments = paymentRepository.findAll(rangeSpec);
        long rangeCount = rangePayments.size();
        BigDecimal rangeTotal = rangePayments.stream()
                .map(Payment::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Payment method breakdown in the selected period (matching filters)
        Map<String, BigDecimal> breakdown = new java.util.LinkedHashMap<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            breakdown.put(pm.name(), BigDecimal.ZERO);
        }
        for (Payment p : rangePayments) {
            if (p.getPaymentMethod() != null && p.getAmount() != null) {
                String pmName = p.getPaymentMethod().name();
                breakdown.put(pmName, breakdown.getOrDefault(pmName, BigDecimal.ZERO).add(p.getAmount()));
            }
        }

        // 4. Delivered unpaid summary
        long deliveredUnpaidCount = 0;
        BigDecimal deliveredUnpaidTotalBalance = BigDecimal.ZERO;
        try {
            List<Order> deliveredOrders = orderRepository.findAllByGarmentStatus(com.sff.authapp.login.entities.GarmentStatus.DELIVERED);
            if (deliveredOrders != null) {
                String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim().toLowerCase() : null;
                for (Order o : deliveredOrders) {
                    if (o == null) continue;
                    if (kw != null) {
                        User c = o.getCustomer();
                        String cName = c != null && c.getName() != null ? c.getName().toLowerCase() : "";
                        String cPhone = c != null && c.getPhone() != null ? c.getPhone().toLowerCase() : "";
                        String cCode = c != null && c.getCustomerCode() != null ? c.getCustomerCode().toLowerCase() : "";
                        String oId = String.valueOf(o.getId());
                        if (!cName.contains(kw) && !cPhone.contains(kw) && !cCode.contains(kw) && !oId.contains(kw)) {
                            continue;
                        }
                    }
                    BigDecimal grandTotal = calculateGrandTotal(o);
                    BigDecimal totalPaid = calculateTotalPaid(o.getId());
                    BigDecimal balance = grandTotal.subtract(totalPaid);
                    if (balance.compareTo(BigDecimal.ZERO) > 0) {
                        deliveredUnpaidCount++;
                        deliveredUnpaidTotalBalance = deliveredUnpaidTotalBalance.add(balance);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new PaymentAnalyticsResponse(
                todayTotal,
                todayCount,
                rangeTotal,
                rangeCount,
                breakdown,
                deliveredUnpaidCount,
                deliveredUnpaidTotalBalance
        );
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        if (obj instanceof BigDecimal bd) {
            return bd;
        }
        if (obj instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long toLong(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    // ============================================================
    // DELIVERED UNPAID ORDERS
    // ============================================================

    @Override
    @Transactional
    public Page<DeliveredUnpaidOrderResponse> getDeliveredUnpaidOrders(
            String keyword,
            Pageable pageable
    ) {
        List<Order> deliveredOrders = orderRepository.findAllByGarmentStatus(com.sff.authapp.login.entities.GarmentStatus.DELIVERED);
        List<DeliveredUnpaidOrderResponse> resultList = new java.util.ArrayList<>();

        String kw = keyword != null ? keyword.trim().toLowerCase() : null;

        for (Order o : deliveredOrders) {
            BigDecimal grandTotal = calculateGrandTotal(o);
            BigDecimal totalPaid = calculateTotalPaid(o.getId());
            BigDecimal balance = grandTotal.subtract(totalPaid);

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                User c = o.getCustomer();
                String cName = c != null ? c.getName() : "Unknown";
                String cPhone = c != null ? c.getPhone() : "";
                String cCode = c != null ? c.getCustomerCode() : "";

                if (kw != null && !kw.isEmpty()) {
                    boolean match = cName.toLowerCase().contains(kw) ||
                            cPhone.toLowerCase().contains(kw) ||
                            cCode.toLowerCase().contains(kw) ||
                            String.valueOf(o.getId()).contains(kw);
                    if (!match) continue;
                }

                int totalGarments = o.getGarments().size();
                int deliveredGarments = (int) o.getGarments().stream()
                        .filter(g -> g.getStatus() == com.sff.authapp.login.entities.GarmentStatus.DELIVERED)
                        .count();

                resultList.add(new DeliveredUnpaidOrderResponse(
                        o.getId(),
                        o.getOrderDate(),
                        c != null ? c.getId() : null,
                        cName,
                        cPhone,
                        cCode,
                        grandTotal,
                        totalPaid,
                        balance,
                        totalGarments,
                        deliveredGarments,
                        o.getUpdatedAt()
                ));
            }
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), resultList.size());
        List<DeliveredUnpaidOrderResponse> pageContent = start < resultList.size()
                ? resultList.subList(start, end)
                : java.util.Collections.emptyList();

        return new org.springframework.data.domain.PageImpl<>(
                pageContent,
                pageable,
                resultList.size()
        );
    }
}