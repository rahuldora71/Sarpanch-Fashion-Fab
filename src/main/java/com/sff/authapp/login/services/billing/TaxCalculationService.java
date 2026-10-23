package com.sff.authapp.login.services.billing;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxCalculationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    @Getter
    @Builder
    public static class LineTaxResult {
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
    }

    public LineTaxResult calculateLineItem(
            BigDecimal quantity,
            BigDecimal rate,
            BigDecimal discount,
            BigDecimal gstRate,
            boolean isTaxInclusive,
            boolean isInterState
    ) {
        if (quantity == null) quantity = BigDecimal.ONE;
        if (rate == null) rate = BigDecimal.ZERO;
        if (discount == null) discount = BigDecimal.ZERO;
        if (gstRate == null) gstRate = BigDecimal.ZERO;

        BigDecimal grossAmount = quantity.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal effectiveNet = grossAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (effectiveNet.compareTo(BigDecimal.ZERO) < 0) {
            effectiveNet = BigDecimal.ZERO;
        }

        BigDecimal taxableAmount;
        BigDecimal totalTax;
        BigDecimal totalAmount;

        if (gstRate.compareTo(BigDecimal.ZERO) == 0) {
            taxableAmount = effectiveNet;
            totalTax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            totalAmount = effectiveNet;
        } else if (isTaxInclusive) {
            // totalAmount = effectiveNet
            // taxableAmount = totalAmount / (1 + gstRate / 100)
            BigDecimal factor = BigDecimal.ONE.add(gstRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
            taxableAmount = effectiveNet.divide(factor, 2, RoundingMode.HALF_UP);
            totalTax = effectiveNet.subtract(taxableAmount).setScale(2, RoundingMode.HALF_UP);
            totalAmount = effectiveNet;
        } else {
            // totalAmount = taxableAmount + totalTax
            taxableAmount = effectiveNet;
            totalTax = taxableAmount.multiply(gstRate).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            totalAmount = taxableAmount.add(totalTax).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal cgstRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgstRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal igstRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal cgstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal igstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (isInterState) {
            igstRate = gstRate.setScale(2, RoundingMode.HALF_UP);
            igstAmount = totalTax;
        } else {
            cgstRate = gstRate.divide(TWO, 2, RoundingMode.HALF_UP);
            sgstRate = gstRate.subtract(cgstRate).setScale(2, RoundingMode.HALF_UP);

            cgstAmount = totalTax.divide(TWO, 2, RoundingMode.HALF_UP);
            sgstAmount = totalTax.subtract(cgstAmount).setScale(2, RoundingMode.HALF_UP);
        }

        return LineTaxResult.builder()
                .grossAmount(grossAmount)
                .discount(discount)
                .taxableAmount(taxableAmount)
                .gstRate(gstRate.setScale(2, RoundingMode.HALF_UP))
                .cgstRate(cgstRate)
                .sgstRate(sgstRate)
                .igstRate(igstRate)
                .cgstAmount(cgstAmount)
                .sgstAmount(sgstAmount)
                .igstAmount(igstAmount)
                .totalTax(totalTax)
                .totalAmount(totalAmount)
                .build();
    }
}
