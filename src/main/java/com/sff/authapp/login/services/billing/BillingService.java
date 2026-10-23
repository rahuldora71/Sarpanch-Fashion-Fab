package com.sff.authapp.login.services.billing;

import com.sff.authapp.login.dtos.billing.BillingDtos.*;
import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.helpers.GarmentClassificationHelper;
import com.sff.authapp.login.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final OrderRepository orderRepository;
    private final GarmentRepository garmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final InvoiceAuditLogRepository invoiceAuditLogRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final MonthEndClosingRepository monthEndClosingRepository;
    private final BillingConfigService billingConfigService;
    private final InvoiceSequenceService sequenceService;
    private final TaxCalculationService taxCalculationService;

    // ============================================================
    // 1. ELIGIBLE ORDERS FOR BILL GENERATION
    // ============================================================

    @Transactional(readOnly = true)
    public List<EligibleOrderDto> getEligibleOrders(
            String search,
            String customer,
            String orderNumber,
            String billingStatusFilter,
            String paymentStatusFilter,
            String classificationFilter,
            LocalDate startDate,
            LocalDate endDate
    ) {
        BillingConfig config = billingConfigService.getConfig();
        List<Order> allOrders = orderRepository.findAll();
        List<Long> billedOrderIds = invoiceOrderRepository.findBilledOrderIds();

        List<EligibleOrderDto> result = new ArrayList<>();

        for (Order order : allOrders) {
            // Date filter
            if (startDate != null && order.getOrderDate().isBefore(startDate)) continue;
            if (endDate != null && order.getOrderDate().isAfter(endDate)) continue;

            // Search filter
            String custName = order.getCustomer() != null ? order.getCustomer().getName() : "";
            String custPhone = order.getCustomer() != null ? order.getCustomer().getPhone() : "";
            String ordNo = order.getId().toString();
            String samplePage = order.getSampleBookPageNumber() != null ? order.getSampleBookPageNumber() : "";

            if (search != null && !search.trim().isEmpty()) {
                String q = search.toLowerCase();
                boolean match = ordNo.contains(q) || custName.toLowerCase().contains(q)
                        || custPhone.contains(q) || samplePage.toLowerCase().contains(q);
                if (!match) continue;
            }

            if (customer != null && !customer.trim().isEmpty()) {
                if (!custName.toLowerCase().contains(customer.toLowerCase()) && !custPhone.contains(customer)) {
                    continue;
                }
            }

            if (orderNumber != null && !orderNumber.trim().isEmpty()) {
                if (!ordNo.contains(orderNumber)) continue;
            }

            // Garments analysis via dedicated helper
            List<Garment> garments = order.getGarments() != null ? order.getGarments() : Collections.emptyList();
            int totalGarments = garments.size();
            List<String> serviceNames = new ArrayList<>();
            List<EligibleGarmentItemDto> garmentDtos = new ArrayList<>();

            for (Garment g : garments) {
                String sName = (g.getService() != null) ? g.getService().getName() : "Stitching";
                serviceNames.add(sName);

                GarmentClassificationHelper.GarmentBreakdown gb = GarmentClassificationHelper.analyzeGarment(g);

                garmentDtos.add(EligibleGarmentItemDto.builder()
                        .garmentId(g.getId())
                        .serviceName(sName)
                        .price(gb.getGarmentPrice())
                        .customerFabric(gb.isCustomerFabric())
                        .sampleNumber(g.getSampleNumber())
                        .addOnsCount(gb.getAddOnsCount())
                        .addOnsTotal(gb.getAddOnsTotal())
                        .serviceBasePrice(gb.getServiceBasePrice())
                        .stitchingAmount(gb.getStitchingAmount())
                        .fabricAmount(gb.getFabricAmount())
                        .hasShopFabric(gb.isHasShopFabric())
                        .build());
            }

            GarmentClassificationHelper.OrderBreakdown orderBreakdown = GarmentClassificationHelper.analyzeOrder(order);
            BigDecimal stitchingAmount = orderBreakdown.getTotalStitchingAmount();
            BigDecimal fabricAmount = orderBreakdown.getTotalFabricAmount();
            String classification = orderBreakdown.getClassification();
            String fabricSummary = orderBreakdown.getFabricSummary();

            BigDecimal discount = order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO;
            BigDecimal totalOrderAmount = stitchingAmount.add(fabricAmount).subtract(discount);
            if (totalOrderAmount.compareTo(BigDecimal.ZERO) < 0) totalOrderAmount = BigDecimal.ZERO;

            // Payments on order
            BigDecimal paidAmount = BigDecimal.ZERO;
            if (order.getPayments() != null) {
                for (Payment p : order.getPayments()) {
                    if (p.getAmount() != null) {
                        paidAmount = paidAmount.add(p.getAmount());
                    }
                }
            }
            BigDecimal balanceAmount = totalOrderAmount.subtract(paidAmount);
            if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) balanceAmount = BigDecimal.ZERO;

            String paymentStatus = "UNPAID";
            if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
                    paymentStatus = "PAID";
                } else {
                    paymentStatus = "PARTIAL";
                }
            }

            // Billing Status
            boolean isBilled = billedOrderIds.contains(order.getId());
            String billingStatus = isBilled ? "BILLED" : "UNBILLED";

            // Check config switches
            if (!config.isIncludeStitchingOnly() && "STITCHING_ONLY".equals(classification)) {
                continue;
            }
            if (!config.isAllowMixedOrders() && "MIXED".equals(classification)) {
                continue;
            }

            // Filter conditions
            if (billingStatusFilter != null && !billingStatusFilter.trim().isEmpty() && !billingStatusFilter.equalsIgnoreCase("ALL")) {
                if (!billingStatus.equalsIgnoreCase(billingStatusFilter)) continue;
            }
            if (paymentStatusFilter != null && !paymentStatusFilter.trim().isEmpty() && !paymentStatusFilter.equalsIgnoreCase("ALL")) {
                if (!paymentStatus.equalsIgnoreCase(paymentStatusFilter)) continue;
            }
            if (classificationFilter != null && !classificationFilter.trim().isEmpty() && !classificationFilter.equalsIgnoreCase("ALL")) {
                if (!classification.equalsIgnoreCase(classificationFilter)) continue;
            }

            result.add(EligibleOrderDto.builder()
                    .orderId(order.getId())
                    .orderNumber("ORD-" + order.getId())
                    .sampleBookPageNumber(order.getSampleBookPageNumber())
                    .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                    .customerName(custName)
                    .customerPhone(custPhone)
                    .customerAddress(order.getCustomer() != null ? order.getCustomer().getAddress() : "")
                    .customerGstin("")
                    .orderDate(order.getOrderDate())
                    .garmentsCount(totalGarments)
                    .servicesSummary(String.join(", ", serviceNames))
                    .fabricSummary(fabricSummary)
                    .stitchingAmount(stitchingAmount)
                    .fabricAmount(fabricAmount)
                    .totalOrderAmount(totalOrderAmount)
                    .paidAmount(paidAmount)
                    .balanceAmount(balanceAmount)
                    .classification(classification)
                    .billingStatus(billingStatus)
                    .paymentStatus(paymentStatus)
                    .garments(garmentDtos)
                    .build());
        }

        // Sort by order date desc
        result.sort((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()));
        return result;
    }

    // ============================================================
    // 2. GENERATE INVOICE FROM SELECTED ORDERS
    // ============================================================

    @Transactional
    public InvoiceResponseDto generateInvoiceFromOrders(GenerateInvoiceRequestDto req, String username) {
        if (req.getOrderIds() == null || req.getOrderIds().isEmpty()) {
            throw new IllegalArgumentException("At least one order must be selected to generate a bill.");
        }

        BillingConfig config = billingConfigService.getConfig();
        List<Order> orders = orderRepository.findAllById(req.getOrderIds());
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Selected orders could not be found.");
        }

        Order primaryOrder = orders.get(0);
        User customer = primaryOrder.getCustomer();

        // Customer details
        String customerName = customer != null ? customer.getName() : "Direct Customer";
        String customerPhone = customer != null ? customer.getPhone() : "";
        String customerAddress = customer != null ? customer.getAddress() : "";
        String customerGstin = req.getCustomerGstin() != null ? req.getCustomerGstin().trim() : "";
        String customerStateCode = req.getCustomerStateCode() != null ? req.getCustomerStateCode().trim() : config.getStateCode();

        boolean isInterState = !config.getStateCode().equalsIgnoreCase(customerStateCode);
        boolean isTaxInclusive = req.getIsTaxInclusive() != null ? req.getIsTaxInclusive() : config.isDefaultTaxInclusive();

        // Default tax rules
        TaxRule defaultStitchingTax = taxRuleRepository.findByCategoryAndIsDefaultTrue("STITCHING")
                .orElseGet(() -> taxRuleRepository.findByCategoryAndIsDefaultTrue("GENERAL")
                        .orElse(TaxRule.builder().gstRate(new BigDecimal("5.00")).hsnSacCode("9988").build()));

        TaxRule defaultFabricTax = taxRuleRepository.findByCategoryAndIsDefaultTrue("FABRIC")
                .orElse(TaxRule.builder().gstRate(new BigDecimal("5.00")).hsnSacCode("5208").build());

        // Build invoice items
        List<InvoiceItem> items = new ArrayList<>();
        int itemIndex = 1;

        // Persist and apply any garment fabric source overrides passed in request
        if (req.getGarmentCustomerFabricOverrides() != null && !req.getGarmentCustomerFabricOverrides().isEmpty()) {
            for (Map.Entry<Long, Boolean> entry : req.getGarmentCustomerFabricOverrides().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    garmentRepository.findById(entry.getKey()).ifPresent(g -> {
                        g.setCustomerFabric(entry.getValue());
                        garmentRepository.save(g);
                    });
                }
            }
        }

        boolean hasStitching = false;
        boolean hasFabric = false;
        Set<Long> allowedGarmentIds = req.getSelectedGarmentIds() != null && !req.getSelectedGarmentIds().isEmpty()
                ? new HashSet<>(req.getSelectedGarmentIds())
                : null;

        String billingMode = req.getBillingMode() != null ? req.getBillingMode().toUpperCase() : "FABRIC_CONVERTED";

        if (req.getOverrideItems() != null && !req.getOverrideItems().isEmpty()) {
            for (InvoiceItemDraftDto od : req.getOverrideItems()) {
                BigDecimal qty = od.getQuantity() != null ? od.getQuantity() : BigDecimal.ONE;
                BigDecimal rate = od.getRate() != null ? od.getRate() : BigDecimal.ZERO;
                BigDecimal disc = od.getDiscount() != null ? od.getDiscount() : BigDecimal.ZERO;
                BigDecimal rateGst = od.getGstRate() != null ? od.getGstRate() : defaultFabricTax.getGstRate();
                String hsn = od.getHsnSacCode() != null ? od.getHsnSacCode() :
                        ("FABRIC".equalsIgnoreCase(od.getItemType()) ? defaultFabricTax.getHsnSacCode() : defaultStitchingTax.getHsnSacCode());

                if ("FABRIC".equalsIgnoreCase(od.getItemType())) {
                    hasFabric = true;
                } else {
                    hasStitching = true;
                }

                TaxCalculationService.LineTaxResult lTax = taxCalculationService.calculateLineItem(
                        qty, rate, disc, rateGst, isTaxInclusive, isInterState
                );

                items.add(InvoiceItem.builder()
                        .itemOrder(itemIndex++)
                        .itemType(od.getItemType() != null ? od.getItemType().toUpperCase() : "FABRIC")
                        .description(od.getDescription())
                        .hsnSacCode(hsn)
                        .quantity(qty)
                        .unit(od.getUnit() != null ? od.getUnit() : "Mtr")
                        .rate(rate)
                        .grossAmount(lTax.getGrossAmount())
                        .discount(disc)
                        .taxableAmount(lTax.getTaxableAmount())
                        .gstRate(lTax.getGstRate())
                        .cgstRate(lTax.getCgstRate())
                        .sgstRate(lTax.getSgstRate())
                        .igstRate(lTax.getIgstRate())
                        .cgstAmount(lTax.getCgstAmount())
                        .sgstAmount(lTax.getSgstAmount())
                        .igstAmount(lTax.getIgstAmount())
                        .totalTax(lTax.getTotalTax())
                        .totalAmount(lTax.getTotalAmount())
                        .garmentId(od.getGarmentId())
                        .orderId(od.getOrderId())
                        .fabricId(od.getFabricId())
                        .build());
            }
        } else if ("FABRIC_CONVERTED".equals(billingMode)) {
            hasFabric = true;
            // Group garments by service category or name (e.g. "SHIRT", "PANT", "KURTA", "COAT")
            Map<String, List<Garment>> serviceGroups = new LinkedHashMap<>();
            for (Order order : orders) {
                List<Garment> garments = order.getGarments() != null ? order.getGarments() : Collections.emptyList();
                for (Garment g : garments) {
                    if (allowedGarmentIds != null && !allowedGarmentIds.contains(g.getId())) continue;
                    String sName = (g.getService() != null && g.getService().getName() != null)
                            ? g.getService().getName() : "Apparel";
                    serviceGroups.computeIfAbsent(sName, k -> new ArrayList<>()).add(g);
                }
            }

            for (Map.Entry<String, List<Garment>> entry : serviceGroups.entrySet()) {
                String sName = entry.getKey();
                List<Garment> groupGarments = entry.getValue();
                int count = groupGarments.size();

                BigDecimal groupTotal = BigDecimal.ZERO;
                for (Garment g : groupGarments) {
                    BigDecimal gPrice = g.getPrice() != null ? g.getPrice() : BigDecimal.ZERO;
                    BigDecimal addOnSum = BigDecimal.ZERO;
                    if (g.getAddOns() != null) {
                        for (GarmentAddOn addOn : g.getAddOns()) {
                            if (addOn.getAddOn() != null && addOn.getAddOn().getPrice() != null) {
                                addOnSum = addOnSum.add(addOn.getAddOn().getPrice());
                            }
                        }
                    }
                    groupTotal = groupTotal.add(gPrice).add(addOnSum);
                }

                if (groupTotal.compareTo(BigDecimal.ZERO) <= 0) continue;

                // Determine standard consumption per piece & description & HSN
                String sUpper = sName.toUpperCase();
                BigDecimal consumptionPerPiece;
                String fabricName;
                String hsnCode = defaultFabricTax.getHsnSacCode();

                if (sUpper.contains("SHIRT")) {
                    consumptionPerPiece = new BigDecimal("1.60");
                    fabricName = "Siyaram Shirting Fabric";
                    hsnCode = "5208";
                } else if (sUpper.contains("PANT") || sUpper.contains("TROUSER")) {
                    consumptionPerPiece = new BigDecimal("1.25");
                    fabricName = "Raymond Suiting Fabric";
                    hsnCode = "5407";
                } else if (sUpper.contains("KURTA") || sUpper.contains("PAJAMA")) {
                    consumptionPerPiece = new BigDecimal("2.25");
                    fabricName = "Fine Linen Kurta Fabric";
                    hsnCode = "5210";
                } else if (sUpper.contains("COAT") || sUpper.contains("BLAZER") || sUpper.contains("SUIT") || sUpper.contains("SAFARI")) {
                    consumptionPerPiece = new BigDecimal("2.00");
                    fabricName = "Raymond Premium Suiting";
                    hsnCode = "5407";
                } else {
                    consumptionPerPiece = new BigDecimal("1.50");
                    fabricName = "Premium Shirting / Suiting Fabric";
                    hsnCode = defaultFabricTax.getHsnSacCode();
                }

                BigDecimal totalMeters = consumptionPerPiece.multiply(new BigDecimal(count)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal gstRate = defaultFabricTax.getGstRate();
                BigDecimal divisor = BigDecimal.ONE.add(gstRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
                BigDecimal taxableAmount = groupTotal.divide(divisor, 2, RoundingMode.HALF_UP);
                BigDecimal totalTax = groupTotal.subtract(taxableAmount);
                BigDecimal ratePerMeter = taxableAmount.divide(totalMeters, 3, RoundingMode.HALF_UP);

                BigDecimal halfTax = totalTax.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                BigDecimal otherHalfTax = totalTax.subtract(halfTax);

                String label = fabricName + (count > 1 ? " [" + count + " " + sName + "s]" : " [" + sName + "]");

                items.add(InvoiceItem.builder()
                        .itemOrder(itemIndex++)
                        .itemType("FABRIC")
                        .description(label)
                        .hsnSacCode(hsnCode)
                        .quantity(totalMeters)
                        .unit("Mtr")
                        .rate(ratePerMeter)
                        .grossAmount(groupTotal)
                        .discount(BigDecimal.ZERO)
                        .taxableAmount(taxableAmount)
                        .gstRate(gstRate)
                        .cgstRate(isInterState ? BigDecimal.ZERO : gstRate.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP))
                        .sgstRate(isInterState ? BigDecimal.ZERO : gstRate.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP))
                        .igstRate(isInterState ? gstRate : BigDecimal.ZERO)
                        .cgstAmount(isInterState ? BigDecimal.ZERO : halfTax)
                        .sgstAmount(isInterState ? BigDecimal.ZERO : otherHalfTax)
                        .igstAmount(isInterState ? totalTax : BigDecimal.ZERO)
                        .totalTax(totalTax)
                        .totalAmount(groupTotal)
                        .orderId(orders.get(0).getId())
                        .build());
            }

            // Include additional charges
            for (Order order : orders) {
                if (order.getAdditionalCharges() != null) {
                    for (AdditionalCharge ac : order.getAdditionalCharges()) {
                        BigDecimal acAmount = ac.getAmount() != null ? ac.getAmount() : BigDecimal.ZERO;
                        if (acAmount.compareTo(BigDecimal.ZERO) <= 0) continue;
                        BigDecimal divisor = BigDecimal.ONE.add(defaultFabricTax.getGstRate().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
                        BigDecimal taxable = acAmount.divide(divisor, 2, RoundingMode.HALF_UP);
                        BigDecimal tax = acAmount.subtract(taxable);
                        BigDecimal halfTax = tax.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);

                        items.add(InvoiceItem.builder()
                                .itemOrder(itemIndex++)
                                .itemType("CUSTOM")
                                .description(ac.getDescription() != null ? ac.getDescription() : "Additional Accessory")
                                .hsnSacCode(defaultFabricTax.getHsnSacCode())
                                .quantity(BigDecimal.ONE)
                                .unit("Pcs")
                                .rate(taxable)
                                .grossAmount(acAmount)
                                .discount(BigDecimal.ZERO)
                                .taxableAmount(taxable)
                                .gstRate(defaultFabricTax.getGstRate())
                                .cgstRate(isInterState ? BigDecimal.ZERO : defaultFabricTax.getGstRate().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP))
                                .sgstRate(isInterState ? BigDecimal.ZERO : defaultFabricTax.getGstRate().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP))
                                .igstRate(isInterState ? defaultFabricTax.getGstRate() : BigDecimal.ZERO)
                                .cgstAmount(isInterState ? BigDecimal.ZERO : halfTax)
                                .sgstAmount(isInterState ? BigDecimal.ZERO : tax.subtract(halfTax))
                                .igstAmount(isInterState ? tax : BigDecimal.ZERO)
                                .totalTax(tax)
                                .totalAmount(acAmount)
                                .orderId(order.getId())
                                .build());
                    }
                }
            }
        } else if ("GARMENT_CONSOLIDATED".equals(billingMode)) {
            hasStitching = true;
            for (Order order : orders) {
                List<Garment> garments = order.getGarments() != null ? order.getGarments() : Collections.emptyList();
                for (Garment g : garments) {
                    if (allowedGarmentIds != null && !allowedGarmentIds.contains(g.getId())) continue;
                    String sName = g.getService() != null ? g.getService().getName() : "Custom Stitching";
                    BigDecimal gPrice = g.getPrice() != null ? g.getPrice() : BigDecimal.ZERO;
                    BigDecimal addOnsSum = BigDecimal.ZERO;
                    List<String> addOnNames = new ArrayList<>();
                    if (g.getAddOns() != null) {
                        for (GarmentAddOn addOn : g.getAddOns()) {
                            if (addOn.getAddOn() != null && addOn.getAddOn().getPrice() != null) {
                                addOnsSum = addOnsSum.add(addOn.getAddOn().getPrice());
                                addOnNames.add(addOn.getAddOn().getName());
                            }
                        }
                    }

                    BigDecimal garmentTotal = gPrice.add(addOnsSum);
                    String desc = sName + (g.getSampleNumber() != null ? " [Sample: " + g.getSampleNumber() + "]" : "");
                    if (!addOnNames.isEmpty()) {
                        desc += " (incl. " + String.join(", ", addOnNames) + ")";
                    }

                    TaxCalculationService.LineTaxResult stitchTax = taxCalculationService.calculateLineItem(
                            BigDecimal.ONE,
                            garmentTotal,
                            BigDecimal.ZERO,
                            defaultStitchingTax.getGstRate(),
                            isTaxInclusive,
                            isInterState
                    );

                    items.add(InvoiceItem.builder()
                            .itemOrder(itemIndex++)
                            .itemType("STITCHING")
                            .description(desc)
                            .hsnSacCode(defaultStitchingTax.getHsnSacCode())
                            .quantity(BigDecimal.ONE)
                            .unit("Pcs")
                            .rate(garmentTotal)
                            .grossAmount(stitchTax.getGrossAmount())
                            .discount(BigDecimal.ZERO)
                            .taxableAmount(stitchTax.getTaxableAmount())
                            .gstRate(stitchTax.getGstRate())
                            .cgstRate(stitchTax.getCgstRate())
                            .sgstRate(stitchTax.getSgstRate())
                            .igstRate(stitchTax.getIgstRate())
                            .cgstAmount(stitchTax.getCgstAmount())
                            .sgstAmount(stitchTax.getSgstAmount())
                            .igstAmount(stitchTax.getIgstAmount())
                            .totalTax(stitchTax.getTotalTax())
                            .totalAmount(stitchTax.getTotalAmount())
                            .garmentId(g.getId())
                            .orderId(order.getId())
                            .build());
                }

                if (order.getAdditionalCharges() != null) {
                    for (AdditionalCharge ac : order.getAdditionalCharges()) {
                        BigDecimal acAmount = ac.getAmount() != null ? ac.getAmount() : BigDecimal.ZERO;
                        TaxCalculationService.LineTaxResult acTax = taxCalculationService.calculateLineItem(
                                BigDecimal.ONE, acAmount, BigDecimal.ZERO, defaultStitchingTax.getGstRate(), isTaxInclusive, isInterState
                        );
                        items.add(InvoiceItem.builder()
                                .itemOrder(itemIndex++)
                                .itemType("CUSTOM")
                                .description(ac.getDescription() != null ? ac.getDescription() : "Additional Charge")
                                .hsnSacCode(defaultStitchingTax.getHsnSacCode())
                                .quantity(BigDecimal.ONE)
                                .unit("Job")
                                .rate(acAmount)
                                .grossAmount(acTax.getGrossAmount())
                                .discount(BigDecimal.ZERO)
                                .taxableAmount(acTax.getTaxableAmount())
                                .gstRate(acTax.getGstRate())
                                .cgstRate(acTax.getCgstRate())
                                .sgstRate(acTax.getSgstRate())
                                .igstRate(acTax.getIgstRate())
                                .cgstAmount(acTax.getCgstAmount())
                                .sgstAmount(acTax.getSgstAmount())
                                .igstAmount(acTax.getIgstAmount())
                                .totalTax(acTax.getTotalTax())
                                .totalAmount(acTax.getTotalAmount())
                                .orderId(order.getId())
                                .build());
                    }
                }
            }
        } else {
            // DETAILED_ITEMIZED
            for (Order order : orders) {
                List<Garment> garments = order.getGarments() != null ? order.getGarments() : Collections.emptyList();

                for (Garment g : garments) {
                    if (allowedGarmentIds != null && !allowedGarmentIds.contains(g.getId())) {
                        continue; // skipped by partial billing
                    }

                    String serviceName = g.getService() != null ? g.getService().getName() : "Custom Stitching";
                    GarmentClassificationHelper.GarmentBreakdown gb = GarmentClassificationHelper.analyzeGarment(g);

                    // If garment includes shop fabric, bill fabric as FABRIC item (5% GST) and base tailoring as STITCHING item
                    if (gb.isHasShopFabric() && gb.getFabricAmount().compareTo(BigDecimal.ZERO) > 0) {
                        hasFabric = true;
                        TaxCalculationService.LineTaxResult fabricTax = taxCalculationService.calculateLineItem(
                                BigDecimal.ONE,
                                gb.getFabricAmount(),
                                BigDecimal.ZERO,
                                defaultFabricTax.getGstRate(),
                                isTaxInclusive,
                                isInterState
                        );

                        items.add(InvoiceItem.builder()
                                .itemOrder(itemIndex++)
                                .itemType("FABRIC")
                                .description("Fabric for " + serviceName + (g.getSampleNumber() != null ? " [Sample: " + g.getSampleNumber() + "]" : ""))
                                .hsnSacCode(defaultFabricTax.getHsnSacCode())
                                .quantity(BigDecimal.ONE)
                                .unit("Pcs")
                                .rate(gb.getFabricAmount())
                                .grossAmount(fabricTax.getGrossAmount())
                                .discount(BigDecimal.ZERO)
                                .taxableAmount(fabricTax.getTaxableAmount())
                                .gstRate(fabricTax.getGstRate())
                                .cgstRate(fabricTax.getCgstRate())
                                .sgstRate(fabricTax.getSgstRate())
                                .igstRate(fabricTax.getIgstRate())
                                .cgstAmount(fabricTax.getCgstAmount())
                                .sgstAmount(fabricTax.getSgstAmount())
                                .igstAmount(fabricTax.getIgstAmount())
                                .totalTax(fabricTax.getTotalTax())
                                .totalAmount(fabricTax.getTotalAmount())
                                .garmentId(g.getId())
                                .orderId(order.getId())
                                .build());

                        BigDecimal stitchPrice = gb.getServiceBasePrice().compareTo(BigDecimal.ZERO) > 0
                                ? gb.getServiceBasePrice()
                                : gb.getStitchingAmount().subtract(gb.getAddOnsTotal());

                        hasStitching = true;
                        TaxCalculationService.LineTaxResult stitchTax = taxCalculationService.calculateLineItem(
                                BigDecimal.ONE,
                                stitchPrice,
                                BigDecimal.ZERO,
                                defaultStitchingTax.getGstRate(),
                                isTaxInclusive,
                                isInterState
                        );

                        items.add(InvoiceItem.builder()
                                .itemOrder(itemIndex++)
                                .itemType("STITCHING")
                                .description(serviceName + " Stitching" + (g.getSampleNumber() != null ? " [Sample: " + g.getSampleNumber() + "]" : ""))
                                .hsnSacCode(defaultStitchingTax.getHsnSacCode())
                                .quantity(BigDecimal.ONE)
                                .unit("Job")
                                .rate(stitchPrice)
                                .grossAmount(stitchTax.getGrossAmount())
                                .discount(BigDecimal.ZERO)
                                .taxableAmount(stitchTax.getTaxableAmount())
                                .gstRate(stitchTax.getGstRate())
                                .cgstRate(stitchTax.getCgstRate())
                                .sgstRate(stitchTax.getSgstRate())
                                .igstRate(stitchTax.getIgstRate())
                                .cgstAmount(stitchTax.getCgstAmount())
                                .sgstAmount(stitchTax.getSgstAmount())
                                .igstAmount(stitchTax.getIgstAmount())
                                .totalTax(stitchTax.getTotalTax())
                                .totalAmount(stitchTax.getTotalAmount())
                                .garmentId(g.getId())
                                .orderId(order.getId())
                                .build());
                    } else {
                        BigDecimal stitchPrice = gb.getStitchingAmount().subtract(gb.getAddOnsTotal());
                        hasStitching = true;
                        TaxCalculationService.LineTaxResult stitchTax = taxCalculationService.calculateLineItem(
                                BigDecimal.ONE,
                                stitchPrice,
                                BigDecimal.ZERO,
                                defaultStitchingTax.getGstRate(),
                                isTaxInclusive,
                                isInterState
                        );

                        items.add(InvoiceItem.builder()
                                .itemOrder(itemIndex++)
                                .itemType("STITCHING")
                                .description(serviceName + (g.getSampleNumber() != null ? " [Sample: " + g.getSampleNumber() + "]" : ""))
                                .hsnSacCode(defaultStitchingTax.getHsnSacCode())
                                .quantity(BigDecimal.ONE)
                                .unit("Pcs")
                                .rate(stitchPrice)
                                .grossAmount(stitchTax.getGrossAmount())
                                .discount(BigDecimal.ZERO)
                                .taxableAmount(stitchTax.getTaxableAmount())
                                .gstRate(stitchTax.getGstRate())
                                .cgstRate(stitchTax.getCgstRate())
                                .sgstRate(stitchTax.getSgstRate())
                                .igstRate(stitchTax.getIgstRate())
                                .cgstAmount(stitchTax.getCgstAmount())
                                .sgstAmount(stitchTax.getSgstAmount())
                                .igstAmount(stitchTax.getIgstAmount())
                                .totalTax(stitchTax.getTotalTax())
                                .totalAmount(stitchTax.getTotalAmount())
                                .garmentId(g.getId())
                                .orderId(order.getId())
                                .build());
                    }

                    if (g.getAddOns() != null) {
                        for (GarmentAddOn addOn : g.getAddOns()) {
                            if (addOn.getAddOn() != null) {
                                BigDecimal addOnPrice = addOn.getAddOn().getPrice() != null ? addOn.getAddOn().getPrice() : BigDecimal.ZERO;
                                TaxCalculationService.LineTaxResult addOnTax = taxCalculationService.calculateLineItem(
                                        BigDecimal.ONE,
                                        addOnPrice,
                                        BigDecimal.ZERO,
                                        defaultStitchingTax.getGstRate(),
                                        isTaxInclusive,
                                        isInterState
                                );

                                items.add(InvoiceItem.builder()
                                        .itemOrder(itemIndex++)
                                        .itemType("ADD_ON")
                                        .description("Add-on: " + addOn.getAddOn().getName() + " (" + serviceName + ")")
                                        .hsnSacCode(defaultStitchingTax.getHsnSacCode())
                                        .quantity(BigDecimal.ONE)
                                        .unit("Job")
                                        .rate(addOnPrice)
                                        .grossAmount(addOnTax.getGrossAmount())
                                        .discount(BigDecimal.ZERO)
                                        .taxableAmount(addOnTax.getTaxableAmount())
                                        .gstRate(addOnTax.getGstRate())
                                        .cgstRate(addOnTax.getCgstRate())
                                        .sgstRate(addOnTax.getSgstRate())
                                        .igstRate(addOnTax.getIgstRate())
                                        .cgstAmount(addOnTax.getCgstAmount())
                                        .sgstAmount(addOnTax.getSgstAmount())
                                        .igstAmount(addOnTax.getIgstAmount())
                                        .totalTax(addOnTax.getTotalTax())
                                        .totalAmount(addOnTax.getTotalAmount())
                                        .garmentId(g.getId())
                                        .orderId(order.getId())
                                        .build());
                            }
                        }
                    }
                }

                if (order.getAdditionalCharges() != null) {
                    for (AdditionalCharge ac : order.getAdditionalCharges()) {
                        BigDecimal acAmount = ac.getAmount() != null ? ac.getAmount() : BigDecimal.ZERO;
                        TaxCalculationService.LineTaxResult acTax = taxCalculationService.calculateLineItem(
                                BigDecimal.ONE,
                                acAmount,
                                BigDecimal.ZERO,
                                defaultStitchingTax.getGstRate(),
                                isTaxInclusive,
                                isInterState
                        );

                        items.add(InvoiceItem.builder()
                                .itemOrder(itemIndex++)
                                .itemType("CUSTOM")
                                .description(ac.getDescription() != null ? ac.getDescription() : "Additional Charge")
                                .hsnSacCode(defaultStitchingTax.getHsnSacCode())
                                .quantity(BigDecimal.ONE)
                                .unit("Job")
                                .rate(acAmount)
                                .grossAmount(acTax.getGrossAmount())
                                .discount(BigDecimal.ZERO)
                                .taxableAmount(acTax.getTaxableAmount())
                                .gstRate(acTax.getGstRate())
                                .cgstRate(acTax.getCgstRate())
                                .sgstRate(acTax.getSgstRate())
                                .igstRate(acTax.getIgstRate())
                                .cgstAmount(acTax.getCgstAmount())
                                .sgstAmount(acTax.getSgstAmount())
                                .igstAmount(acTax.getIgstAmount())
                                .totalTax(acTax.getTotalTax())
                                .totalAmount(acTax.getTotalAmount())
                                .orderId(order.getId())
                                .build());
                    }
                }
            }
        }

        // Custom items from request
        if (req.getCustomItems() != null) {
            for (InvoiceItemDraftDto cd : req.getCustomItems()) {
                BigDecimal qty = cd.getQuantity() != null ? cd.getQuantity() : BigDecimal.ONE;
                BigDecimal rate = cd.getRate() != null ? cd.getRate() : BigDecimal.ZERO;
                BigDecimal disc = cd.getDiscount() != null ? cd.getDiscount() : BigDecimal.ZERO;
                BigDecimal rateGst = cd.getGstRate() != null ? cd.getGstRate() : defaultStitchingTax.getGstRate();
                String hsn = cd.getHsnSacCode() != null ? cd.getHsnSacCode() :
                        ("FABRIC".equalsIgnoreCase(cd.getItemType()) ? defaultFabricTax.getHsnSacCode() : defaultStitchingTax.getHsnSacCode());

                if ("FABRIC".equalsIgnoreCase(cd.getItemType())) {
                    hasFabric = true;
                } else {
                    hasStitching = true;
                }

                TaxCalculationService.LineTaxResult lTax = taxCalculationService.calculateLineItem(
                        qty, rate, disc, rateGst, isTaxInclusive, isInterState
                );

                items.add(InvoiceItem.builder()
                        .itemOrder(itemIndex++)
                        .itemType(cd.getItemType() != null ? cd.getItemType().toUpperCase() : "CUSTOM")
                        .description(cd.getDescription())
                        .hsnSacCode(hsn)
                        .quantity(qty)
                        .unit(cd.getUnit() != null ? cd.getUnit() : "Pcs")
                        .rate(rate)
                        .grossAmount(lTax.getGrossAmount())
                        .discount(disc)
                        .taxableAmount(lTax.getTaxableAmount())
                        .gstRate(lTax.getGstRate())
                        .cgstRate(lTax.getCgstRate())
                        .sgstRate(lTax.getSgstRate())
                        .igstRate(lTax.getIgstRate())
                        .cgstAmount(lTax.getCgstAmount())
                        .sgstAmount(lTax.getSgstAmount())
                        .igstAmount(lTax.getIgstAmount())
                        .totalTax(lTax.getTotalTax())
                        .totalAmount(lTax.getTotalAmount())
                        .garmentId(cd.getGarmentId())
                        .orderId(cd.getOrderId())
                        .fabricId(cd.getFabricId())
                        .build());
            }
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("No billable items found in selected orders.");
        }

        // Classification
        String classification;
        if (hasFabric && hasStitching) {
            classification = "MIXED";
        } else if (hasFabric) {
            classification = "FABRIC_ONLY";
        } else {
            classification = "STITCHING_ONLY";
        }

        // Aggregate Totals
        BigDecimal subtotal = items.stream().map(InvoiceItem::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = req.getDiscount() != null ? req.getDiscount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = items.stream().map(InvoiceItem::getTaxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cgstAmount = items.stream().map(InvoiceItem::getCgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sgstAmount = items.stream().map(InvoiceItem::getSgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igstAmount = items.stream().map(InvoiceItem::getIgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = items.stream().map(InvoiceItem::getTotalTax).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal roundOff = req.getRoundOff() != null ? req.getRoundOff() : BigDecimal.ZERO;
        BigDecimal grandTotal = items.stream().map(InvoiceItem::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add).add(roundOff);

        // Sequence number
        String invoiceNumber = sequenceService.generateNextInvoiceNumber();

        // Advance / paid amount
        BigDecimal paidAmount = req.getAdvancePaidAmount() != null ? req.getAdvancePaidAmount() : BigDecimal.ZERO;
        BigDecimal balanceAmount = grandTotal.subtract(paidAmount);
        if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) balanceAmount = BigDecimal.ZERO;

        String paymentStatus = "UNPAID";
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            paymentStatus = balanceAmount.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIALLY_PAID";
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .invoiceDate(req.getInvoiceDate() != null ? req.getInvoiceDate() : LocalDate.now())
                .dueDate(req.getDueDate() != null ? req.getDueDate() : LocalDate.now().plusDays(15))
                .invoiceType(req.getInvoiceType() != null ? req.getInvoiceType() : "TAX_INVOICE")
                .classification(classification)
                .status("GENERATED")
                .isTaxInclusive(isTaxInclusive)
                .placeOfSupply(req.getPlaceOfSupply() != null ? req.getPlaceOfSupply() : config.getState())
                .isInterState(isInterState)
                .customer(customer)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .customerAddress(customerAddress)
                .customerGstin(customerGstin)
                .customerStateCode(customerStateCode)
                .subtotal(subtotal)
                .discount(discount)
                .taxableAmount(taxableAmount)
                .cgstAmount(cgstAmount)
                .sgstAmount(sgstAmount)
                .igstAmount(igstAmount)
                .totalTax(totalTax)
                .roundOff(roundOff)
                .grandTotal(grandTotal)
                .paidAmount(paidAmount)
                .balanceAmount(balanceAmount)
                .paymentStatus(paymentStatus)
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "CASH")
                .notes(req.getNotes())
                .termsAndConditions(req.getTermsAndConditions() != null ? req.getTermsAndConditions() : config.getTermsAndConditions())
                .isCancelled(false)
                .build();

        // Attach items
        for (InvoiceItem it : items) {
            it.setInvoice(invoice);
        }
        invoice.setItems(items);

        // Attach invoice orders link
        List<InvoiceOrder> invOrders = new ArrayList<>();
        boolean isPartial = allowedGarmentIds != null;
        for (Order o : orders) {
            invOrders.add(InvoiceOrder.builder()
                    .invoice(invoice)
                    .order(o)
                    .billedAmount(grandTotal)
                    .isPartial(isPartial)
                    .build());
        }
        invoice.setInvoiceOrders(invOrders);

        // Attach payment if initial paid > 0
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            List<InvoicePayment> payments = new ArrayList<>();
            payments.add(InvoicePayment.builder()
                    .invoice(invoice)
                    .amount(paidAmount)
                    .paymentDate(invoice.getInvoiceDate())
                    .paymentMethod(invoice.getPaymentMethod())
                    .notes("Initial settlement at bill generation")
                    .receivedBy(username)
                    .build());
            invoice.setPayments(payments);
        }

        // Attach audit log
        List<InvoiceAuditLog> auditLogs = new ArrayList<>();
        auditLogs.add(InvoiceAuditLog.builder()
                .invoice(invoice)
                .action("GENERATED")
                .details(String.format("Generated invoice %s from %d order(s). Total: ₹%s", invoiceNumber, orders.size(), grandTotal.toPlainString()))
                .performedBy(username != null ? username : "STAFF")
                .timestamp(LocalDateTime.now())
                .build());
        invoice.setAuditLogs(auditLogs);

        invoice = invoiceRepository.save(invoice);
        log.info("Successfully generated invoice: {}", invoice.getInvoiceNumber());

        return mapToInvoiceResponseDto(invoice);
    }

    // ============================================================
    // 3. CREATE MANUAL / CUSTOM INVOICE
    // ============================================================

    @Transactional
    public InvoiceResponseDto createManualInvoice(CreateManualInvoiceRequestDto req, String username) {
        BillingConfig config = billingConfigService.getConfig();

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Invoice must have at least one line item.");
        }

        String customerName = req.getCustomerName() != null ? req.getCustomerName().trim() : "Walk-in Customer";
        String customerPhone = req.getCustomerPhone() != null ? req.getCustomerPhone().trim() : "";
        String customerAddress = req.getCustomerAddress() != null ? req.getCustomerAddress().trim() : "";
        String customerGstin = req.getCustomerGstin() != null ? req.getCustomerGstin().trim() : "";
        String customerStateCode = req.getCustomerStateCode() != null ? req.getCustomerStateCode().trim() : config.getStateCode();

        boolean isInterState = !config.getStateCode().equalsIgnoreCase(customerStateCode);
        boolean isTaxInclusive = req.getIsTaxInclusive() != null ? req.getIsTaxInclusive() : config.isDefaultTaxInclusive();

        TaxRule defaultGeneralTax = taxRuleRepository.findByCategoryAndIsDefaultTrue("GENERAL")
                .orElse(TaxRule.builder().gstRate(new BigDecimal("5.00")).hsnSacCode("9988").build());

        List<InvoiceItem> items = new ArrayList<>();
        int itemIndex = 1;
        boolean hasFabric = false;
        boolean hasStitching = false;

        for (InvoiceItemDraftDto draft : req.getItems()) {
            BigDecimal qty = draft.getQuantity() != null ? draft.getQuantity() : BigDecimal.ONE;
            BigDecimal rate = draft.getRate() != null ? draft.getRate() : BigDecimal.ZERO;
            BigDecimal disc = draft.getDiscount() != null ? draft.getDiscount() : BigDecimal.ZERO;
            BigDecimal gstRate = draft.getGstRate() != null ? draft.getGstRate() : defaultGeneralTax.getGstRate();
            String hsn = draft.getHsnSacCode() != null ? draft.getHsnSacCode() : defaultGeneralTax.getHsnSacCode();

            String itType = draft.getItemType() != null ? draft.getItemType().toUpperCase() : "CUSTOM";
            if ("FABRIC".equals(itType)) hasFabric = true;
            else hasStitching = true;

            TaxCalculationService.LineTaxResult lineTax = taxCalculationService.calculateLineItem(
                    qty, rate, disc, gstRate, isTaxInclusive, isInterState
            );

            items.add(InvoiceItem.builder()
                    .itemOrder(itemIndex++)
                    .itemType(itType)
                    .description(draft.getDescription())
                    .hsnSacCode(hsn)
                    .quantity(qty)
                    .unit(draft.getUnit() != null ? draft.getUnit() : "Pcs")
                    .rate(rate)
                    .grossAmount(lineTax.getGrossAmount())
                    .discount(disc)
                    .taxableAmount(lineTax.getTaxableAmount())
                    .gstRate(lineTax.getGstRate())
                    .cgstRate(lineTax.getCgstRate())
                    .sgstRate(lineTax.getSgstRate())
                    .igstRate(lineTax.getIgstRate())
                    .cgstAmount(lineTax.getCgstAmount())
                    .sgstAmount(lineTax.getSgstAmount())
                    .igstAmount(lineTax.getIgstAmount())
                    .totalTax(lineTax.getTotalTax())
                    .totalAmount(lineTax.getTotalAmount())
                    .build());
        }

        String classification = req.getClassification();
        if (classification == null || classification.trim().isEmpty()) {
            if (hasFabric && hasStitching) classification = "MIXED";
            else if (hasFabric) classification = "FABRIC_ONLY";
            else classification = "STITCHING_ONLY";
        }

        BigDecimal subtotal = items.stream().map(InvoiceItem::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = req.getDiscount() != null ? req.getDiscount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = items.stream().map(InvoiceItem::getTaxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cgstAmount = items.stream().map(InvoiceItem::getCgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sgstAmount = items.stream().map(InvoiceItem::getSgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igstAmount = items.stream().map(InvoiceItem::getIgstAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = items.stream().map(InvoiceItem::getTotalTax).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal roundOff = req.getRoundOff() != null ? req.getRoundOff() : BigDecimal.ZERO;
        BigDecimal grandTotal = items.stream().map(InvoiceItem::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add).add(roundOff);

        String invoiceNumber = sequenceService.generateNextInvoiceNumber();

        BigDecimal paidAmount = req.getPaidAmount() != null ? req.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balanceAmount = grandTotal.subtract(paidAmount);
        if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) balanceAmount = BigDecimal.ZERO;

        String paymentStatus = "UNPAID";
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            paymentStatus = balanceAmount.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIALLY_PAID";
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .invoiceDate(req.getInvoiceDate() != null ? req.getInvoiceDate() : LocalDate.now())
                .dueDate(req.getDueDate() != null ? req.getDueDate() : LocalDate.now().plusDays(15))
                .invoiceType(req.getInvoiceType() != null ? req.getInvoiceType() : "TAX_INVOICE")
                .classification(classification)
                .status("GENERATED")
                .isTaxInclusive(isTaxInclusive)
                .placeOfSupply(req.getPlaceOfSupply() != null ? req.getPlaceOfSupply() : config.getState())
                .isInterState(isInterState)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .customerAddress(customerAddress)
                .customerGstin(customerGstin)
                .customerStateCode(customerStateCode)
                .subtotal(subtotal)
                .discount(discount)
                .taxableAmount(taxableAmount)
                .cgstAmount(cgstAmount)
                .sgstAmount(sgstAmount)
                .igstAmount(igstAmount)
                .totalTax(totalTax)
                .roundOff(roundOff)
                .grandTotal(grandTotal)
                .paidAmount(paidAmount)
                .balanceAmount(balanceAmount)
                .paymentStatus(paymentStatus)
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "CASH")
                .notes(req.getNotes())
                .termsAndConditions(req.getTermsAndConditions() != null ? req.getTermsAndConditions() : config.getTermsAndConditions())
                .isCancelled(false)
                .build();

        for (InvoiceItem it : items) {
            it.setInvoice(invoice);
        }
        invoice.setItems(items);

        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            List<InvoicePayment> payments = new ArrayList<>();
            payments.add(InvoicePayment.builder()
                    .invoice(invoice)
                    .amount(paidAmount)
                    .paymentDate(invoice.getInvoiceDate())
                    .paymentMethod(invoice.getPaymentMethod())
                    .notes("Settled at invoice creation")
                    .receivedBy(username)
                    .build());
            invoice.setPayments(payments);
        }

        List<InvoiceAuditLog> auditLogs = new ArrayList<>();
        auditLogs.add(InvoiceAuditLog.builder()
                .invoice(invoice)
                .action("GENERATED")
                .details(String.format("Created manual invoice %s. Total: ₹%s", invoiceNumber, grandTotal.toPlainString()))
                .performedBy(username != null ? username : "STAFF")
                .timestamp(LocalDateTime.now())
                .build());
        invoice.setAuditLogs(auditLogs);

        invoice = invoiceRepository.save(invoice);
        return mapToInvoiceResponseDto(invoice);
    }

    // ============================================================
    // 4. INVOICE REGISTER & LOOKUP
    // ============================================================

    @Transactional(readOnly = true)
    public Page<InvoiceResponseDto> getInvoices(
            String search,
            String status,
            String classification,
            String paymentStatus,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isCancelled,
            Pageable pageable
    ) {
        return invoiceRepository.findFilteredInvoices(search, status, classification, paymentStatus, startDate, endDate, isCancelled, pageable)
                .map(this::mapToInvoiceResponseDto);
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));
        return mapToInvoiceResponseDto(invoice);
    }

    // ============================================================
    // 5. PAYMENT RECORDING
    // ============================================================

    @Transactional
    public InvoiceResponseDto recordPayment(Long invoiceId, RecordPaymentRequestDto req, String username) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

        if (invoice.isCancelled()) {
            throw new IllegalStateException("Cannot record payment against cancelled invoice.");
        }

        BigDecimal payAmount = req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO;
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        BigDecimal newPaid = invoice.getPaidAmount().add(payAmount);
        BigDecimal newBalance = invoice.getGrandTotal().subtract(newPaid);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }

        invoice.setPaidAmount(newPaid);
        invoice.setBalanceAmount(newBalance);
        invoice.setPaymentStatus(newBalance.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIALLY_PAID");
        invoice.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : invoice.getPaymentMethod());

        InvoicePayment p = InvoicePayment.builder()
                .invoice(invoice)
                .amount(payAmount)
                .paymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now())
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "CASH")
                .transactionReference(req.getTransactionReference())
                .notes(req.getNotes())
                .receivedBy(username != null ? username : req.getReceivedBy())
                .build();

        invoice.getPayments().add(p);

        InvoiceAuditLog logEntry = InvoiceAuditLog.builder()
                .invoice(invoice)
                .action("PAYMENT_RECORDED")
                .details(String.format("Payment of ₹%s recorded via %s. Ref: %s",
                        payAmount.toPlainString(), p.getPaymentMethod(), p.getTransactionReference() != null ? p.getTransactionReference() : "N/A"))
                .performedBy(username != null ? username : "STAFF")
                .timestamp(LocalDateTime.now())
                .build();

        invoice.getAuditLogs().add(logEntry);

        invoice = invoiceRepository.save(invoice);
        return mapToInvoiceResponseDto(invoice);
    }

    // ============================================================
    // 6. CANCEL / VOID INVOICE
    // ============================================================

    @Transactional
    public InvoiceResponseDto cancelInvoice(Long invoiceId, CancelInvoiceRequestDto req, String username) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

        if (invoice.isCancelled()) {
            throw new IllegalStateException("Invoice is already cancelled.");
        }

        invoice.setCancelled(true);
        invoice.setCancellationReason(req.getReason() != null ? req.getReason() : "Cancelled by user");
        invoice.setCancelledAt(LocalDateTime.now());
        invoice.setCancelledBy(username != null ? username : req.getCancelledBy());
        invoice.setStatus("CANCELLED");

        InvoiceAuditLog logEntry = InvoiceAuditLog.builder()
                .invoice(invoice)
                .action("CANCELLED")
                .details(String.format("Invoice cancelled. Reason: %s", invoice.getCancellationReason()))
                .performedBy(username != null ? username : "STAFF")
                .timestamp(LocalDateTime.now())
                .build();

        invoice.getAuditLogs().add(logEntry);
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice {} successfully cancelled by {}", invoice.getInvoiceNumber(), username);
        return mapToInvoiceResponseDto(invoice);
    }

    // ============================================================
    // 7. DASHBOARD SUMMARY
    // ============================================================

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal todaySales = invoiceRepository.sumGrandTotalByDate(today);
        BigDecimal todayTaxable = invoiceRepository.sumTaxableByDate(today);
        BigDecimal todayGst = invoiceRepository.sumTaxByDate(today);
        long todayCount = invoiceRepository.countByInvoiceDateAndNotCancelled(today);

        BigDecimal monthTurnover = invoiceRepository.sumGrandTotalBetween(monthStart, monthEnd);
        BigDecimal monthTaxable = invoiceRepository.sumTaxableBetween(monthStart, monthEnd);
        BigDecimal monthGst = invoiceRepository.sumTaxBetween(monthStart, monthEnd);
        long monthCount = invoiceRepository.countBetween(monthStart, monthEnd);
        long monthCancelled = invoiceRepository.countCancelledBetween(monthStart, monthEnd);

        BigDecimal fabricSales = invoiceItemRepository.sumFabricSalesBetween(monthStart, monthEnd);
        BigDecimal stitchingSales = invoiceItemRepository.sumStitchingSalesBetween(monthStart, monthEnd);
        BigDecimal totalPaid = invoiceRepository.sumPaidBetween(monthStart, monthEnd);
        BigDecimal totalOutstanding = invoiceRepository.sumTotalOutstanding();

        // 14-day daily chart
        List<ChartDataPointDto> dailySalesChart = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd MMM");
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            BigDecimal daySales = invoiceRepository.sumGrandTotalByDate(d);
            dailySalesChart.add(ChartDataPointDto.builder()
                    .label(d.format(dayFormatter))
                    .value(daySales)
                    .build());
        }

        // 6-month turnover chart
        List<ChartDataPointDto> monthlySalesChart = new ArrayList<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 5; i >= 0; i--) {
            LocalDate m = today.minusMonths(i);
            LocalDate start = m.withDayOfMonth(1);
            LocalDate end = m.withDayOfMonth(m.lengthOfMonth());
            BigDecimal mSales = invoiceRepository.sumGrandTotalBetween(start, end);
            BigDecimal mTaxable = invoiceRepository.sumTaxableBetween(start, end);
            monthlySalesChart.add(ChartDataPointDto.builder()
                    .label(m.format(monthFormatter))
                    .value(mSales)
                    .secondaryValue(mTaxable)
                    .build());
        }

        // Payment distribution
        List<PaymentDistributionDto> paymentDistribution = new ArrayList<>();
        List<Object[]> payDistRows = invoicePaymentRepository.getPaymentMethodDistributionBetween(monthStart, monthEnd);
        for (Object[] row : payDistRows) {
            String method = (String) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal sum = (BigDecimal) row[2];
            paymentDistribution.add(PaymentDistributionDto.builder()
                    .method(method != null ? method : "OTHER")
                    .count(count)
                    .totalAmount(sum != null ? sum : BigDecimal.ZERO)
                    .build());
        }

        return DashboardSummaryDto.builder()
                .todaySales(todaySales)
                .todayTaxable(todayTaxable)
                .todayGst(todayGst)
                .todayInvoicesCount(todayCount)
                .monthTurnover(monthTurnover)
                .monthTaxable(monthTaxable)
                .monthGst(monthGst)
                .monthInvoicesCount(monthCount)
                .monthCancelledCount(monthCancelled)
                .fabricSales(fabricSales)
                .stitchingSales(stitchingSales)
                .mixedSales(monthTurnover.subtract(fabricSales).subtract(stitchingSales).max(BigDecimal.ZERO))
                .totalOutstanding(totalOutstanding)
                .totalPaid(totalPaid)
                .pendingAmount(totalOutstanding)
                .dailySalesChart(dailySalesChart)
                .monthlySalesChart(monthlySalesChart)
                .paymentDistribution(paymentDistribution)
                .build();
    }

    // ============================================================
    // 8. MONTHLY GST & TURNOVER REPORT
    // ============================================================

    @Transactional(readOnly = true)
    public MonthlyReportDto getMonthlyReport(int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Invoice> invoices = invoiceRepository.findInvoicesForReport(start, end, null);

        int count = 0;
        int cancelled = 0;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal turnover = BigDecimal.ZERO;

        List<InvoiceResponseDto> invoiceDtos = new ArrayList<>();

        for (Invoice inv : invoices) {
            invoiceDtos.add(mapToInvoiceResponseDto(inv));
            if (inv.isCancelled()) {
                cancelled++;
                continue;
            }
            count++;
            taxable = taxable.add(inv.getTaxableAmount());
            cgst = cgst.add(inv.getCgstAmount());
            sgst = sgst.add(inv.getSgstAmount());
            igst = igst.add(inv.getIgstAmount());
            tax = tax.add(inv.getTotalTax());
            turnover = turnover.add(inv.getGrandTotal());
        }

        // HSN Summary
        List<HsnSummaryItemDto> hsnList = getHsnSummary(start, end);

        return MonthlyReportDto.builder()
                .month(month)
                .year(year)
                .invoiceCount(count)
                .cancelledCount(cancelled)
                .taxableAmount(taxable)
                .cgstAmount(cgst)
                .sgstAmount(sgst)
                .igstAmount(igst)
                .totalTax(tax)
                .totalTurnover(turnover)
                .hsnSummary(hsnList)
                .invoices(invoiceDtos)
                .build();
    }

    // ============================================================
    // 9. CA / ACCOUNTANT EXPORT
    // ============================================================

    @Transactional(readOnly = true)
    public CaExportDto getCaExport(LocalDate startDate, LocalDate endDate) {
        BillingConfig config = billingConfigService.getConfig();

        List<Invoice> invoices = invoiceRepository.findInvoicesForReport(startDate, endDate, null);

        List<InvoiceResponseDto> b2b = new ArrayList<>();
        List<InvoiceResponseDto> b2c = new ArrayList<>();
        List<InvoiceResponseDto> cancelled = new ArrayList<>();

        BigDecimal b2bTaxable = BigDecimal.ZERO;
        BigDecimal b2bTax = BigDecimal.ZERO;
        BigDecimal b2cTaxable = BigDecimal.ZERO;
        BigDecimal b2cTax = BigDecimal.ZERO;

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalTurnover = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            InvoiceResponseDto dto = mapToInvoiceResponseDto(inv);
            if (inv.isCancelled()) {
                cancelled.add(dto);
                continue;
            }

            totalTaxable = totalTaxable.add(inv.getTaxableAmount());
            totalCgst = totalCgst.add(inv.getCgstAmount());
            totalSgst = totalSgst.add(inv.getSgstAmount());
            totalIgst = totalIgst.add(inv.getIgstAmount());
            totalTax = totalTax.add(inv.getTotalTax());
            totalTurnover = totalTurnover.add(inv.getGrandTotal());

            boolean isB2B = inv.getCustomerGstin() != null && !inv.getCustomerGstin().trim().isEmpty();
            if (isB2B) {
                b2b.add(dto);
                b2bTaxable = b2bTaxable.add(inv.getTaxableAmount());
                b2bTax = b2bTax.add(inv.getTotalTax());
            } else {
                b2c.add(dto);
                b2cTaxable = b2cTaxable.add(inv.getTaxableAmount());
                b2cTax = b2cTax.add(inv.getTotalTax());
            }
        }

        List<HsnSummaryItemDto> hsnList = getHsnSummary(startDate, endDate);

        return CaExportDto.builder()
                .period(startDate.toString() + " to " + endDate.toString())
                .gstin(config.getGstin())
                .shopName(config.getShopName())
                .b2bInvoicesCount(b2b.size())
                .b2bTaxable(b2bTaxable)
                .b2bTax(b2bTax)
                .b2cInvoicesCount(b2c.size())
                .b2cTaxable(b2cTaxable)
                .b2cTax(b2cTax)
                .totalTaxable(totalTaxable)
                .totalCgst(totalCgst)
                .totalSgst(totalSgst)
                .totalIgst(totalIgst)
                .totalTax(totalTax)
                .totalTurnover(totalTurnover)
                .hsnSummary(hsnList)
                .b2bInvoices(b2b)
                .b2cInvoices(b2c)
                .cancelledInvoices(cancelled)
                .build();
    }

    // ============================================================
    // 10. MONTH-END CLOSING
    // ============================================================

    @Transactional
    public MonthEndClosing closeMonth(MonthEndClosingRequestDto req) {
        if (monthEndClosingRepository.existsByClosingMonthAndClosingYear(req.getMonth(), req.getYear())) {
            throw new IllegalStateException("Month " + req.getMonth() + "/" + req.getYear() + " is already closed.");
        }

        MonthlyReportDto report = getMonthlyReport(req.getMonth(), req.getYear());

        MonthEndClosing closing = MonthEndClosing.builder()
                .closingMonth(req.getMonth())
                .closingYear(req.getYear())
                .totalInvoices(report.getInvoiceCount())
                .totalTaxableAmount(report.getTaxableAmount())
                .totalCgst(report.getCgstAmount())
                .totalSgst(report.getSgstAmount())
                .totalIgst(report.getIgstAmount())
                .totalTurnover(report.getTotalTurnover())
                .cancelledInvoicesCount(report.getCancelledCount())
                .closedAt(LocalDateTime.now())
                .closedBy(req.getClosedBy() != null ? req.getClosedBy() : "OWNER")
                .notes(req.getNotes())
                .build();

        return monthEndClosingRepository.save(closing);
    }

    private List<HsnSummaryItemDto> getHsnSummary(LocalDate start, LocalDate end) {
        List<Object[]> rows = invoiceItemRepository.getHsnSummaryBetween(start, end);
        List<HsnSummaryItemDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            String code = (String) r[0];
            BigDecimal qty = (BigDecimal) r[1];
            BigDecimal taxable = (BigDecimal) r[2];
            BigDecimal rate = (BigDecimal) r[3];
            BigDecimal cgst = (BigDecimal) r[4];
            BigDecimal sgst = (BigDecimal) r[5];
            BigDecimal igst = (BigDecimal) r[6];
            BigDecimal tax = (BigDecimal) r[7];
            BigDecimal total = (BigDecimal) r[8];

            result.add(HsnSummaryItemDto.builder()
                    .hsnSacCode(code != null ? code : "9988")
                    .totalQuantity(qty != null ? qty : BigDecimal.ZERO)
                    .taxableAmount(taxable != null ? taxable : BigDecimal.ZERO)
                    .gstRate(rate != null ? rate : BigDecimal.ZERO)
                    .cgstAmount(cgst != null ? cgst : BigDecimal.ZERO)
                    .sgstAmount(sgst != null ? sgst : BigDecimal.ZERO)
                    .igstAmount(igst != null ? igst : BigDecimal.ZERO)
                    .totalTax(tax != null ? tax : BigDecimal.ZERO)
                    .totalAmount(total != null ? total : BigDecimal.ZERO)
                    .build());
        }
        return result;
    }

    // ============================================================
    // DTO MAPPER
    // ============================================================

    private InvoiceResponseDto mapToInvoiceResponseDto(Invoice inv) {
        List<InvoiceItemResponseDto> itemDtos = new ArrayList<>();
        if (inv.getItems() != null) {
            for (InvoiceItem it : inv.getItems()) {
                itemDtos.add(InvoiceItemResponseDto.builder()
                        .id(it.getId())
                        .itemOrder(it.getItemOrder())
                        .itemType(it.getItemType())
                        .description(it.getDescription())
                        .hsnSacCode(it.getHsnSacCode())
                        .quantity(it.getQuantity())
                        .unit(it.getUnit())
                        .rate(it.getRate())
                        .grossAmount(it.getGrossAmount())
                        .discount(it.getDiscount())
                        .taxableAmount(it.getTaxableAmount())
                        .gstRate(it.getGstRate())
                        .cgstRate(it.getCgstRate())
                        .sgstRate(it.getSgstRate())
                        .igstRate(it.getIgstRate())
                        .cgstAmount(it.getCgstAmount())
                        .sgstAmount(it.getSgstAmount())
                        .igstAmount(it.getIgstAmount())
                        .totalTax(it.getTotalTax())
                        .totalAmount(it.getTotalAmount())
                        .garmentId(it.getGarmentId())
                        .orderId(it.getOrderId())
                        .fabricId(it.getFabricId())
                        .build());
            }
        }

        List<InvoiceOrderResponseDto> orderDtos = new ArrayList<>();
        if (inv.getInvoiceOrders() != null) {
            for (InvoiceOrder io : inv.getInvoiceOrders()) {
                orderDtos.add(InvoiceOrderResponseDto.builder()
                        .id(io.getId())
                        .orderId(io.getOrder() != null ? io.getOrder().getId() : null)
                        .billedAmount(io.getBilledAmount())
                        .isPartial(io.isPartial())
                        .sampleBookPageNumber(io.getOrder() != null ? io.getOrder().getSampleBookPageNumber() : null)
                        .orderDate(io.getOrder() != null ? io.getOrder().getOrderDate() : null)
                        .build());
            }
        }

        List<InvoicePaymentResponseDto> paymentDtos = new ArrayList<>();
        if (inv.getPayments() != null) {
            for (InvoicePayment p : inv.getPayments()) {
                paymentDtos.add(InvoicePaymentResponseDto.builder()
                        .id(p.getId())
                        .amount(p.getAmount())
                        .paymentDate(p.getPaymentDate())
                        .paymentMethod(p.getPaymentMethod())
                        .transactionReference(p.getTransactionReference())
                        .notes(p.getNotes())
                        .receivedBy(p.getReceivedBy())
                        .createdAt(p.getCreatedAt())
                        .build());
            }
        }

        List<InvoiceAuditLogDto> auditDtos = new ArrayList<>();
        if (inv.getAuditLogs() != null) {
            for (InvoiceAuditLog l : inv.getAuditLogs()) {
                auditDtos.add(InvoiceAuditLogDto.builder()
                        .id(l.getId())
                        .action(l.getAction())
                        .details(l.getDetails())
                        .performedBy(l.getPerformedBy())
                        .timestamp(l.getTimestamp())
                        .build());
            }
        }

        return InvoiceResponseDto.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(inv.getDueDate())
                .invoiceType(inv.getInvoiceType())
                .classification(inv.getClassification())
                .status(inv.getStatus())
                .isTaxInclusive(inv.isTaxInclusive())
                .placeOfSupply(inv.getPlaceOfSupply())
                .isInterState(inv.isInterState())
                .customerId(inv.getCustomer() != null ? inv.getCustomer().getId() : null)
                .customerName(inv.getCustomerName())
                .customerPhone(inv.getCustomerPhone())
                .customerAddress(inv.getCustomerAddress())
                .customerGstin(inv.getCustomerGstin())
                .customerStateCode(inv.getCustomerStateCode())
                .subtotal(inv.getSubtotal())
                .discount(inv.getDiscount())
                .taxableAmount(inv.getTaxableAmount())
                .cgstAmount(inv.getCgstAmount())
                .sgstAmount(inv.getSgstAmount())
                .igstAmount(inv.getIgstAmount())
                .totalTax(inv.getTotalTax())
                .roundOff(inv.getRoundOff())
                .grandTotal(inv.getGrandTotal())
                .paidAmount(inv.getPaidAmount())
                .balanceAmount(inv.getBalanceAmount())
                .paymentStatus(inv.getPaymentStatus())
                .paymentMethod(inv.getPaymentMethod())
                .notes(inv.getNotes())
                .termsAndConditions(inv.getTermsAndConditions())
                .isCancelled(inv.isCancelled())
                .cancellationReason(inv.getCancellationReason())
                .cancelledAt(inv.getCancelledAt())
                .cancelledBy(inv.getCancelledBy())
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .items(itemDtos)
                .invoiceOrders(orderDtos)
                .payments(paymentDtos)
                .auditLogs(auditDtos)
                .build();
    }

    @Transactional
    public void updateGarmentFabricSource(Long garmentId, boolean customerFabric) {
        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new com.sff.authapp.login.exceptions.ResourceNotFoundException("Garment not found with id: " + garmentId));
        garment.setCustomerFabric(customerFabric);
        garmentRepository.save(garment);
        log.info("Updated garment {} fabric source: customerFabric={}", garmentId, customerFabric);
    }
}
