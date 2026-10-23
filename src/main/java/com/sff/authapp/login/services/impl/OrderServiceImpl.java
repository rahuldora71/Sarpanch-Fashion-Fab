
        package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.order.*;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.helpers.CustomerCodeHelper;
import com.sff.authapp.login.helpers.GarmentClassificationHelper;
import com.sff.authapp.login.repositories.*;
import com.sff.authapp.login.services.OrderService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

        @Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final GarmentRepository garmentRepository;
    private final TailoringServiceRepository tailoringServiceRepository;
    private final GarmentMeasurementRepository garmentMeasurementRepository;
    private final GarmentAddOnRepository garmentAddOnRepository;
    private final AddOnRepository addOnRepository;
    private final AdditionalChargeRepository additionalChargeRepository;
    private final PaymentRepository paymentRepository;


    // ============================================================
    // CREATE NORMAL ORDER
    // ============================================================

    @Override
    public OrderResponse createOrder(
            OrderRequestDto request
    ) {

        System.out.println(
                "Creating order for customer ID: "
                        + request.customerId()
        );

        System.out.println(
                "OrderRequestDto: "
                        + request
        );

        User customer =
                findCustomerById(request.customerId());

        /*
         * Generate the customer code when the customer
         * places their first order.
         *
         * If the customer already has a code,
         * nothing happens.
         */
        ensureCustomerCode(customer);

        Order order = Order.builder()
                .customer(customer)
                .orderDate(request.orderDate())
                .sampleBookPageNumber(
                        normalize(
                                request.sampleBookPageNumber()
                        )
                )
                .discount(request.discount())
                .build();

        Order savedOrder =
                orderRepository.save(order);

        return toResponse(savedOrder);
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(
            Long orderId
    ) {

        return toResponse(
                findOrderById(orderId)
        );
    }


    // ============================================================
    // GET ALL
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(
            Pageable pageable
    ) {

        return orderRepository
                .findAll(pageable)
                .map(this::toResponse);
    }


    // ============================================================
    // GET BY CUSTOMER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomer(
            Long customerId,
            Pageable pageable
    ) {

        findCustomerById(customerId);

        return orderRepository
                .findByCustomerId(
                        customerId,
                        pageable
                )
                .map(this::toResponse);
    }


    // ============================================================
    // GET BY DATE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByDate(
            LocalDate orderDate,
            Pageable pageable
    ) {

        return orderRepository
                .findByOrderDate(
                        orderDate,
                        pageable
                )
                .map(this::toResponse);
    }


    // ============================================================
    // GET BY DATE RANGE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {

        if (startDate == null || endDate == null) {

            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(endDate)) {

            throw new IllegalArgumentException(
                    "Start date cannot be after end date"
            );
        }

        return orderRepository
                .findByOrderDateBetween(
                        startDate,
                        endDate,
                        pageable
                )
                .map(this::toResponse);
    }


    // ============================================================
    // SEARCH SAMPLE BOOK PAGE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchOrdersBySampleBookPage(
            String keyword,
            Pageable pageable
    ) {

        String normalizedKeyword =
                normalize(keyword);

        if (normalizedKeyword == null) {
            return getAllOrders(pageable);
        }

        return orderRepository
                .findBySampleBookPageNumberContainingIgnoreCase(
                        normalizedKeyword,
                        pageable
                )
                .map(this::toResponse);
    }

