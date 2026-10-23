package com.sff.authapp.login.helpers;

import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.Order;
import com.sff.authapp.login.entities.TailoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GarmentClassificationHelperTest {

    @Test
    @DisplayName("Coat with price 3500 and service price 2750 should identify 750 fabric and 2750 stitching (Shop Fabric)")
    void testCoatFabricDetection() {
        TailoringService coatService = TailoringService.builder()
                .name("coat")
                .price(new BigDecimal("2750.00"))
                .build();

        Garment coatGarment = Garment.builder()
                .service(coatService)
                .price(new BigDecimal("3500.00"))
                .customerFabric(true) // Even if previously defaulted to true
                .build();

        GarmentClassificationHelper.GarmentBreakdown breakdown =
                GarmentClassificationHelper.analyzeGarment(coatGarment);

        assertTrue(breakdown.isHasShopFabric(), "Coat should be recognized as Shop Fabric because price exceeds service price");
        assertFalse(breakdown.isCustomerFabric(), "Coat should NOT be customer fabric");
        assertEquals(new BigDecimal("750.00"), breakdown.getFabricAmount());
        assertEquals(new BigDecimal("2750.00"), breakdown.getStitchingAmount());
        assertEquals(new BigDecimal("3500.00"), breakdown.getGarmentPrice());
    }

    @Test
    @DisplayName("Pant with price 400 and addons 50 should identify 0 fabric and 450 stitching (Customer Cloth)")
    void testPantCustomerClothDetection() {
        GarmentClassificationHelper.GarmentBreakdown breakdown =
                GarmentClassificationHelper.calculateBreakdown(
                        new BigDecimal("400.00"),
                        new BigDecimal("400.00"),
                        new BigDecimal("50.00"),
                        2,
                        true
                );

        assertFalse(breakdown.isHasShopFabric(), "Pant should be recognized as Customer Cloth");
        assertTrue(breakdown.isCustomerFabric());
        assertEquals(BigDecimal.ZERO, breakdown.getFabricAmount());
        assertEquals(new BigDecimal("450.00"), breakdown.getStitchingAmount());
    }

    @Test
    @DisplayName("Formal Shirt with price 350 matching service price 350 should identify Customer Cloth")
    void testFormalShirtCustomerClothDetection() {
        GarmentClassificationHelper.GarmentBreakdown breakdown =
                GarmentClassificationHelper.calculateBreakdown(
                        new BigDecimal("350.00"),
                        new BigDecimal("350.00"),
                        BigDecimal.ZERO,
                        0,
                        true
                );

        assertFalse(breakdown.isHasShopFabric());
        assertTrue(breakdown.isCustomerFabric());
        assertEquals(BigDecimal.ZERO, breakdown.getFabricAmount());
        assertEquals(new BigDecimal("350.00"), breakdown.getStitchingAmount());
    }

    @Test
    @DisplayName("Order 10 with Coat (Shop Fabric), Pant (Customer Cloth) and Shirt (Customer Cloth) should classify as MIXED")
    void testOrder10MixedClassification() {
        TailoringService coatService = TailoringService.builder().name("coat").price(new BigDecimal("2750.00")).build();
        TailoringService pantService = TailoringService.builder().name("Pant").price(new BigDecimal("400.00")).build();
        TailoringService shirtService = TailoringService.builder().name("Formal Shirt").price(new BigDecimal("350.00")).build();

        Garment coat = Garment.builder().service(coatService).price(new BigDecimal("3500.00")).customerFabric(true).build();
        Garment pant = Garment.builder().service(pantService).price(new BigDecimal("400.00")).customerFabric(true).build();
        Garment shirt = Garment.builder().service(shirtService).price(new BigDecimal("350.00")).customerFabric(true).build();

        Order order = Order.builder()
                .garments(List.of(coat, pant, shirt))
                .build();

        GarmentClassificationHelper.OrderBreakdown breakdown =
                GarmentClassificationHelper.analyzeOrder(order);

        assertEquals("MIXED", breakdown.getClassification());
        assertEquals(3, breakdown.getTotalGarments());
        assertEquals(1, breakdown.getShopFabricGarmentsCount());
        assertEquals(2, breakdown.getCustomerFabricGarmentsCount());
        assertEquals(new BigDecimal("750.00"), breakdown.getTotalFabricAmount());
        assertEquals(new BigDecimal("3500.00"), breakdown.getTotalStitchingAmount());
        assertEquals("Mixed (2 Cust, 1 Shop)", breakdown.getFabricSummary());
    }
}
