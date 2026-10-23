package com.sff.authapp.login.dtos.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerProfileOrder(

        Long id,

        String orderNumber,

        LocalDate orderDate,

        LocalDate deliveryDate,

        String status,

        BigDecimal total,

        BigDecimal balanceDue,

        List<CustomerProfileGarment> garments,

        String billImageUrl

) {
}