//    ===============
//    Get Complete order
//    ===============
@Transactional(readOnly = true)
@Override
public OrderDetailsResponse getOrderDetails(Long orderId) {

    // ============================================================
    // 1. FIND ORDER
    // ============================================================

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Order not found with id: " + orderId
                    )
            );


    // ============================================================
    // 2. CUSTOMER
    // ============================================================

    User customer = order.getCustomer();

    OrderDetailsResponse.CustomerDetails customerDetails =
            new OrderDetailsResponse.CustomerDetails(
                    customer.getId(),
                    customer.getCustomerCode(),
                    customer.getName(),
                    customer.getPhone(),
                    customer.getEmail(),
                    customer.getImage()
            );


    // ============================================================
    // 3. FETCH GARMENTS
    // ============================================================

    List<Garment> garments =
            garmentRepository.findCompleteGarmentsByOrderId(orderId);

    // ============================================================
    // 4. FETCH ADDITIONAL CHARGES
    // ============================================================

    List<AdditionalCharge> charges =
            additionalChargeRepository
                    .findByOrderIdOrderByCreatedAtAscIdAsc(orderId);


    // ============================================================
    // 5. FETCH PAYMENTS
    // ============================================================

    List<Payment> payments =
            paymentRepository
                    .findByOrderIdOrderByPaidAtAscIdAsc(orderId);


    // ============================================================
    // 6. MAP GARMENTS
    // ============================================================

    List<OrderDetailsResponse.GarmentDetails> garmentDetails =
            garments.stream()
                    .map(this::mapGarmentDetails)
                    .toList();


    // ============================================================
    // 7. MAP CHARGES
    // ============================================================

    List<OrderDetailsResponse.ChargeDetails> chargeDetails =
            charges.stream()
                    .map(charge ->
                            new OrderDetailsResponse.ChargeDetails(
                                    charge.getId(),
                                    charge.getDescription(),
                                    charge.getAmount()
                            )
                    )
                    .toList();


    // ============================================================
    // 8. MAP PAYMENTS
    // ============================================================

    List<OrderDetailsResponse.PaymentDetails> paymentDetails =
            payments.stream()
                    .map(payment ->
                            new OrderDetailsResponse.PaymentDetails(
                                    payment.getId(),
                                    payment.getAmount(),

                                    payment.getPaymentMethod() != null
                                            ? payment.getPaymentMethod().name()
                                            : null,

                                    payment.getPaymentType() != null
                                            ? payment.getPaymentType().name()
                                            : null,

                                    payment.getNote(),

                                    payment.getReceivedBy() != null
                                            ? payment.getReceivedBy().getId()
                                            : null,

                                    payment.getPaidAt()
                            )
                    )
                    .toList();


    // ============================================================
    // 9. CALCULATE SUBTOTAL
    // ============================================================

    BigDecimal subtotal =
            garments.stream()
                    .map(Garment::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );


    // ============================================================
    // 10. ADD ADD-ONS TO SUBTOTAL
    // ============================================================

    BigDecimal addOnTotal =
            garments.stream()
                    .flatMap(
                            garment -> garment.getAddOns().stream()
                    )
                    .map(GarmentAddOn::getTotalPrice)
                    .filter(Objects::nonNull)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );

    subtotal = subtotal.add(addOnTotal);


    // ============================================================
    // 11. ADDITIONAL CHARGES
    // ============================================================

    BigDecimal additionalCharges =
            charges.stream()
                    .map(AdditionalCharge::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );


    // ============================================================
    // 12. DISCOUNT
    // ============================================================

    BigDecimal discount =
            order.getDiscount() != null
                    ? order.getDiscount()
                    : BigDecimal.ZERO;


    // ============================================================
    // 13. GRAND TOTAL
    // ============================================================

    BigDecimal grandTotal =
            subtotal
                    .add(additionalCharges)
                    .subtract(discount);


    // Safety against negative totals
    if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
        grandTotal = BigDecimal.ZERO;
    }


    // ============================================================
    // 14. TOTAL PAID
    // ============================================================

    BigDecimal totalPaid =
            payments.stream()
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );


    // ============================================================
    // 15. BALANCE DUE
    // ============================================================

    BigDecimal balanceDue =
            grandTotal.subtract(totalPaid);

    if (balanceDue.compareTo(BigDecimal.ZERO) < 0) {
        balanceDue = BigDecimal.ZERO;
    }


    // ============================================================
    // 16. RETURN COMPLETE ORDER
    // ============================================================

    return new OrderDetailsResponse(
            order.getId(),
            order.getOrderDate(),
            order.getSampleBookPageNumber(),

            customerDetails,

            subtotal,
            discount,
            additionalCharges,
            grandTotal,
            totalPaid,
            balanceDue,

            garmentDetails,
            chargeDetails,
            paymentDetails,

            order.getCreatedAt(),
            order.getUpdatedAt()
    );
}

    // ============================================================
    // GET BY GARMENT STATUS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByGarmentStatus(
            GarmentStatus status,
            Pageable pageable
    ) {

        if (status == null) {

            throw new IllegalArgumentException(
                    "Garment status is required"
            );
        }

        return orderRepository
                .findByGarmentStatus(
                        status,
                        pageable
                )
                .map(this::toResponse);
    }


    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public OrderResponse updateOrder(
            Long orderId,
            OrderRequestDto request
    ) {

        Order order =
                findOrderById(orderId);


        // --------------------------------------------------------
        // CUSTOMER
        // --------------------------------------------------------

        if (
                request.customerId() != null
                        && !request.customerId()
                        .equals(order.getCustomer().getId())
        ) {

            User customer =
                    findCustomerById(
                            request.customerId()
                    );

            order.setCustomer(customer);

            ensureCustomerCode(customer);
        }


        // --------------------------------------------------------
        // ORDER DATE
        // --------------------------------------------------------

        if (request.orderDate() != null) {

            order.setOrderDate(
                    request.orderDate()
            );
        }


        // --------------------------------------------------------
        // SAMPLE BOOK PAGE
        // --------------------------------------------------------

        if (request.sampleBookPageNumber() != null) {

            order.setSampleBookPageNumber(
                    normalize(
                            request.sampleBookPageNumber()
                    )
            );
        }


        // --------------------------------------------------------
        // DISCOUNT
        // --------------------------------------------------------

        if (request.discount() != null) {

            if (
                    request.discount()
                            .compareTo(BigDecimal.ZERO) < 0
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Discount cannot be negative"
                );
            }

            order.setDiscount(
                    request.discount()
            );
        }


        Order updatedOrder =
                orderRepository.save(order);

        return toResponse(updatedOrder);
    }


    // ============================================================
    // DELETE
    // ============================================================

    @Override
    public void deleteOrder(
            Long orderId
    ) {

        Order order =
                findOrderById(orderId);

        /*
         * Currently using hard delete.
         *
         * Before enabling this in production, decide whether
         * orders should be immutable/historically preserved.
         */
        orderRepository.delete(order);
    }


    // ============================================================
    // COMPLETE ORDER
    // ============================================================

    @Override
    public CompleteOrderResponse createCompleteOrder(
            CompleteOrderRequestDto request
    ) {

//        print the request for debugging
        System.out.println(
                "Creating complete order for customer ID: "
                        + request
        );
        // =========================================================
        // 1. FIND CUSTOMER
        // =========================================================

        User customer =
                userRepository.findById(
                        request.customerId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id: "
                                        + request.customerId()
                        )
                );


        // =========================================================
        // 2. ENSURE CUSTOMER CODE
        // =========================================================

        ensureCustomerCode(customer);


        // =========================================================
        // 3. CREATE ORDER
        // =========================================================

        Order order = Order.builder()
                .customer(customer)
                .orderDate(request.orderDate())
                .sampleBookPageNumber(
                        normalize(
                                request.sampleBookPageNumber()
                        )
                )
                .discount(
                        request.discount() != null
                                ? request.discount()
                                : BigDecimal.ZERO
                )
                .build();

        Order savedOrder =
                orderRepository.save(order);


        List<CompleteOrderResponse.GarmentCreatedResponse>
                garmentResponses =
                new ArrayList<>();


        BigDecimal subtotal =
                BigDecimal.ZERO;


        // =========================================================
        // 4. CREATE GARMENTS
        // =========================================================

        for (
                CompleteOrderGarmentDto garmentRequest
                : request.garments()
        ) {

            // -----------------------------------------------------
            // FIND TAILORING SERVICE
            // -----------------------------------------------------

            TailoringService service =
                    tailoringServiceRepository.findById(
                            garmentRequest.serviceId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Tailoring service not found with id: "
                                            + garmentRequest.serviceId()
                            )
                    );


            // -----------------------------------------------------
            // CHECK SERVICE ACTIVE
            // -----------------------------------------------------

            if (!service.isActive()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tailoring service is inactive: "
                                + service.getName()
                );
            }


            // -----------------------------------------------------
            // GARMENT PRICE
            // -----------------------------------------------------

            BigDecimal garmentPrice =
                    garmentRequest.price() != null
                            ? garmentRequest.price()
                            : service.getPrice();


            if (
                    garmentPrice.compareTo(
                            BigDecimal.ZERO
                    ) < 0
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Garment price cannot be negative"
                );
            }


            // -----------------------------------------------------
            // CREATE GARMENT
            // -----------------------------------------------------

            boolean isCustFabric = GarmentClassificationHelper.determineCustomerFabric(
                    garmentRequest.customerFabric(),
                    garmentPrice,
                    service.getPrice()
            );

            Garment garment =
                    Garment.builder()
                            .order(savedOrder)
                            .service(service)
                            .price(garmentPrice)
                            .customerFabric(isCustFabric)
                            .sampleNumber(
                                    normalize(
                                            garmentRequest.sampleNumber()
                                    )
                            )
                            .dueDate(
                                    garmentRequest.dueDate()
                            )
                            .note(
                                    garmentRequest.note()
                            )
                            .status(
                                    GarmentStatus.FABRIC_CUT
                            )
                            .build();


            Garment savedGarment =
                    garmentRepository.save(garment);


            BigDecimal garmentTotal =
                    garmentPrice;


            // =====================================================
            // 5. MEASUREMENT
            // =====================================================

            CompleteOrderMeasurementDto measurementRequest =
                    garmentRequest.measurement();


            if (measurementRequest == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Measurement is required for service: "
                                + service.getName()
                );
            }


            // -----------------------------------------------------
            // FIND USER WHO TOOK MEASUREMENT
            // -----------------------------------------------------

            User takenBy =
                    userRepository.findById(
                            measurementRequest.takenByUserId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Measurement user not found with id: "
                                            + measurementRequest
                                            .takenByUserId()
                            )
                    );


            GarmentMeasurement measurement;

            // =====================================================
            // 5A. REUSE EXISTING MEASUREMENT OR CREATE A NEW SNAPSHOT
            // =====================================================

            if (measurementRequest.measurementId() != null) {

                Long measurementId = measurementRequest.measurementId();

                // The source measurement must belong to this customer
                // and to the currently selected tailoring service.
                GarmentMeasurement existingMeasurement =
                        garmentMeasurementRepository
                                .findByIdAndUserIdAndServiceId(
                                        measurementId,
                                        customer.getId(),
                                        service.getId()
                                )
                                .orElseThrow(() ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Selected measurement does not belong to this customer and service"
                                        )
                                );

                // IMPORTANT:
                // The frontend sends BOTH the source measurement ID and
                // the current measurement values. If the values are the
                // same, reuse the existing snapshot. If the user changed
                // any measurement value, create a new snapshot instead of
                // modifying the old measurement.
                Map<String, Object> submittedFields =
                        measurementRequest.measurementFields() == null
                                ? new LinkedHashMap<>()
                                : new LinkedHashMap<>(
                                measurementRequest.measurementFields()
                        );

                Map<String, Object> existingFields =
                        existingMeasurement.getMeasurementFields() == null
                                ? new LinkedHashMap<>()
                                : existingMeasurement.getMeasurementFields();

                boolean sameMeasurement =
                        Objects.equals(existingFields, submittedFields);

                if (sameMeasurement) {
                    // Nothing changed. Reuse the existing measurement ID.
                    measurement = existingMeasurement;
                } else {
                    // Something changed. Create a new immutable measurement
                    // snapshot and leave the source measurement untouched.
                    measurement =
                            GarmentMeasurement.builder()
                                    .service(service)
                                    .user(customer)
                                    .takenBy(takenBy)
                                    .measurementFields(submittedFields)
                                    .alteration(
                                            measurementRequest.alteration() == null
                                                    ? new LinkedHashMap<>()
                                                    : new LinkedHashMap<>(
                                                    measurementRequest.alteration()
                                            )
                                    )
                                    .note(
                                            measurementRequest.note()
                                    )
                                    .alterationNote(
                                            measurementRequest.alterationNote()
                                    )
                                    .editable(
                                            measurementRequest.editable() == null
                                                    || measurementRequest.editable()
                                    )
                                    .build();

                    measurement =
                            garmentMeasurementRepository.save(measurement);
                }

            } else {

                // No source measurement was supplied, so this is a brand-new
                // measurement snapshot.
                Map<String, Object> submittedFields =
                        measurementRequest.measurementFields() == null
                                ? new LinkedHashMap<>()
                                : new LinkedHashMap<>(
                                measurementRequest.measurementFields()
                        );

                measurement =
                        GarmentMeasurement.builder()
                                .service(service)
                                .user(customer)
                                .takenBy(takenBy)
                                .measurementFields(submittedFields)
                                .alteration(
                                        measurementRequest.alteration() == null
                                                ? new LinkedHashMap<>()
                                                : new LinkedHashMap<>(
                                                measurementRequest.alteration()
                                        )
                                )
                                .note(
                                        measurementRequest.note()
                                )
                                .alterationNote(
                                        measurementRequest.alterationNote()
                                )
                                .editable(
                                        measurementRequest.editable() == null
                                                || measurementRequest.editable()
                                )
                                .build();

                measurement =
                        garmentMeasurementRepository.save(measurement);
            }

            // The garment points to the final measurement snapshot.
            // This ID is therefore either the original source ID or the
            // newly-created ID when the measurement was changed.
            savedGarment.setGarmentMeasurement(measurement);
            garmentRepository.save(savedGarment);


            // =====================================================
            // 6. CREATE ADD-ONS
            // =====================================================

            List<Long> garmentAddOnIds =
                    new ArrayList<>();


            if (garmentRequest.addOns() != null) {

                for (
                        CompleteOrderAddOnDto addOnRequest
                        : garmentRequest.addOns()
                ) {

                    // -------------------------------------------------
                    // FIND ADD-ON
                    // -------------------------------------------------

                    AddOn addOn =
                            addOnRepository.findById(
                                    addOnRequest.addOnId()
                            ).orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Add-on not found with id: "
                                                    + addOnRequest
                                                    .addOnId()
                                    )
                            );


                    // -------------------------------------------------
                    // CHECK ACTIVE
                    // -------------------------------------------------

                    if (!addOn.isActive()) {

                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Add-on is inactive: "
                                        + addOn.getName()
                        );
                    }


                    // -------------------------------------------------
                    // PRICE FROM DATABASE
                    // -------------------------------------------------

                    BigDecimal unitPrice =
                            addOn.getPrice();


                    BigDecimal addOnTotal =
                            unitPrice.multiply(
                                    BigDecimal.valueOf(
                                            addOnRequest.quantity()
                                    )
                            );


                    // -------------------------------------------------
                    // CREATE GARMENT ADD-ON SNAPSHOT
                    // -------------------------------------------------

                    GarmentAddOn garmentAddOn =
                            GarmentAddOn.builder()
                                    .garment(savedGarment)
                                    .addOn(addOn)
                                    .addOnName(
                                            addOn.getName()
                                    )
                                    .unitPrice(
                                            unitPrice
                                    )
                                    .quantity(
                                            addOnRequest.quantity()
                                    )
                                    .totalPrice(
                                            addOnTotal
                                    )
                                    .tailorCommission(
                                            addOn.getTailorCommission()
                                    )
                                    .note(
                                            addOnRequest.note()
                                    )
                                    .build();


                    GarmentAddOn savedGarmentAddOn =
                            garmentAddOnRepository.save(
                                    garmentAddOn
                            );


                    garmentAddOnIds.add(
                            savedGarmentAddOn.getId()
                    );


                    garmentTotal =
                            garmentTotal.add(
                                    addOnTotal
                            );
                }
            }


            // -----------------------------------------------------
            // ADD GARMENT TOTAL TO SUBTOTAL
            // -----------------------------------------------------

            subtotal =
                    subtotal.add(
                            garmentTotal
                    );


            // -----------------------------------------------------
            // ADD GARMENT RESPONSE
            // -----------------------------------------------------

            garmentResponses.add(
                    new CompleteOrderResponse
                            .GarmentCreatedResponse(
                            savedGarment.getId(),
                            service.getId(),
                            service.getName(),
                            garmentPrice,
                            savedGarment.getSampleNumber(),
                            measurement.getId(),
                            garmentAddOnIds
                    )
            );
        }


        // =========================================================
        // 7. ADDITIONAL CHARGES
        // =========================================================

        BigDecimal additionalChargesTotal =
                BigDecimal.ZERO;


        if (request.additionalCharges() != null) {

            for (
                    CompleteOrderChargeDto chargeRequest
                    : request.additionalCharges()
            ) {

                String description =
                        normalize(
                                chargeRequest.description()
                        );


                if (
                        description == null
                                || description.isBlank()
                ) {
                    continue;
                }


                if (
                        chargeRequest.amount()
                                .compareTo(
                                        BigDecimal.ZERO
                                ) <= 0
                ) {
                    continue;
                }


                AdditionalCharge charge =
                        AdditionalCharge.builder()
                                .order(savedOrder)
                                .description(description)
                                .amount(
                                        chargeRequest.amount()
                                )
                                .build();


                additionalChargeRepository.save(
                        charge
                );


                additionalChargesTotal =
                        additionalChargesTotal.add(
                                chargeRequest.amount()
                        );
            }
        }


        // =========================================================
        // 8. DISCOUNT
        // =========================================================

        BigDecimal discount =
                request.discount() != null
                        ? request.discount()
                        : BigDecimal.ZERO;


        if (
                discount.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount cannot be negative"
            );
        }


        BigDecimal beforeDiscount =
                subtotal.add(
                        additionalChargesTotal
                );


        if (
                discount.compareTo(
                        beforeDiscount
                ) > 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount cannot exceed order total"
            );
        }


        BigDecimal grandTotal =
                beforeDiscount.subtract(
                        discount
                );


        // =========================================================
        // 9. PAYMENT
        // =========================================================

        BigDecimal paymentAmount =
                BigDecimal.ZERO;


        String paymentMethod = null;

        String paymentType = null;


        if (request.payment() != null) {

            paymentAmount =
                    request.payment().amount();


            paymentMethod =
                    request.payment()
                            .paymentMethod()
                            .name();


            paymentType =
                    request.payment()
                            .paymentType()
                            .name();


            // -----------------------------------------------------
            // PAYMENT VALIDATION
            // -----------------------------------------------------

            if (
                    paymentAmount.compareTo(
                            BigDecimal.ZERO
                    ) < 0
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment amount cannot be negative"
                );
            }


            if (
                    paymentAmount.compareTo(
                            grandTotal
                    ) > 0
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment cannot exceed order total"
                );
            }


            // -----------------------------------------------------
            // CREATE PAYMENT
            // -----------------------------------------------------

            if (
                    paymentAmount.compareTo(
                            BigDecimal.ZERO
                    ) > 0
            ) {

                User receivedBy =
                        userRepository.findById(
                                request.payment()
                                        .receivedById()
                        ).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment receiver not found with id: "
                                                + request.payment()
                                                .receivedById()
                                )
                        );


                Payment payment =
                        Payment.builder()
                                .order(savedOrder)
                                .amount(paymentAmount)
                                .paymentMethod(
                                        request.payment()
                                                .paymentMethod()
                                )
                                .paymentType(
                                        request.payment()
                                                .paymentType()
                                )
                                .receivedBy(receivedBy)
                                .note(
                                        request.payment().note()
                                )
                                .paidAt(
                                        LocalDateTime.now()
                                )
                                .build();


                paymentRepository.save(
                        payment
                );
            }
        }


        // =========================================================
        // 10. BALANCE
        // =========================================================

        BigDecimal balanceDue =
                grandTotal.subtract(
                        paymentAmount
                );


        // =========================================================
        // 11. RETURN RESPONSE
        // =========================================================

        return new CompleteOrderResponse(
                savedOrder.getId(),
                customer.getId(),
                customer.getName(),
                customer.getCustomerCode(),
                savedOrder.getOrderDate(),
                savedOrder.getSampleBookPageNumber(),
                discount,
                subtotal,
                additionalChargesTotal,
                grandTotal,
                paymentAmount,
                balanceDue,
                paymentMethod,
                paymentType,
                garmentResponses,
                savedOrder.getCreatedAt()
        );
    }


    // ============================================================
    // FIND ORDER
    // ============================================================

    private Order findOrderById(
            Long orderId
    ) {

        return orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: "
                                        + orderId
                        )
                );
    }


    // ============================================================
    // FIND CUSTOMER
    // ============================================================

    private User findCustomerById(
            Long customerId
    ) {

        return userRepository
                .findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id: "
                                        + customerId
                        )
                );
    }


    // ============================================================
    // MAPPER
    // ============================================================

    private OrderResponse toResponse(
            Order order
    ) {

        User customer =
                order.getCustomer();

        return new OrderResponse(
                order.getId(),

                customer != null
                        ? customer.getId()
                        : null,

                customer != null
                        ? customer.getName()
                        : null,

                customer != null
                        ? customer.getCustomerCode()
                        : null,

                order.getOrderDate(),

                order.getSampleBookPageNumber(),

                order.getCreatedAt(),

                order.getUpdatedAt()
        );
    }


    // ============================================================
    // NORMALIZE
    // ============================================================

    private String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }


    // ============================================================
    // CUSTOMER CODE
    // ============================================================

    /**
     * Generates a customer code if the customer does not
     * already have one.
     *
     * Customer codes are generated from the customer's
     * database ID and therefore remain stable.
     */
    private void ensureCustomerCode(
            User customer
    ) {

        if (
                customer.getCustomerCode() != null
                        && !customer.getCustomerCode().isBlank()
        ) {

            return;
        }


        String customerCode =
                CustomerCodeHelper.generateFromUserId(
                        customer.getId()
                );


        customer.setCustomerCode(
                customerCode
        );


        userRepository.save(
                customer
        );
    }
    // ============================================================
    // NEXT SAMPLE BOOK PAGE NUMBER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public String getNextSampleBookPageNumber() {
        String latest = orderRepository.findLatestSampleBookPageNumber();
        if (latest == null || latest.isBlank()) {
            return "1";
        }
        try {
            // Parse numeric suffix and increment
            String stripped = latest.replaceAll("[^0-9]", "");
            if (stripped.isBlank()) {
                return latest + "-1";
            }
            long next = Long.parseLong(stripped) + 1;
            // Rebuild with same prefix (handles "PG-125" → "PG-126" or "125" → "126")
            String prefix = latest.replaceAll("[0-9]+$", "");
            return prefix + next;
        } catch (NumberFormatException ex) {
            return latest + "-1";
        }
    }


    // ============================================================
    // EDIT ORDER (DIFFERENTIAL UPDATE)
    // ============================================================

    @Override
    public CompleteOrderResponse editOrder(Long orderId, EditOrderRequestDto request) {

        // =========================================================
        // 1. LOAD ORDER
        // =========================================================

        Order order = findOrderById(orderId);

        // =========================================================
        // 2. UPDATE ORDER-LEVEL FIELDS
        // =========================================================

        if (request.orderDate() != null) {
            order.setOrderDate(request.orderDate());
        }

        if (request.sampleBookPageNumber() != null) {
            order.setSampleBookPageNumber(normalize(request.sampleBookPageNumber()));
        }

        if (request.discount() != null) {
            if (request.discount().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount cannot be negative");
            }
            order.setDiscount(request.discount());
        }

        User customer = order.getCustomer();

        // =========================================================
        // 3. FETCH EXISTING GARMENTS
        // =========================================================

        List<Garment> existingGarments = garmentRepository.findByOrderIdOrderByIdAsc(orderId);

        // Build a map of existingGarmentId -> Garment for quick lookup
        Map<Long, Garment> existingMap = new LinkedHashMap<>();
        for (Garment g : existingGarments) {
            existingMap.put(g.getId(), g);
        }

        // Track which existing garment IDs appear in the edit request
        Set<Long> submittedExistingIds = new HashSet<>();

        List<CompleteOrderResponse.GarmentCreatedResponse> garmentResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        // =========================================================
        // 4. PROCESS EACH GARMENT IN THE REQUEST
        // =========================================================

        for (EditOrderGarmentDto garmentRequest : request.garments()) {

            // Validate due date is present
            if (garmentRequest.dueDate() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Delivery date (dueDate) is mandatory for every garment"
                );
            }

            // Load service
            TailoringService service = tailoringServiceRepository
                    .findById(garmentRequest.serviceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tailoring service not found with id: " + garmentRequest.serviceId()
                    ));

            if (!service.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tailoring service is inactive: " + service.getName()
                );
            }

            BigDecimal garmentPrice = garmentRequest.price() != null
                    ? garmentRequest.price()
                    : service.getPrice();

            if (garmentPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Garment price cannot be negative");
            }

            Garment garment;

            if (garmentRequest.garmentId() != null) {
                // ------------------------------------------------
                // UPDATING AN EXISTING GARMENT
                // ------------------------------------------------
                Garment existing = existingMap.get(garmentRequest.garmentId());
                if (existing == null || !existing.getOrder().getId().equals(orderId)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Garment " + garmentRequest.garmentId() + " does not belong to order " + orderId
                    );
                }

                submittedExistingIds.add(existing.getId());

                if (existing.getStatus() != GarmentStatus.FABRIC_CUT) {
                    // ------------------------------------------------
                    // LOCKED GARMENT (Cutting started or completed)
                    // Do NOT modify anything on this garment!
                    // ------------------------------------------------
                    List<GarmentAddOn> lockedAddOns = garmentAddOnRepository.findByGarmentIdOrderByIdAsc(existing.getId());
                    List<Long> lockedAddOnIds = lockedAddOns.stream().map(GarmentAddOn::getId).toList();
                    BigDecimal lockedAddOnsTotal = lockedAddOns.stream()
                            .map(GarmentAddOn::getTotalPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Long lockedMeasurementId = existing.getGarmentMeasurement() != null
                            ? existing.getGarmentMeasurement().getId()
                            : null;

                    garmentResponses.add(new CompleteOrderResponse.GarmentCreatedResponse(
                            existing.getId(),
                            existing.getService().getId(),
                            existing.getService().getName(),
                            existing.getPrice(),
                            existing.getSampleNumber(),
                            lockedMeasurementId,
                            lockedAddOnIds
                    ));

                    if (existing.getStatus() != GarmentStatus.CANCELLED) {
                        BigDecimal existingBasePrice = existing.getPrice() != null ? existing.getPrice() : BigDecimal.ZERO;
                        subtotal = subtotal.add(existingBasePrice).add(lockedAddOnsTotal);
                    }
                } else {
                    // ------------------------------------------------
                    // EDITABLE GARMENT (Still in FABRIC_CUT / Pending Cutting)
                    // ------------------------------------------------
                    existing.setService(service);
                    existing.setDueDate(garmentRequest.dueDate());
                    existing.setTryDate(garmentRequest.tryDate());
                    existing.setNote(normalize(garmentRequest.note()));
                    existing.setSampleNumber(normalize(garmentRequest.sampleNumber()));
                    existing.setPrice(garmentPrice);
                    existing.setCustomerFabric(GarmentClassificationHelper.determineCustomerFabric(
                            garmentRequest.customerFabric(),
                            garmentPrice,
                            service.getPrice()
                    ));

                    garment = garmentRepository.save(existing);

                    // Update measurement via existing snapshot logic
                    CompleteOrderMeasurementDto mReq = garmentRequest.measurement();
                    GarmentMeasurement measurement = resolveOrCreateMeasurement(mReq, customer, service);
                    garment.setGarmentMeasurement(measurement);
                    garment = garmentRepository.save(garment);

                    // Update add-ons: remove old and replace
                    garmentAddOnRepository.deleteAllByGarmentId(garment.getId());
                    garmentAddOnRepository.flush();
                    List<Long> addOnIds = processAddOns(garment, garmentRequest.addOns(), service);

                    BigDecimal addOnsTotal = garmentAddOnRepository.findByGarmentIdOrderByIdAsc(garment.getId())
                            .stream()
                            .map(GarmentAddOn::getTotalPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    garmentResponses.add(new CompleteOrderResponse.GarmentCreatedResponse(
                            garment.getId(),
                            service.getId(),
                            service.getName(),
                            garment.getPrice(),
                            garment.getSampleNumber(),
                            measurement.getId(),
                            addOnIds
                    ));
                    subtotal = subtotal.add(garment.getPrice()).add(addOnsTotal);
                }

            } else {
                // ------------------------------------------------
                // NEW GARMENT — ADD TO EXISTING ORDER
                // ------------------------------------------------
                boolean isCustFabric = GarmentClassificationHelper.determineCustomerFabric(
                        garmentRequest.customerFabric(),
                        garmentPrice,
                        service.getPrice()
                );

                garment = Garment.builder()
                        .order(order)
                        .service(service)
                        .price(garmentPrice)
                        .customerFabric(isCustFabric)
                        .sampleNumber(normalize(garmentRequest.sampleNumber()))
                        .dueDate(garmentRequest.dueDate())
                        .tryDate(garmentRequest.tryDate())
                        .note(normalize(garmentRequest.note()))
                        .status(GarmentStatus.FABRIC_CUT)
                        .build();

                Garment savedGarment = garmentRepository.save(garment);

                CompleteOrderMeasurementDto mReq = garmentRequest.measurement();
                GarmentMeasurement measurement = resolveOrCreateMeasurement(mReq, customer, service);
                savedGarment.setGarmentMeasurement(measurement);
                savedGarment = garmentRepository.save(savedGarment);

                List<Long> addOnIds = processAddOns(savedGarment, garmentRequest.addOns(), service);

                BigDecimal addOnsTotal = garmentAddOnRepository.findByGarmentIdOrderByIdAsc(savedGarment.getId())
                        .stream()
                        .map(GarmentAddOn::getTotalPrice)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                garmentResponses.add(new CompleteOrderResponse.GarmentCreatedResponse(
                        savedGarment.getId(),
                        service.getId(),
                        service.getName(),
                        garmentPrice,
                        savedGarment.getSampleNumber(),
                        measurement.getId(),
                        addOnIds
                ));
                subtotal = subtotal.add(garmentPrice).add(addOnsTotal);
            }
        }

        // =========================================================
        // 5. HANDLE REMOVED GARMENTS
        // =========================================================

        for (Map.Entry<Long, Garment> entry : existingMap.entrySet()) {
            Long existingId = entry.getKey();
            Garment existing = entry.getValue();

            if (!submittedExistingIds.contains(existingId)) {
                // This garment was NOT included in the edit request → user wants it removed

                // PRODUCTION SAFETY CHECK: Cannot delete a garment that has progressed in production
                GarmentStatus status = existing.getStatus();
                boolean canDelete = (status == GarmentStatus.FABRIC_CUT || status == GarmentStatus.CANCELLED);

                if (!canDelete) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Garment '" + existing.getService().getName() + "' (ID: " + existingId +
                            ") cannot be removed because production has already started (status: " + status + ")"
                    );
                }

                garmentRepository.delete(existing);
            }
        }

        // =========================================================
        // 6. ADDITIONAL CHARGES (replace all)
        // =========================================================

        BigDecimal additionalChargesTotal = BigDecimal.ZERO;

        additionalChargeRepository.deleteAllByOrderId(orderId);

        if (request.additionalCharges() != null) {
            for (CompleteOrderChargeDto chargeRequest : request.additionalCharges()) {
                String desc = normalize(chargeRequest.description());
                if (desc == null) continue;
                BigDecimal amount = chargeRequest.amount() != null ? chargeRequest.amount() : BigDecimal.ZERO;
                if (amount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Charge amount cannot be negative");
                }
                AdditionalCharge charge = AdditionalCharge.builder()
                        .order(order)
                        .description(desc)
                        .amount(amount)
                        .build();
                additionalChargeRepository.save(charge);
                additionalChargesTotal = additionalChargesTotal.add(amount);
            }
        }

        // =========================================================
        // 7. BILLING
        // =========================================================

        BigDecimal discount = order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO;
        BigDecimal grandTotal = subtotal.add(additionalChargesTotal).subtract(discount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        // =========================================================
        // 8. OPTIONAL NEW PAYMENT
        // =========================================================

        BigDecimal newPaymentAmount = BigDecimal.ZERO;

        if (request.payment() != null && request.payment().amount() != null
                && request.payment().amount().compareTo(BigDecimal.ZERO) > 0) {

            User receivedBy = userRepository.findById(request.payment().receivedById())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment receiver not found with id: " + request.payment().receivedById()
                    ));

            Payment payment = Payment.builder()
                    .order(order)
                    .amount(request.payment().amount())
                    .paymentMethod(request.payment().paymentMethod())
                    .paymentType(request.payment().paymentType())
                    .receivedBy(receivedBy)
                    .note(request.payment().note())
                    .paidAt(java.time.LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
            newPaymentAmount = payment.getAmount();
        }

        // Total paid across all payments
        List<Payment> allPayments = paymentRepository.findByOrderIdOrderByPaidAtAscIdAsc(orderId);
        BigDecimal totalPaid = allPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balanceDue = grandTotal.subtract(totalPaid);
        if (balanceDue.compareTo(BigDecimal.ZERO) < 0) balanceDue = BigDecimal.ZERO;

        Order savedOrder = orderRepository.save(order);

