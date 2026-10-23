package com.sff.authapp.login.dtos.billing;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillingConfigDto {
        private Long id;
        private String shopName;
        private String tagline;
        private String gstin;
        private String panNumber;
        private String state;
        private String stateCode;
        private String address;
        private String phone;
        private String email;
        private String bankName;
        private String accountNumber;
        private String ifscCode;
        private String upiId;
        private String invoicePrefix;
        private String financialYear;
        private boolean defaultTaxInclusive;
        private boolean includeStitchingOnly;
        private boolean allowMixedOrders;
        private boolean allowFabricOnly;
        private boolean allowManualInvoices;
        private boolean allowCustomLineItems;
        private boolean allowPartialBilling;
        private boolean enforceSequentialNumbering;
        private String termsAndConditions;
        private String footerNote;
        private String defaultInvoiceTemplate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaxRuleDto {
        private Long id;
        private String taxName;
        private String taxType;
        private BigDecimal gstRate;
        private BigDecimal cgstRate;
        private BigDecimal sgstRate;
        private BigDecimal igstRate;
        private String hsnSacCode;
        private String category;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private boolean active;
        private boolean isDefault;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EligibleGarmentItemDto {
        private Long garmentId;
        private String serviceName;
        private BigDecimal price;
        private boolean customerFabric;
        private String sampleNumber;
        private int addOnsCount;
        private BigDecimal addOnsTotal;
        private BigDecimal serviceBasePrice;
        private BigDecimal stitchingAmount;
        private BigDecimal fabricAmount;
        private boolean hasShopFabric;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EligibleOrderDto {
        private Long orderId;
        private String orderNumber;
        private String sampleBookPageNumber;
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private String customerAddress;
        private String customerGstin;
        private LocalDate orderDate;
        private int garmentsCount;
        private String servicesSummary;
        private String fabricSummary;
        private BigDecimal stitchingAmount;
        private BigDecimal fabricAmount;
        private BigDecimal totalOrderAmount;
        private BigDecimal paidAmount;
        private BigDecimal balanceAmount;
        private String classification; // STITCHING_ONLY, FABRIC_ONLY, FABRIC_AND_STITCHING, SERVICE_ONLY, MIXED
        private String billingStatus; // UNBILLED, PARTIALLY_BILLED, BILLED
        private String paymentStatus; // PAID, PARTIAL, UNPAID
        @Builder.Default
        private List<EligibleGarmentItemDto> garments = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceItemDraftDto {
        private String itemType; // STITCHING, FABRIC, ACCESSORY, ADD_ON, CUSTOM
        private String description;
        private String hsnSacCode;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal rate;
        private BigDecimal discount;
        private BigDecimal gstRate;
        private Long garmentId;
        private Long orderId;
        private Long fabricId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerateInvoiceRequestDto {
        private List<Long> orderIds;
        private List<Long> selectedGarmentIds;
        private String invoiceType;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private Boolean isTaxInclusive;
        private String placeOfSupply;
        private String customerGstin;
        private String customerStateCode;
        private BigDecimal discount;
        private BigDecimal roundOff;
        private String notes;
        private String termsAndConditions;
        private String paymentMethod;
        private BigDecimal advancePaidAmount;
        private String billingMode; // FABRIC_CONVERTED, GARMENT_CONSOLIDATED, DETAILED_ITEMIZED
        @Builder.Default
        private List<InvoiceItemDraftDto> overrideItems = new ArrayList<>();
        @Builder.Default
        private List<InvoiceItemDraftDto> customItems = new ArrayList<>();
        @Builder.Default
        private Map<Long, Boolean> garmentCustomerFabricOverrides = new HashMap<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateManualInvoiceRequestDto {
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private String customerAddress;
        private String customerGstin;
        private String customerStateCode;
        private String invoiceType;
        private String classification;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private Boolean isTaxInclusive;
        private String placeOfSupply;
        @Builder.Default
        private List<InvoiceItemDraftDto> items = new ArrayList<>();
        private BigDecimal discount;
        private BigDecimal roundOff;
        private String notes;
        private String termsAndConditions;
        private String paymentMethod;
        private BigDecimal paidAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceItemResponseDto {
        private Long id;
        private int itemOrder;
        private String itemType;
        private String description;
        private String hsnSacCode;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal rate;
        private BigDecimal grossAmount;
        private BigDecimal discount;
        private BigDecimal taxableAmount;
        private BigDecimal gstRate;
        private BigDecimal cgstRate;
        private BigDecimal sgstRate;
        private BigDecimal igstRate;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
        private BigDecimal totalTax;
        private BigDecimal totalAmount;
        private Long garmentId;
        private Long orderId;
        private Long fabricId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoicePaymentResponseDto {
        private Long id;
        private BigDecimal amount;
        private LocalDate paymentDate;
        private String paymentMethod;
        private String transactionReference;
        private String notes;
        private String receivedBy;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceOrderResponseDto {
        private Long id;
        private Long orderId;
        private BigDecimal billedAmount;
        private boolean isPartial;
        private String sampleBookPageNumber;
        private LocalDate orderDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceAuditLogDto {
        private Long id;
        private String action;
        private String details;
        private String performedBy;
        private LocalDateTime timestamp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceResponseDto {
        private Long id;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private String invoiceType;
        private String classification;
        private String status;
        private boolean isTaxInclusive;
        private String placeOfSupply;
        private boolean isInterState;
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private String customerAddress;
        private String customerGstin;
        private String customerStateCode;
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal taxableAmount;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
        private BigDecimal totalTax;
        private BigDecimal roundOff;
        private BigDecimal grandTotal;
        private BigDecimal paidAmount;
        private BigDecimal balanceAmount;
        private String paymentStatus;
        private String paymentMethod;
        private String notes;
        private String termsAndConditions;
        private boolean isCancelled;
        private String cancellationReason;
        private LocalDateTime cancelledAt;
        private String cancelledBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        @Builder.Default
        private List<InvoiceItemResponseDto> items = new ArrayList<>();
        @Builder.Default
        private List<InvoiceOrderResponseDto> invoiceOrders = new ArrayList<>();
        @Builder.Default
        private List<InvoicePaymentResponseDto> payments = new ArrayList<>();
        @Builder.Default
        private List<InvoiceAuditLogDto> auditLogs = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordPaymentRequestDto {
        private BigDecimal amount;
        private LocalDate paymentDate;
        private String paymentMethod;
        private String transactionReference;
        private String notes;
        private String receivedBy;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelInvoiceRequestDto {
        private String reason;
        private String cancelledBy;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartDataPointDto {
        private String label;
        private BigDecimal value;
        private BigDecimal secondaryValue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDistributionDto {
        private String method;
        private long count;
        private BigDecimal totalAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardSummaryDto {
        private BigDecimal todaySales;
        private BigDecimal todayTaxable;
        private BigDecimal todayGst;
        private long todayInvoicesCount;
        private BigDecimal monthTurnover;
        private BigDecimal monthTaxable;
        private BigDecimal monthGst;
        private long monthInvoicesCount;
        private long monthCancelledCount;
        private BigDecimal fabricSales;
        private BigDecimal stitchingSales;
        private BigDecimal mixedSales;
        private BigDecimal totalOutstanding;
        private BigDecimal totalPaid;
        private BigDecimal pendingAmount;
        @Builder.Default
        private List<ChartDataPointDto> dailySalesChart = new ArrayList<>();
        @Builder.Default
        private List<ChartDataPointDto> monthlySalesChart = new ArrayList<>();
        @Builder.Default
        private List<ChartDataPointDto> fabricVsStitchingChart = new ArrayList<>();
        @Builder.Default
        private List<PaymentDistributionDto> paymentDistribution = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FabricDto {
        private Long id;
        private String name;
        private String fabricCode;
        private String fabricType;
        private String color;
        private BigDecimal widthInches;
        private BigDecimal stockMeters;
        private BigDecimal costPricePerMeter;
        private BigDecimal sellingPricePerMeter;
        private String hsnCode;
        private Long taxRuleId;
        private String taxRuleName;
        private BigDecimal minStockAlert;
        private String supplier;
        private String notes;
        private boolean active;
        private boolean isLowStock;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FabricStockAdjustmentDto {
        private Long fabricId;
        private String transactionType; // PURCHASE, SALE, ORDER_CONSUMPTION, ADJUSTMENT, RETURN, WASTAGE
        private BigDecimal quantityMeters;
        private String referenceNo;
        private String referenceType;
        private String notes;
        private String performedBy;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FabricStockTransactionDto {
        private Long id;
        private Long fabricId;
        private String fabricName;
        private String fabricCode;
        private String transactionType;
        private BigDecimal quantityMeters;
        private BigDecimal previousStock;
        private BigDecimal newStock;
        private String referenceNo;
        private String referenceType;
        private String notes;
        private String performedBy;
        private LocalDateTime transactionDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FabricConsumptionRuleDto {
        private Long id;
        private String garmentType;
        private BigDecimal widthInches;
        private BigDecimal baseConsumptionMeters;
        private BigDecimal wastagePercent;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FabricEstimateRequestDto {
        private String garmentType;
        private BigDecimal widthInches;
        private int quantity;
        private BigDecimal chestSizeInches;
        private BigDecimal lengthInches;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FabricEstimateResponseDto {
        private String garmentType;
        private BigDecimal widthInches;
        private int quantity;
        private BigDecimal baseMeters;
        private BigDecimal wastageMeters;
        private BigDecimal totalEstimatedMeters;
        private String notes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HsnSummaryItemDto {
        private String hsnSacCode;
        private BigDecimal totalQuantity;
        private BigDecimal taxableAmount;
        private BigDecimal gstRate;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
        private BigDecimal totalTax;
        private BigDecimal totalAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyReportDto {
        private int month;
        private int year;
        private int invoiceCount;
        private int cancelledCount;
        private BigDecimal taxableAmount;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
        private BigDecimal totalTax;
        private BigDecimal totalTurnover;
        @Builder.Default
        private List<HsnSummaryItemDto> hsnSummary = new ArrayList<>();
        @Builder.Default
        private List<InvoiceResponseDto> invoices = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CaExportDto {
        private String period;
        private String gstin;
        private String shopName;
        private int b2bInvoicesCount;
        private BigDecimal b2bTaxable;
        private BigDecimal b2bTax;
        private int b2cInvoicesCount;
        private BigDecimal b2cTaxable;
        private BigDecimal b2cTax;
        private BigDecimal totalTaxable;
        private BigDecimal totalCgst;
        private BigDecimal totalSgst;
        private BigDecimal totalIgst;
        private BigDecimal totalTax;
        private BigDecimal totalTurnover;
        @Builder.Default
        private List<HsnSummaryItemDto> hsnSummary = new ArrayList<>();
        @Builder.Default
        private List<InvoiceResponseDto> b2bInvoices = new ArrayList<>();
        @Builder.Default
        private List<InvoiceResponseDto> b2cInvoices = new ArrayList<>();
        @Builder.Default
        private List<InvoiceResponseDto> cancelledInvoices = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthEndClosingRequestDto {
        private int month;
        private int year;
        private String closedBy;
        private String notes;
    }
}
