package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.billing.BillingDtos.*;
import com.sff.authapp.login.dtos.order.PublicOrderTrackingDtos.*;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.*;
import com.sff.authapp.login.services.PublicOrderTrackingService;
import com.sff.authapp.login.services.billing.BillingConfigService;
import com.sff.authapp.login.services.billing.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicOrderTrackingServiceImpl implements PublicOrderTrackingService {

    private final OrderRepository orderRepository;
    private final GarmentRepository garmentRepository;
    private final PaymentRepository paymentRepository;
    private final AdditionalChargeRepository additionalChargeRepository;
    private final OrderReviewRepository orderReviewRepository;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final BillingService billingService;
    private final BillingConfigService billingConfigService;
    private final OrderAccessLogRepository orderAccessLogRepository;
    private final MessageLogRepository messageLogRepository;

    @Override
    @Transactional
    public PublicOrderTrackingResponse trackOrder(String token) {
        Order order = resolveOrder(token);

        List<Garment> garments = garmentRepository.findCompleteGarmentsByOrderId(order.getId());
        List<AdditionalCharge> charges = additionalChargeRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
        List<Payment> payments = paymentRepository.findByOrderIdOrderByPaidAtAscIdAsc(order.getId());

        // 1. Calculate Financials
        PublicOrderFinancials financials = calculateFinancials(order, garments, charges, payments);

        // 2. Derive Status & Dominant Stage
        OrderStatusSummary statusSummary = deriveStatusSummary(order, garments);

        // 3. Build Timeline
        List<PublicTimelineStageDto> timeline = buildTimeline(order, garments, statusSummary);

        // 4. Map Customer-Safe Garment Details (Zero private notes or worker data)
        List<PublicGarmentDto> garmentDtos = garments.stream()
                .map(this::mapToPublicGarmentDto)
                .toList();

        // 5. Review info
        Optional<OrderReview> reviewOpt = orderReviewRepository.findByOrderId(order.getId());
        PublicOrderReviewDto reviewDto = reviewOpt.map(this::mapToReviewDto).orElse(null);
        boolean canReview = reviewOpt.isEmpty();

        // 6. Customer masking
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : "Valued Customer";
        String customerPhone = order.getCustomer() != null ? order.getCustomer().getPhone() : "";
        String maskedPhone = maskPhoneNumber(customerPhone);

        // 7. Estimated / Earliest delivery or try date
        LocalDate estimatedDate = null;
        for (Garment g : garments) {
            if (g.getDueDate() != null) {
                if (estimatedDate == null || g.getDueDate().isAfter(estimatedDate)) {
                    estimatedDate = g.getDueDate();
                }
            } else if (g.getTryDate() != null) {
                if (estimatedDate == null || g.getTryDate().isAfter(estimatedDate)) {
                    estimatedDate = g.getTryDate();
                }
            }
        }
        if (estimatedDate == null) {
            estimatedDate = order.getOrderDate() != null ? order.getOrderDate().plusDays(7) : LocalDate.now().plusDays(7);
        }

        PublicOrderTrackingResponse response = PublicOrderTrackingResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .trackingToken(order.getEffectiveTrackingToken())
                .orderDate(order.getOrderDate())
                .estimatedDeliveryDate(estimatedDate)
                .status(statusSummary.status)
                .statusLabel(statusSummary.statusLabel)
                .currentStage(statusSummary.currentStage)
                .progressPercentage(statusSummary.progressPercentage)
                .isDelivered(statusSummary.isDelivered)
                .deliveryDate(statusSummary.isDelivered ? (order.getUpdatedAt() != null ? order.getUpdatedAt().toLocalDate() : LocalDate.now()) : null)
                .customerName(customerName)
                .customerPhoneMasked(maskedPhone)
                .sampleBookPageNumber(order.getSampleBookPageNumber())
                .financials(financials)
                .timeline(timeline)
                .garments(garmentDtos)
                .review(reviewDto)
                .canReview(canReview)
                .lastRefreshedAt(LocalDateTime.now())
                .build();

        // Asynchronously/safely log customer tracking visit & mark message as clicked
        try {
            orderAccessLogRepository.save(OrderAccessLog.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .trackingToken(order.getEffectiveTrackingToken())
                    .customerName(customerName)
                    .ipAddress("127.0.0.1")
                    .userAgent("Customer Mobile Browser")
                    .deviceType("MOBILE")
                    .accessedAt(LocalDateTime.now())
                    .build());

            List<MessageLog> matchingLogs = messageLogRepository.findByTrackingUrlContaining(order.getEffectiveTrackingToken());
            for (MessageLog m : matchingLogs) {
                if ("SENT".equals(m.getStatus()) || "DELIVERED".equals(m.getStatus())) {
                    m.setStatus("CLICKED");
                    m.setClickedAt(LocalDateTime.now());
                    messageLogRepository.save(m);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to record customer link access: {}", ex.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public PublicOrderReviewDto submitReview(String token, PublicOrderReviewRequest request) {
        Order order = resolveOrder(token);

        if (orderReviewRepository.existsByOrderId(order.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A review has already been submitted for this order."
            );
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be between 1 and 5 stars."
            );
        }

        // Sanitize review comment: remove HTML tags and truncate to 500 characters
        String rawComment = request.getComment() != null ? request.getComment().trim() : "";
        String cleanComment = rawComment.replaceAll("<[^>]*>", "");
        if (cleanComment.length() > 500) {
            cleanComment = cleanComment.substring(0, 500);
        }

        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : "Customer";

        OrderReview review = OrderReview.builder()
                .order(order)
                .rating(request.getRating())
                .comment(cleanComment)
                .customerName(customerName)
                .build();

        OrderReview saved = orderReviewRepository.save(review);
        log.info("Public review submitted for Order #{} by {}", order.getOrderNumber(), customerName);

        return mapToReviewDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDto getPublicInvoice(String token) {
        Order order = resolveOrder(token);

        // Check if formal invoice exists in invoice register
        List<InvoiceOrder> activeInvoiceOrders = invoiceOrderRepository.findActiveInvoiceOrdersByOrderId(order.getId());
        if (!activeInvoiceOrders.isEmpty()) {
            Long invoiceId = activeInvoiceOrders.get(0).getInvoice().getId();
            return billingService.getInvoiceById(invoiceId);
        }

        // Otherwise generate on-the-fly public invoice/bill representation
        return buildOnTheFlyInvoiceDto(order);
    }

    private Order resolveOrder(String token) {
        if (token == null || token.isBlank()) {
            throw new ResourceNotFoundException("Tracking token is required.");
        }

        String cleanToken = token.trim();

        // 1. Try finding by tracking token
        Optional<Order> orderOpt = orderRepository.findByTrackingToken(cleanToken);
        if (orderOpt.isPresent()) {
            return orderOpt.get();
        }

        // 2. Try finding by order number or id if numeric
        if (cleanToken.matches("^\\d+$")) {
            try {
                Long numericValue = Long.parseLong(cleanToken);
                Optional<Order> byOrderNum = orderRepository.findByOrderNumber(numericValue);
                if (byOrderNum.isPresent()) {
                    Order ord = byOrderNum.get();
                    if (ord.getTrackingToken() == null) {
                        ord.getEffectiveTrackingToken();
                        orderRepository.save(ord);
                    }
                    return ord;
                }

                Optional<Order> byId = orderRepository.findById(numericValue);
                if (byId.isPresent()) {
                    Order ord = byId.get();
                    if (ord.getTrackingToken() == null) {
                        ord.getEffectiveTrackingToken();
                        orderRepository.save(ord);
                    }
                    return ord;
                }
            } catch (NumberFormatException ignored) {}
        }

        throw new ResourceNotFoundException("No order found with the provided tracking token or order number.");
    }

    private PublicOrderFinancials calculateFinancials(
            Order order,
            List<Garment> garments,
            List<AdditionalCharge> charges,
            List<Payment> payments
    ) {
        BigDecimal subtotal = garments.stream()
                .map(Garment::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal addOnsTotal = garments.stream()
                .filter(g -> g.getAddOns() != null)
                .flatMap(g -> g.getAddOns().stream())
                .map(GarmentAddOn::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        subtotal = subtotal.add(addOnsTotal);

        BigDecimal additionalCharges = charges.stream()
                .map(AdditionalCharge::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO;

        BigDecimal grandTotal = subtotal.add(additionalCharges).subtract(discount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balanceDue = grandTotal.subtract(totalPaid);
        if (balanceDue.compareTo(BigDecimal.ZERO) < 0) {
            balanceDue = BigDecimal.ZERO;
        }

        boolean isFullyPaid = balanceDue.compareTo(BigDecimal.ZERO) == 0;

        return PublicOrderFinancials.builder()
                .subtotal(subtotal)
                .discount(discount)
                .additionalCharges(additionalCharges)
                .totalAmount(grandTotal)
                .amountPaid(totalPaid)
                .balanceDue(balanceDue)
                .isFullyPaid(isFullyPaid)
                .build();
    }

    private OrderStatusSummary deriveStatusSummary(Order order, List<Garment> garments) {
        if (Boolean.TRUE.equals(order.getIsDelivered())) {
            return new OrderStatusSummary("DELIVERED", "Delivered", "All items delivered to customer", 100, true);
        }

        if (garments.isEmpty()) {
            return new OrderStatusSummary("BOOKED", "Order Confirmed", "Measurements recorded and scheduled", 20, false);
        }

        long deliveredCount = garments.stream().filter(g -> g.getStatus() == GarmentStatus.DELIVERED).count();
        long readyCount = garments.stream().filter(g -> g.getStatus() == GarmentStatus.READY).count();
        long stitchingCount = garments.stream().filter(g -> g.getStatus() == GarmentStatus.STITCHING || g.getStatus() == GarmentStatus.IRONING).count();
        long cuttingCount = garments.stream().filter(g -> g.getStatus() == GarmentStatus.CUTTING || g.getStatus() == GarmentStatus.FABRIC_CUT).count();
        int total = garments.size();

        if (deliveredCount == total) {
            return new OrderStatusSummary("DELIVERED", "Delivered", "Order handed over successfully", 100, true);
        }

        if (deliveredCount + readyCount == total) {
            return new OrderStatusSummary("READY", "Ready for Pickup", "Garments finished and ready for fitting / trial", 90, false);
        }

        if (readyCount > 0) {
            return new OrderStatusSummary("IN_PROGRESS", "In Tailoring", readyCount + " of " + total + " garments ready for trial", 75, false);
        }

        if (stitchingCount > 0) {
            return new OrderStatusSummary("IN_PROGRESS", "Stitching Phase", "Garments currently being handcrafted & stitched (" + stitchingCount + "/" + total + ")", 55, false);
        }

        if (cuttingCount > 0) {
            return new OrderStatusSummary("IN_PROGRESS", "Cutting Phase", "Fabric drafting and master cutting in progress (" + cuttingCount + "/" + total + ")", 35, false);
        }

        return new OrderStatusSummary("BOOKED", "Order Confirmed", "Order received and scheduled for cutting", 20, false);
    }

    private List<PublicTimelineStageDto> buildTimeline(
            Order order,
            List<Garment> garments,
            OrderStatusSummary statusSummary
    ) {
        List<PublicTimelineStageDto> stages = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");

        String orderDateStr = order.getOrderDate() != null ? order.getOrderDate().format(dtf) : "";

        // 1. Booked Stage
        stages.add(PublicTimelineStageDto.builder()
                .stageKey("BOOKED")
                .title("Order Confirmed")
                .description("Measurements taken & order recorded")
                .state("COMPLETED")
                .timestamp(orderDateStr)
                .build());

        // 2. Cutting Stage
        boolean cuttingDone = garments.stream().allMatch(g ->
                g.getStatus() == GarmentStatus.STITCHING ||
                g.getStatus() == GarmentStatus.IRONING ||
                g.getStatus() == GarmentStatus.READY ||
                g.getStatus() == GarmentStatus.DELIVERED
        ) && !garments.isEmpty();

        boolean cuttingActive = statusSummary.status.equals("IN_PROGRESS") &&
                garments.stream().anyMatch(g -> g.getStatus() == GarmentStatus.CUTTING || g.getStatus() == GarmentStatus.FABRIC_CUT);

        stages.add(PublicTimelineStageDto.builder()
                .stageKey("CUTTING")
                .title("Fabric Cutting")
                .description("Precision pattern cutting by master tailor")
                .state(cuttingDone ? "COMPLETED" : (cuttingActive ? "CURRENT" : (statusSummary.status.equals("BOOKED") ? "PENDING" : "COMPLETED")))
                .timestamp(cuttingDone ? "Cut Complete" : (cuttingActive ? "In Progress" : ""))
                .build());

        // 3. Stitching Stage
        boolean stitchingDone = garments.stream().allMatch(g ->
                g.getStatus() == GarmentStatus.READY || g.getStatus() == GarmentStatus.DELIVERED
        ) && !garments.isEmpty();

        boolean stitchingActive = garments.stream().anyMatch(g ->
                g.getStatus() == GarmentStatus.STITCHING || g.getStatus() == GarmentStatus.IRONING
        );

        stages.add(PublicTimelineStageDto.builder()
                .stageKey("STITCHING")
                .title("Tailoring & Stitching")
                .description("Handcrafted stitching, fusing & finishing")
                .state(stitchingDone ? "COMPLETED" : (stitchingActive ? "CURRENT" : (cuttingDone ? "CURRENT" : "PENDING")))
                .timestamp(stitchingDone ? "Stitched" : (stitchingActive ? "On Machine" : ""))
                .build());

        // 4. Ready Stage
        boolean readyDone = (garments.stream().allMatch(g -> g.getStatus() == GarmentStatus.READY || g.getStatus() == GarmentStatus.DELIVERED) && !garments.isEmpty()) || statusSummary.status.equals("READY") || statusSummary.isDelivered;
        boolean readyActive = statusSummary.status.equals("READY");

        stages.add(PublicTimelineStageDto.builder()
                .stageKey("READY")
                .title("Ready for Pickup")
                .description("Pressed, quality-checked and ready for trial")
                .state(statusSummary.isDelivered ? "COMPLETED" : (readyActive ? "CURRENT" : (readyDone ? "COMPLETED" : "PENDING")))
                .timestamp(readyActive ? "Ready at Shop" : "")
                .build());

        // 5. Delivered Stage
        stages.add(PublicTimelineStageDto.builder()
                .stageKey("DELIVERED")
                .title("Delivered")
                .description("Order delivered to customer")
                .state(statusSummary.isDelivered ? "COMPLETED" : "PENDING")
                .timestamp(statusSummary.isDelivered && order.getUpdatedAt() != null ? order.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "")
                .build());

        return stages;
    }

    private PublicGarmentDto mapToPublicGarmentDto(Garment garment) {
        String serviceName = garment.getService() != null ? garment.getService().getName() : "Custom Garment";
        String statusLabel = formatGarmentStatusLabel(garment.getStatus());

        List<String> addOnNames = new ArrayList<>();
        if (garment.getAddOns() != null) {
            for (GarmentAddOn addOn : garment.getAddOns()) {
                if (addOn.getAddOn() != null && addOn.getAddOn().getName() != null) {
                    addOnNames.add(addOn.getAddOn().getName());
                }
            }
        }

        return PublicGarmentDto.builder()
                .garmentId(garment.getId())
                .serviceName(serviceName)
                .price(garment.getPrice())
                .sampleNumber(garment.getSampleNumber())
                .status(garment.getStatus() != null ? garment.getStatus().name() : "FABRIC_CUT")
                .statusLabel(statusLabel)
                .customerFabric(garment.isCustomerFabric())
                .fabricSourceLabel(garment.isCustomerFabric() ? "Customer Fabric" : "Shop Provided Fabric")
                .tryDate(garment.getTryDate())
                .dueDate(garment.getDueDate())
                .addOns(addOnNames)
                .build();
    }

    private String formatGarmentStatusLabel(GarmentStatus status) {
        if (status == null) return "Scheduled";
        return switch (status) {
            case FABRIC_CUT -> "Fabric Cut";
            case CUTTING -> "Cutting";
            case STITCHING -> "Stitching";
            case IRONING -> "Ironing & Finish";
            case READY -> "Ready for Trial";
            case DELIVERED -> "Delivered";
            case ALTERATION -> "Under Alteration";
            case CANCELLED -> "Cancelled";
            case WASHING -> "Washing";
        };
    }

    private PublicOrderReviewDto mapToReviewDto(OrderReview review) {
        return PublicOrderReviewDto.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .customerName(review.getCustomerName())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String clean = phone.replaceAll("[^0-9]", "");
        if (clean.length() <= 4) return clean;
        return "******" + clean.substring(clean.length() - 4);
    }

    private InvoiceResponseDto buildOnTheFlyInvoiceDto(Order order) {
        BillingConfigDto config = billingConfigService.getConfigDto();
        List<Garment> garments = garmentRepository.findCompleteGarmentsByOrderId(order.getId());
        List<Payment> payments = paymentRepository.findByOrderIdOrderByPaidAtAscIdAsc(order.getId());
        List<AdditionalCharge> charges = additionalChargeRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId());

        PublicOrderFinancials fin = calculateFinancials(order, garments, charges, payments);

        List<InvoiceItemResponseDto> items = new ArrayList<>();
        int itemIndex = 1;

        for (Garment g : garments) {
            String serviceName = g.getService() != null ? g.getService().getName() : "Custom Tailoring";
            BigDecimal gPrice = g.getPrice() != null ? g.getPrice() : BigDecimal.ZERO;

            items.add(InvoiceItemResponseDto.builder()
                    .id((long) itemIndex)
                    .itemOrder(itemIndex++)
                    .itemType("SERVICE")
                    .description(serviceName + (g.getSampleNumber() != null ? " (" + g.getSampleNumber() + ")" : ""))
                    .hsnSacCode("998822")
                    .quantity(BigDecimal.ONE)
                    .unit("PCS")
                    .rate(gPrice)
                    .grossAmount(gPrice)
                    .discount(BigDecimal.ZERO)
                    .taxableAmount(gPrice)
                    .gstRate(BigDecimal.ZERO)
                    .cgstRate(BigDecimal.ZERO)
                    .sgstRate(BigDecimal.ZERO)
                    .igstRate(BigDecimal.ZERO)
                    .cgstAmount(BigDecimal.ZERO)
                    .sgstAmount(BigDecimal.ZERO)
                    .igstAmount(BigDecimal.ZERO)
                    .totalTax(BigDecimal.ZERO)
                    .totalAmount(gPrice)
                    .garmentId(g.getId())
                    .orderId(order.getId())
                    .build());

            if (g.getAddOns() != null) {
                for (GarmentAddOn addOn : g.getAddOns()) {
                    String addOnName = addOn.getAddOn() != null ? addOn.getAddOn().getName() : "Add-on";
                    BigDecimal addOnPrice = addOn.getTotalPrice() != null ? addOn.getTotalPrice() : BigDecimal.ZERO;
                    items.add(InvoiceItemResponseDto.builder()
                            .id((long) itemIndex)
                            .itemOrder(itemIndex++)
                            .itemType("ADD_ON")
                            .description(addOnName + " (for " + serviceName + ")")
                            .hsnSacCode("998822")
                            .quantity(BigDecimal.valueOf(addOn.getQuantity() != null ? addOn.getQuantity() : 1))
                            .unit("PCS")
                            .rate(addOn.getUnitPrice() != null ? addOn.getUnitPrice() : addOnPrice)
                            .grossAmount(addOnPrice)
                            .discount(BigDecimal.ZERO)
                            .taxableAmount(addOnPrice)
                            .gstRate(BigDecimal.ZERO)
                            .cgstRate(BigDecimal.ZERO)
                            .sgstRate(BigDecimal.ZERO)
                            .igstRate(BigDecimal.ZERO)
                            .cgstAmount(BigDecimal.ZERO)
                            .sgstAmount(BigDecimal.ZERO)
                            .igstAmount(BigDecimal.ZERO)
                            .totalTax(BigDecimal.ZERO)
                            .totalAmount(addOnPrice)
                            .garmentId(g.getId())
                            .orderId(order.getId())
                            .build());
                }
            }
        }

        for (AdditionalCharge c : charges) {
            BigDecimal amt = c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO;
            items.add(InvoiceItemResponseDto.builder()
                    .id((long) itemIndex)
                    .itemOrder(itemIndex++)
                    .itemType("CHARGE")
                    .description(c.getDescription() != null ? c.getDescription() : "Additional Charge")
                    .hsnSacCode("998822")
                    .quantity(BigDecimal.ONE)
                    .unit("LS")
                    .rate(amt)
                    .grossAmount(amt)
                    .discount(BigDecimal.ZERO)
                    .taxableAmount(amt)
                    .gstRate(BigDecimal.ZERO)
                    .cgstRate(BigDecimal.ZERO)
                    .sgstRate(BigDecimal.ZERO)
                    .igstRate(BigDecimal.ZERO)
                    .cgstAmount(BigDecimal.ZERO)
                    .sgstAmount(BigDecimal.ZERO)
                    .igstAmount(BigDecimal.ZERO)
                    .totalTax(BigDecimal.ZERO)
                    .totalAmount(amt)
                    .orderId(order.getId())
                    .build());
        }

        List<InvoicePaymentResponseDto> paymentDtos = payments.stream()
                .map(p -> InvoicePaymentResponseDto.builder()
                        .id(p.getId())
                        .amount(p.getAmount())
                        .paymentDate(p.getPaidAt() != null ? p.getPaidAt().toLocalDate() : order.getOrderDate())
                        .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "CASH")
                        .transactionReference(p.getNote())
                        .notes(p.getNote())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();

        String invNum = (config != null && config.getInvoicePrefix() != null ? config.getInvoicePrefix() : "SFF-") +
                (order.getOrderNumber() != null ? order.getOrderNumber() : order.getId());

        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : "Customer";
        String customerPhone = order.getCustomer() != null ? order.getCustomer().getPhone() : "";

        return InvoiceResponseDto.builder()
                .id(order.getId())
                .invoiceNumber(invNum)
                .invoiceDate(order.getOrderDate())
                .dueDate(order.getOrderDate() != null ? order.getOrderDate().plusDays(7) : LocalDate.now())
                .invoiceType("TAX_INVOICE")
                .classification("STITCHING_ONLY")
                .status("ISSUED")
                .isTaxInclusive(true)
                .placeOfSupply(config != null && config.getState() != null ? config.getState() : "Rajasthan")
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .subtotal(fin.getSubtotal())
                .discount(fin.getDiscount())
                .taxableAmount(fin.getTotalAmount())
                .cgstAmount(BigDecimal.ZERO)
                .sgstAmount(BigDecimal.ZERO)
                .igstAmount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .roundOff(BigDecimal.ZERO)
                .grandTotal(fin.getTotalAmount())
                .paidAmount(fin.getAmountPaid())
                .balanceAmount(fin.getBalanceDue())
                .paymentStatus(fin.isFullyPaid() ? "PAID" : (fin.getAmountPaid().compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "UNPAID"))
                .paymentMethod("CASH")
                .termsAndConditions(config != null ? config.getTermsAndConditions() : "Tailoring goods once made cannot be exchanged.")
                .isCancelled(false)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now())
                .updatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt() : LocalDateTime.now())
                .items(items)
                .payments(paymentDtos)
                .build();
    }

    private record OrderStatusSummary(
            String status,
            String statusLabel,
            String currentStage,
            int progressPercentage,
            boolean isDelivered
    ) {}
}
