package com.sff.authapp.login.dtos.order;

import com.sff.authapp.login.entities.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalOrderResponseDto {

    private Long orderId;
    private Long orderNumber;
    private LocalDate orderDate;
    private BigDecimal totalAmount;
    private BigDecimal advancePayment;
    private BigDecimal balanceDue;
    private Boolean isDelivered;
    private String billImageUrl;
    private String billImagePublicId;
    private PaymentMethod paymentMethod;

    // Customer details
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerCode;

    // Next suggested order number
    private Long nextSuggestedOrderNumber;
}