//        public record CompleteOrderResponse(
//
//                Long orderId,
//
//                Long customerId,
//
//                String customerName,
//
//                String customerCode,
//
//                LocalDate orderDate,
//
//                String sampleBookPageNumber,
//
//                BigDecimal discount,
//
//                BigDecimal subtotal,
//
//                BigDecimal additionalCharges,
//
//                BigDecimal grandTotal,
//
//                BigDecimal paymentAmount,
//
//                BigDecimal balanceDue,
//
//                String paymentMethod,
//
//                String paymentType,
//
//                List<com.sff.authapp.login.dtos.order.CompleteOrderResponse.GarmentCreatedResponse> garments,
//
//                LocalDateTime createdAt
//
//        )
        return new CompleteOrderResponse(
                savedOrder.getId(),
                customer.getId(),
                customer.getName(),
                customer.getCustomerCode(),
                savedOrder.getOrderDate(),
                savedOrder.getSampleBookPageNumber(),
                discount,
                subtotal,
                additionalChargesTotal,
                grandTotal,
                totalPaid,
                balanceDue,
                request.payment() != null ? request.payment().paymentMethod().name() : null,
                request.payment() != null ? request.payment().paymentType().name() : null,
                garmentResponses,
                savedOrder.getCreatedAt()
        );
    }


    // ============================================================
    // PRIVATE HELPERS FOR editOrder
    // ============================================================

    /**
     * Resolves or creates a measurement snapshot from the measurement DTO.
     * Reuses an existing measurement if values are unchanged.
     * Creates a new immutable snapshot if values differ.
     */
    private GarmentMeasurement resolveOrCreateMeasurement(
            CompleteOrderMeasurementDto mReq,
            User customer,
            TailoringService service
    ) {
        if (mReq == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Measurement is required for service: " + service.getName()
            );
        }

        User takenBy = userRepository.findById(mReq.takenByUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Measurement user not found with id: " + mReq.takenByUserId()
                ));

        if (mReq.measurementId() != null) {
            GarmentMeasurement existing = garmentMeasurementRepository
                    .findByIdAndUserIdAndServiceId(mReq.measurementId(), customer.getId(), service.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Selected measurement does not belong to this customer and service"
                    ));

            Map<String, Object> submitted = mReq.measurementFields() == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(mReq.measurementFields());
            Map<String, Object> stored = existing.getMeasurementFields() == null
                    ? new LinkedHashMap<>() : existing.getMeasurementFields();

            if (Objects.equals(stored, submitted)) {
                return existing; // Same values — reuse snapshot
            }
        }

        // Create new immutable snapshot
        Map<String, Object> fields = mReq.measurementFields() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(mReq.measurementFields());

        GarmentMeasurement snapshot = GarmentMeasurement.builder()
                .service(service)
                .user(customer)
                .takenBy(takenBy)
                .measurementFields(fields)
                .alteration(mReq.alteration() == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>(mReq.alteration()))
                .note(mReq.note())
                .alterationNote(mReq.alterationNote())
                .editable(mReq.editable() == null || mReq.editable())
                .build();

        return garmentMeasurementRepository.save(snapshot);
    }


    /**
     * Processes the add-on list for a garment, using historical price snapshots.
     * Returns list of created GarmentAddOn IDs.
     */
    private List<Long> processAddOns(
            Garment garment,
            java.util.List<CompleteOrderAddOnDto> addOnDtos,
            TailoringService service
    ) {
        List<Long> ids = new ArrayList<>();
        if (addOnDtos == null) return ids;

        for (CompleteOrderAddOnDto addOnDto : addOnDtos) {
            AddOn addOn = addOnRepository.findById(addOnDto.addOnId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Add-on not found with id: " + addOnDto.addOnId()
                    ));

            int qty = addOnDto.quantity() != null && addOnDto.quantity() > 0 ? addOnDto.quantity() : 1;
            BigDecimal unitPrice = addOn.getPrice();
            BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal commission = addOn.getTailorCommission() != null
                    ? addOn.getTailorCommission().multiply(BigDecimal.valueOf(qty))
                    : BigDecimal.ZERO;

            GarmentAddOn ga = GarmentAddOn.builder()
                    .garment(garment)
                    .addOn(addOn)
                    .addOnName(addOn.getName())
                    .unitPrice(unitPrice)
                    .quantity(qty)
                    .totalPrice(total)
                    .tailorCommission(commission)
                    .note(addOnDto.note())
                    .build();

            ids.add(garmentAddOnRepository.save(ga).getId());
        }
        return ids;
    }


    private OrderDetailsResponse.GarmentDetails mapGarmentDetails(
            Garment garment
    ) {

        // ============================================================
        // MEASUREMENT
        // ============================================================

        OrderDetailsResponse.MeasurementDetails measurementDetails =
                null;

        GarmentMeasurement measurement =
                garment.getGarmentMeasurement();

        if (measurement != null) {

            measurementDetails =
                    new OrderDetailsResponse.MeasurementDetails(
                            measurement.getId(),

                            measurement.getMeasurementFields(),

                            measurement.getAlteration(),

                            measurement.getTakenBy() != null
                                    ? measurement.getTakenBy().getId()
                                    : null,

                            measurement.isEditable(),

                            measurement.getNote(),

                            measurement.getAlterationNote(),

                            measurement.getCreatedAt()
                    );
        }


        // ============================================================
        // ADD-ONS
        // ============================================================

        List<OrderDetailsResponse.AddOnDetails> addOnDetails =
                garment.getAddOns()
                        .stream()
                        .map(addOn ->
                                new OrderDetailsResponse.AddOnDetails(
                                        addOn.getId(),

                                        addOn.getAddOn() != null
                                                ? addOn.getAddOn().getId()
                                                : null,

                                        addOn.getAddOnName(),

                                        addOn.getQuantity(),

                                        addOn.getUnitPrice(),

                                        addOn.getTotalPrice(),

                                        addOn.getNote()
                                )
                        )
                        .toList();


        // ============================================================
        // GARMENT RESPONSE
        // ============================================================

        return new OrderDetailsResponse.GarmentDetails(

                garment.getId(),

                garment.getService() != null
                        ? garment.getService().getId()
                        : null,

                garment.getService() != null
                        ? garment.getService().getName()
                        : null,

                garment.getPrice(),

                garment.getSampleNumber(),

                garment.getStatus(),

                garment.isCustomerFabric(),

                garment.getTryDate(),

                garment.getDueDate(),

                garment.getAlterationDeliveryDate(),

                garment.getAlterationNote(),

                garment.getNote(),

                garment.getCuttingBy() != null ? garment.getCuttingBy().getId() : null,
                garment.getCuttingBy() != null ? garment.getCuttingBy().getName() : null,
                garment.getMadeBy() != null ? garment.getMadeBy().getId() : null,
                garment.getMadeBy() != null ? garment.getMadeBy().getName() : null,
                garment.getMadeBy() != null ? garment.getMadeBy().getPhone() : null,
                garment.getMadeBy() != null ? garment.getMadeBy().getImage() : null,

                measurementDetails,

                addOnDetails
        );
    }

    @Override
    @Transactional
    public OrderDeliveryResponseDto deliverOrderGarments(
            Long orderId,
            OrderDeliveryRequestDto request
    ) {
        Order order = findOrderById(orderId);
        List<Garment> garments = order.getGarments();

        User deliveredByUser = null;
        if (request.deliveredById() != null) {
            deliveredByUser = userRepository.findById(request.deliveredById()).orElse(null);
        }

        List<Long> newlyDelivered = new ArrayList<>();
        Set<Long> targetIds = new HashSet<>(request.garmentIds());

        for (Garment g : garments) {
            if (targetIds.contains(g.getId())) {
                g.setStatus(GarmentStatus.DELIVERED);
                if (deliveredByUser != null) {
                    g.setDeliveredBy(deliveredByUser);
                }
                if (request.deliveryNote() != null && !request.deliveryNote().isBlank()) {
                    String existing = g.getNote();
                    g.setNote(existing != null && !existing.isBlank()
                            ? existing + " | Delivery: " + request.deliveryNote().trim()
                            : "Delivery: " + request.deliveryNote().trim());
                }
                newlyDelivered.add(g.getId());
            }
        }
        garmentRepository.saveAll(garments);

        // Optional payment collection
        boolean paymentRecorded = false;
        if (request.paymentAmount() != null && request.paymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            PaymentMethod pm = request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.CASH;
            PaymentType pt = request.paymentType() != null ? request.paymentType() : PaymentType.FINAL;
            User receivedBy = deliveredByUser != null ? deliveredByUser : order.getCustomer();

            Payment payment = Payment.builder()
                    .order(order)
                    .amount(request.paymentAmount())
                    .paymentMethod(pm)
                    .paymentType(pt)
                    .receivedBy(receivedBy)
                    .note(request.paymentNote() != null && !request.paymentNote().isBlank()
                            ? request.paymentNote().trim()
                            : "Collected on delivery")
                    .paidAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
            paymentRecorded = true;
        }

        int totalCount = garments.size();
        int deliveredCount = (int) garments.stream()
                .filter(g -> g.getStatus() == GarmentStatus.DELIVERED)
                .count();
        boolean isFullyDelivered = (deliveredCount == totalCount);

        // Calculate totals and balance
        BigDecimal subtotal = garments.stream()
                .map(Garment::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal addOnTotal = garments.stream()
                .flatMap(g -> g.getAddOns().stream())
                .map(GarmentAddOn::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotal = subtotal.add(addOnTotal);

        List<AdditionalCharge> charges = additionalChargeRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
        BigDecimal additionalCharges = charges.stream()
                .map(AdditionalCharge::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO;
        BigDecimal grandTotal = subtotal.add(additionalCharges).subtract(discount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) grandTotal = BigDecimal.ZERO;

        List<Payment> allPayments = paymentRepository.findByOrderIdOrderByPaidAtAscIdAsc(order.getId());
        BigDecimal totalPaid = allPayments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balanceDue = grandTotal.subtract(totalPaid);
        if (balanceDue.compareTo(BigDecimal.ZERO) < 0) balanceDue = BigDecimal.ZERO;

        String msg = isFullyDelivered
                ? "All " + totalCount + " garments in Order #" + orderId + " delivered successfully."
                : newlyDelivered.size() + " garment(s) marked delivered (" + deliveredCount + "/" + totalCount + " total delivered).";

        return new OrderDeliveryResponseDto(
                order.getId(),
                totalCount,
                deliveredCount,
                isFullyDelivered,
                newlyDelivered,
                grandTotal,
                totalPaid,
                balanceDue,
                paymentRecorded,
                msg
        );
    }
}
