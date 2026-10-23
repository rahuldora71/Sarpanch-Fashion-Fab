package com.sff.authapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAppApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.sff.authapp.login.services.PaymentService paymentService;

    @Test
    void testGetPaymentAnalytics() {
        var analytics = paymentService.getPaymentAnalytics(java.time.LocalDate.now(), java.time.LocalDate.now());
        System.out.println("ANALYTICS RESULT: " + analytics);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "OWNER")
    void testGetPaymentAnalyticsEndpoint() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/payments/analytics")
                        .param("startDate", "2026-10-07")
                        .param("endDate", "2026-10-07"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.todayTotal").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.todayTotalAmount").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.methodBreakdown").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.deliveredUnpaidCount").exists());
    }

    @Test
    void contextLoads() {
    }

    @Test
    void preflightRequestIncludesCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type, authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

}
