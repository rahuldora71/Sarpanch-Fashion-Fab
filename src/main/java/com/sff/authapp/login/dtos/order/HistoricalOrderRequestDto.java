package com.sff.authapp.login.dtos.order;

import com.sff.authapp.login.entities.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalOrderRequestDto {

    // Customer details (either customerId for existing or new customer fields)
    private Long customerId;
    private String name;
    private String phone;
    private String alternatePhone;
    private String address;

    // Order slip details
    private Long orderNumber;

    @NotNull(message = "Order date is required")
    private LocalDate orderDate;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    private BigDecimal advancePayment;

    private PaymentMethod paymentMethod;

    @Builder.Default
    private Boolean isDelivered = true;

    private String billImageUrl;
    private String billImagePublicId;

    private String notes;
}
