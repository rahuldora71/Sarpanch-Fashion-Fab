package com.sff.authapp.login.helpers;

import com.sff.authapp.login.entities.AdditionalCharge;
import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentAddOn;
import com.sff.authapp.login.entities.Order;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Dedicated utility and helper for tailoring garment & order classification,
 * separating stitching labor from shop-provided fabric costs.
 *
 * Domain Rules:
 * 1. Base Stitching Rate: The standard tailoring price defined on the service (e.g. Coat = ₹2,750, Pant = ₹400).
 * 2. Garment Price: The total garment price charged in the order (e.g. Coat = ₹3,500, Pant = ₹400).
 * 3. Excess Price: When Garment Price > Service Base Price, the excess represents Shop Fabric charged to the customer!
 *    (e.g. Coat ₹3,500 - ₹2,750 = ₹750 Fabric Amount).
 * 4. Add-ons: Micro customization services (e.g. Pant 2 Add-ons = ₹50) are tailoring labor, added to Stitching Amount.
 * 5. Order Classification:
 *    - STITCHING_ONLY: All garments are customer cloth (fabric amount = ₹0).
 *    - FABRIC_AND_STITCHING: All garments include shop-provided fabric.
 *    - MIXED: The order contains a mixture of customer cloth and shop-provided fabric garments.
 *    - FABRIC_ONLY: Fabric sale without tailoring labor.
 *    - SERVICE_ONLY: Non-garment labor / alteration charges only.
 */
public final class GarmentClassificationHelper {

    private GarmentClassificationHelper() {
        // Utility class
    }

    @Getter
    @Builder
    public static class GarmentBreakdown {
        private final BigDecimal garmentPrice;
        private final BigDecimal serviceBasePrice;
        private final BigDecimal addOnsTotal;
        private final int addOnsCount;
        private final BigDecimal stitchingAmount;
        private final BigDecimal fabricAmount;
        private final boolean hasShopFabric;
        private final boolean isCustomerFabric;
    }

    @Getter
    @Builder
    public static class OrderBreakdown {
        private final int totalGarments;
        private final int shopFabricGarmentsCount;
        private final int customerFabricGarmentsCount;
        private final BigDecimal totalStitchingAmount;
        private final BigDecimal totalFabricAmount;
        private final String classification;
        private final String fabricSummary;
    }

    /**
     * Determines whether a garment used customer cloth or shop-provided fabric.
     *
     * @param explicitCustomerFabric user toggle from UI (nullable)
     * @param garmentPrice price recorded on garment
     * @param serviceBasePrice standard base price of the tailoring service
     * @return true if customer provided their own cloth, false if showroom fabric is included
     */
    public static boolean determineCustomerFabric(
            Boolean explicitCustomerFabric,
            BigDecimal garmentPrice,
            BigDecimal serviceBasePrice
    ) {
        BigDecimal gPrice = garmentPrice != null ? garmentPrice : BigDecimal.ZERO;
        BigDecimal sPrice = serviceBasePrice != null ? serviceBasePrice : BigDecimal.ZERO;

        // If the garment price strictly exceeds the service base price, shop fabric was charged!
        if (sPrice.compareTo(BigDecimal.ZERO) > 0 && gPrice.compareTo(sPrice) > 0) {
            return false;
        }

        // If explicitly set by user, honor explicit choice
        if (explicitCustomerFabric != null) {
            return explicitCustomerFabric;
        }

        // Default to customer cloth when price <= base price
        return true;
    }

    /**
     * Analyzes a Garment entity and breaks down stitching vs fabric amounts.
     */
    public static GarmentBreakdown analyzeGarment(Garment garment) {
        if (garment == null) {
            return GarmentBreakdown.builder()
                    .garmentPrice(BigDecimal.ZERO)
                    .serviceBasePrice(BigDecimal.ZERO)
                    .addOnsTotal(BigDecimal.ZERO)
                    .addOnsCount(0)
                    .stitchingAmount(BigDecimal.ZERO)
                    .fabricAmount(BigDecimal.ZERO)
                    .hasShopFabric(false)
                    .isCustomerFabric(true)
                    .build();
        }

        BigDecimal garmentPrice = garment.getPrice() != null ? garment.getPrice() : BigDecimal.ZERO;

        BigDecimal serviceBasePrice = BigDecimal.ZERO;
        if (garment.getService() != null && garment.getService().getPrice() != null) {
            serviceBasePrice = garment.getService().getPrice();
        }

        BigDecimal addOnsTotal = BigDecimal.ZERO;
        int addOnsCount = 0;
        if (garment.getAddOns() != null) {
            for (GarmentAddOn addOn : garment.getAddOns()) {
                addOnsCount++;
                if (addOn.getAddOn() != null && addOn.getAddOn().getPrice() != null) {
                    addOnsTotal = addOnsTotal.add(addOn.getAddOn().getPrice());
                }
            }
        }

        return calculateBreakdown(garmentPrice, serviceBasePrice, addOnsTotal, addOnsCount, garment.isCustomerFabric());
    }

