package com.sff.authapp.login.dtos.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PublicOrderTrackingDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PublicOrderTrackingResponse {
        private Long orderId;
        private Long orderNumber;
        private String trackingToken;
        private LocalDate orderDate;
        private LocalDate estimatedDeliveryDate;

        // Current status: "BOOKED" | "IN_PROGRESS" | "READY" | "DELIVERED"
        private String status;
        private String statusLabel;
        private String currentStage;
        private int progressPercentage;
        private boolean isDelivered;
        private LocalDate deliveryDate;

        // Customer details (sanitized/safe for public)
        private String customerName;
        private String customerPhoneMasked;
        private String sampleBookPageNumber;

        // Financials
        private PublicOrderFinancials financials;

        // Timeline milestones
        @Builder.Default
        private List<PublicTimelineStageDto> timeline = new ArrayList<>();

        // Garments (customer-facing attributes only)
        @Builder.Default
        private List<PublicGarmentDto> garments = new ArrayList<>();

        // Review state
        private PublicOrderReviewDto review;
        private boolean canReview;

        // Timestamp
        private LocalDateTime lastRefreshedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PublicOrderFinancials {
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal additionalCharges;
        private BigDecimal totalAmount;
        private BigDecimal amountPaid;
        private BigDecimal balanceDue;
        private boolean isFullyPaid;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PublicTimelineStageDto {
        private String stageKey; // "BOOKED", "CUTTING", "STITCHING", "READY", "DELIVERED"
        private String title;
        private String description;
        private String state; // "COMPLETED", "CURRENT", "PENDING"
        private String timestamp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PublicGarmentDto {
        private Long garmentId;
        private String serviceName;
        private BigDecimal price;
        private String sampleNumber;
        private String status;
        private String statusLabel;
        private boolean customerFabric;
        private String fabricSourceLabel;
        private LocalDate tryDate;
        private LocalDate dueDate;
        @Builder.Default
        private List<String> addOns = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PublicOrderReviewDto {
        private Long id;
        private Integer rating;
        private String comment;
        private String customerName;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PublicOrderReviewRequest {
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1 star")
        @Max(value = 5, message = "Rating cannot exceed 5 stars")
        private Integer rating;

        @Size(max = 500, message = "Review comment cannot exceed 500 characters")
        private String comment;
    }
}
