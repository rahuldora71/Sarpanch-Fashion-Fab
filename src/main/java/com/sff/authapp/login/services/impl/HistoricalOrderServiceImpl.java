package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.order.HistoricalCustomerSummaryDto;
import com.sff.authapp.login.dtos.order.HistoricalOrderRequestDto;
import com.sff.authapp.login.dtos.order.HistoricalOrderResponseDto;
import com.sff.authapp.login.dtos.order.NextOrderNumberResponseDto;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.helpers.CustomerCodeHelper;
import com.sff.authapp.login.repositories.*;
import com.sff.authapp.login.services.HistoricalOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalOrderServiceImpl implements HistoricalOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TailoringServiceRepository tailoringServiceRepository;
    private final GarmentRepository garmentRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public HistoricalOrderResponseDto createHistoricalOrder(
            HistoricalOrderRequestDto request,
            Long authenticatedUserId
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Order request cannot be null");
        }

        // 1. Resolve Customer
        User customer;
        if (request.getCustomerId() != null) {
            customer = userRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));

            // Ensure customer code exists
            if (customer.getCustomerCode() == null || customer.getCustomerCode().isBlank()) {
                customer.setCustomerCode(CustomerCodeHelper.generateFromUserId(customer.getId()));
                customer = userRepository.save(customer);
            }
        } else {
            // Validate new customer fields
            if (request.getName() == null || request.getName().trim().isBlank()) {
                throw new IllegalArgumentException("Customer name is required for new customer");
            }
            if (request.getPhone() == null || request.getPhone().trim().isBlank()) {
                throw new IllegalArgumentException("Customer phone is required for new customer");
            }

            Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").build()));

            User newCustomer = User.builder()
                    .name(request.getName().trim())
                    .phone(request.getPhone().trim())
                    .alternatePhone(request.getAlternatePhone() != null && !request.getAlternatePhone().isBlank()
                            ? request.getAlternatePhone().trim() : null)
                    .address(request.getAddress() != null && !request.getAddress().isBlank()
                            ? request.getAddress().trim() : null)
                    .enable(true)
                    .provider(Provider.LOCAL)
                    .roles(new HashSet<>(Collections.singletonList(customerRole)))
                    .build();

            User saved = userRepository.save(newCustomer);
            saved.setCustomerCode(CustomerCodeHelper.generateFromUserId(saved.getId()));
            customer = userRepository.save(saved);
        }

        // 2. Resolve Order Number
        Long assignedOrderNumber;
        if (request.getOrderNumber() != null && request.getOrderNumber() > 0) {
            if (orderRepository.existsByOrderNumber(request.getOrderNumber())) {
                throw new IllegalArgumentException("Order #" + request.getOrderNumber() + " already exists in the system.");
            }
            assignedOrderNumber = request.getOrderNumber();
        } else {
            Long max = orderRepository.findMaxOrderNumber();
            assignedOrderNumber = (max == null || max < 1) ? 1L : max + 1L;
        }

        // 3. Resolve Order Date & Delivery
        LocalDate orderDate = request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now();
        Boolean isDelivered = request.getIsDelivered() != null ? request.getIsDelivered() : Boolean.TRUE;

        // 4. Save Order
        Order order = Order.builder()
                .customer(customer)
                .orderDate(orderDate)
                .orderNumber(assignedOrderNumber)
                .billImageUrl(request.getBillImageUrl())
                .billImagePublicId(request.getBillImagePublicId())
                .isDelivered(isDelivered)
                .build();
        Order savedOrder = orderRepository.save(order);

        // 5. Ensure TailoringService & Create Garment for financial integrity
        TailoringService tailoringService = tailoringServiceRepository.findByNameIgnoreCase("Tailoring")
                .or(() -> tailoringServiceRepository.findAll().stream().findFirst())
                .orElseGet(() -> {
                    TailoringService ts = new TailoringService();
                    ts.setName("Tailoring");
                    ts.setCategory("GENERAL");
                    ts.setPrice(BigDecimal.ZERO);
                    ts.setActive(true);
                    return tailoringServiceRepository.save(ts);
                });

        Garment garment = Garment.builder()
                .order(savedOrder)
                .service(tailoringService)
                .price(request.getTotalAmount())
                .status(Boolean.TRUE.equals(isDelivered) ? GarmentStatus.DELIVERED : GarmentStatus.READY)
                .note(request.getNotes() != null && !request.getNotes().isBlank()
                        ? request.getNotes().trim() : "Historical Order #" + assignedOrderNumber)
                .build();
        garmentRepository.save(garment);

        // 6. Record Advance Payment if any
        BigDecimal advance = request.getAdvancePayment() != null ? request.getAdvancePayment() : BigDecimal.ZERO;
        PaymentMethod pm = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH;

        if (advance.compareTo(BigDecimal.ZERO) > 0) {
            User receivedBy = null;
            if (authenticatedUserId != null) {
                receivedBy = userRepository.findById(authenticatedUserId).orElse(null);
            }
            if (receivedBy == null) {
                receivedBy = customer;
            }

            PaymentType pt = advance.compareTo(request.getTotalAmount()) >= 0
                    ? PaymentType.FINAL
                    : PaymentType.ADVANCE;

            Payment payment = Payment.builder()
                    .order(savedOrder)
                    .amount(advance)
                    .paymentMethod(pm)
                    .paymentType(pt)
                    .receivedBy(receivedBy)
                    .note("Historical order entry (Order #" + assignedOrderNumber + ")")
                    .paidAt(orderDate.atTime(12, 0))
                    .build();
            paymentRepository.save(payment);
        }

        BigDecimal balanceDue = request.getTotalAmount().subtract(advance).max(BigDecimal.ZERO);
        Long nextSuggested = assignedOrderNumber + 1L;

        return HistoricalOrderResponseDto.builder()
                .orderId(savedOrder.getId())
                .orderNumber(assignedOrderNumber)
                .orderDate(orderDate)
                .totalAmount(request.getTotalAmount())
                .advancePayment(advance)
                .balanceDue(balanceDue)
                .isDelivered(isDelivered)
                .billImageUrl(request.getBillImageUrl())
                .billImagePublicId(request.getBillImagePublicId())
                .paymentMethod(pm)
                .customerId(customer.getId())
                .customerName(customer.getName())
                .customerPhone(customer.getPhone())
                .customerCode(customer.getCustomerCode())
                .nextSuggestedOrderNumber(nextSuggested)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NextOrderNumberResponseDto getNextOrderNumber() {
        Long max = orderRepository.findMaxOrderNumber();
        long next = (max == null || max < 1) ? 1L : max + 1L;
        return new NextOrderNumberResponseDto(next);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalCustomerSummaryDto> findCustomersByPhone(String phone) {
        if (phone == null || phone.trim().length() < 3) {
            return Collections.emptyList();
        }

        String search = phone.trim();
        List<User> matchedUsers = userRepository.findCustomersByPhoneContaining(search);

        List<HistoricalCustomerSummaryDto> results = new ArrayList<>();
        for (User u : matchedUsers) {
            List<Order> orders = orderRepository.findByCustomerIdOrderByOrderDateDescIdDesc(u.getId());
            long totalOrders = orders.size();
            BigDecimal totalSpent = BigDecimal.ZERO;
            BigDecimal currentBalance = BigDecimal.ZERO;
            LocalDate lastOrderDate = orders.isEmpty() ? null : orders.get(0).getOrderDate();

            for (Order o : orders) {
                BigDecimal orderTotal = o.getGarments().stream()
                        .map(Garment::getPrice)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (o.getAdditionalCharges() != null) {
                    orderTotal = orderTotal.add(o.getAdditionalCharges().stream()
                            .map(AdditionalCharge::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                }

                BigDecimal orderPaid = o.getPayments().stream()
                        .map(Payment::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal balance = orderTotal.subtract(orderPaid);
                if (balance.compareTo(BigDecimal.ZERO) < 0) {
                    balance = BigDecimal.ZERO;
                }

                totalSpent = totalSpent.add(orderTotal);
                currentBalance = currentBalance.add(balance);
            }

            results.add(new HistoricalCustomerSummaryDto(
                    u.getId(),
                    u.getCustomerCode(),
                    u.getName(),
                    u.getPhone(),
                    u.getAlternatePhone(),
                    u.getAddress(),
                    u.getImage(),
                    totalOrders,
                    totalSpent,
                    currentBalance,
                    lastOrderDate
            ));
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalOrderResponseDto> getRecentHistoricalOrders(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 50));
        List<Order> orders = orderRepository.findRecentHistoricalOrders(PageRequest.of(0, cappedLimit));

        List<HistoricalOrderResponseDto> responses = new ArrayList<>();
        for (Order o : orders) {
            BigDecimal orderTotal = o.getGarments().stream()
                    .map(Garment::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (o.getAdditionalCharges() != null) {
                orderTotal = orderTotal.add(o.getAdditionalCharges().stream()
                        .map(AdditionalCharge::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
            }

            BigDecimal paid = o.getPayments().stream()
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal balance = orderTotal.subtract(paid);
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO;
            }

            PaymentMethod pm = o.getPayments().stream()
                    .map(Payment::getPaymentMethod)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(PaymentMethod.CASH);

            User c = o.getCustomer();
            responses.add(HistoricalOrderResponseDto.builder()
                    .orderId(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .orderDate(o.getOrderDate())
                    .totalAmount(orderTotal)
                    .advancePayment(paid)
                    .balanceDue(balance)
                    .isDelivered(o.getIsDelivered())
                    .billImageUrl(o.getBillImageUrl())
                    .billImagePublicId(o.getBillImagePublicId())
                    .paymentMethod(pm)
                    .customerId(c != null ? c.getId() : null)
                    .customerName(c != null ? c.getName() : "Unknown")
                    .customerPhone(c != null ? c.getPhone() : "")
                    .customerCode(c != null ? c.getCustomerCode() : "")
                    .build());
        }

        return responses;
    }
}