    /**
     * Pure calculation of garment breakdown given primitive / DTO values.
     */
    public static GarmentBreakdown calculateBreakdown(
            BigDecimal garmentPrice,
            BigDecimal serviceBasePrice,
            BigDecimal addOnsTotal,
            int addOnsCount,
            Boolean explicitCustomerFabric
    ) {
        BigDecimal gPrice = garmentPrice != null ? garmentPrice : BigDecimal.ZERO;
        BigDecimal sPrice = serviceBasePrice != null ? serviceBasePrice : BigDecimal.ZERO;
        BigDecimal aTotal = addOnsTotal != null ? addOnsTotal : BigDecimal.ZERO;

        // Check if shop fabric was charged:
        // 1. Price strictly exceeds service base price (e.g. Coat ₹3,500 > ₹2,750)
        // 2. OR user explicitly specified that it is shop fabric (explicitCustomerFabric == false)
        boolean hasExcessPrice = sPrice.compareTo(BigDecimal.ZERO) > 0 && gPrice.compareTo(sPrice) > 0;
        boolean explicitlyShopFabric = (explicitCustomerFabric != null && !explicitCustomerFabric);

        boolean hasShopFabric = hasExcessPrice || explicitlyShopFabric;
        BigDecimal stitchingAmount;
        BigDecimal fabricAmount;

        if (hasExcessPrice) {
            // Service base price is the stitching labor, excess is fabric!
            stitchingAmount = sPrice.add(aTotal);
            fabricAmount = gPrice.subtract(sPrice);
        } else if (explicitlyShopFabric) {
            // Explicitly shop fabric, but price was not set higher than service price
            stitchingAmount = gPrice.add(aTotal);
            fabricAmount = BigDecimal.ZERO;
        } else {
            // Customer cloth (stitching only)
            stitchingAmount = gPrice.add(aTotal);
            fabricAmount = BigDecimal.ZERO;
        }

        return GarmentBreakdown.builder()
                .garmentPrice(gPrice)
                .serviceBasePrice(sPrice)
                .addOnsTotal(aTotal)
                .addOnsCount(addOnsCount)
                .stitchingAmount(stitchingAmount)
                .fabricAmount(fabricAmount)
                .hasShopFabric(hasShopFabric)
                .isCustomerFabric(!hasShopFabric)
                .build();
    }

    /**
     * Analyzes an Order entity and classifies it according to garment composition.
     */
    public static OrderBreakdown analyzeOrder(Order order) {
        if (order == null) {
            return OrderBreakdown.builder()
                    .totalGarments(0)
                    .shopFabricGarmentsCount(0)
                    .customerFabricGarmentsCount(0)
                    .totalStitchingAmount(BigDecimal.ZERO)
                    .totalFabricAmount(BigDecimal.ZERO)
                    .classification("SERVICE_ONLY")
                    .fabricSummary("N/A")
                    .build();
        }

        List<Garment> garments = order.getGarments() != null ? order.getGarments() : Collections.emptyList();
        int totalGarments = garments.size();
        int shopFabricCount = 0;
        int customerFabricCount = 0;
        BigDecimal totalStitching = BigDecimal.ZERO;
        BigDecimal totalFabric = BigDecimal.ZERO;

        for (Garment g : garments) {
            GarmentBreakdown gb = analyzeGarment(g);
            totalStitching = totalStitching.add(gb.getStitchingAmount());
            totalFabric = totalFabric.add(gb.getFabricAmount());
            if (gb.isHasShopFabric()) {
                shopFabricCount++;
            } else {
                customerFabricCount++;
            }
        }

        // Additional charges on order (e.g. express delivery, alteration) count towards stitching/services
        if (order.getAdditionalCharges() != null) {
            for (AdditionalCharge ac : order.getAdditionalCharges()) {
                if (ac.getAmount() != null) {
                    totalStitching = totalStitching.add(ac.getAmount());
                }
            }
        }

        String classification = deriveClassification(totalGarments, shopFabricCount, customerFabricCount);
        String fabricSummary = buildFabricSummary(totalGarments, shopFabricCount, customerFabricCount);

        return OrderBreakdown.builder()
                .totalGarments(totalGarments)
                .shopFabricGarmentsCount(shopFabricCount)
                .customerFabricGarmentsCount(customerFabricCount)
                .totalStitchingAmount(totalStitching)
                .totalFabricAmount(totalFabric)
                .classification(classification)
                .fabricSummary(fabricSummary)
                .build();
    }

    /**
     * Derives order classification from counts.
     */
    public static String deriveClassification(int totalGarments, int shopFabricCount, int customerFabricCount) {
        if (totalGarments == 0) {
            return "SERVICE_ONLY";
        }
        if (shopFabricCount == 0) {
            return "STITCHING_ONLY";
        }
        if (customerFabricCount == 0) {
            return "FABRIC_AND_STITCHING";
        }
        return "MIXED";
    }

    /**
     * Builds a human-readable fabric summary string.
     */
    public static String buildFabricSummary(int totalGarments, int shopFabricCount, int customerFabricCount) {
        if (totalGarments == 0) {
            return "N/A";
        }
        if (customerFabricCount == totalGarments) {
            return "Customer Cloth (" + customerFabricCount + ")";
        }
        if (shopFabricCount == totalGarments) {
            return "Shop Fabric (" + shopFabricCount + ")";
        }
        return "Mixed (" + customerFabricCount + " Cust, " + shopFabricCount + " Shop)";
    }
}
