package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.billing.BillingDtos.InvoiceResponseDto;
import com.sff.authapp.login.dtos.order.PublicOrderTrackingDtos.*;
import com.sff.authapp.login.services.PublicOrderTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Public Customer Order Tracking",
        description = "Customer-facing APIs accessed via WhatsApp/SMS links for tracking order status, invoice access, and review submission"
)
public class PublicOrderTrackingController {

    private final PublicOrderTrackingService trackingService;

    @GetMapping("/track/{token}")
    @Operation(
            summary = "Track Order (Public)",
            description = "Fetches customer-safe order progress, live production stages, timeline, and financials using secure tracking token or order number."
    )
    public ResponseEntity<PublicOrderTrackingResponse> trackOrder(
            @PathVariable("token") String token
    ) {
        log.info("Public order tracking requested for token/ref: {}", token);
        return ResponseEntity.ok(trackingService.trackOrder(token));
    }

    @PostMapping("/track/{token}/review")
    @Operation(
            summary = "Submit Order Review (Public)",
            description = "Allows customer to submit a star rating (1-5) and feedback for their order. Enforces one review per order."
    )
    public ResponseEntity<PublicOrderReviewDto> submitReview(
            @PathVariable("token") String token,
            @Valid @RequestBody PublicOrderReviewRequest request
    ) {
        log.info("Customer review submitted for token/ref: {}", token);
        PublicOrderReviewDto createdReview = trackingService.submitReview(token, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
    }

    @GetMapping("/track/{token}/invoice")
    @Operation(
            summary = "Get Customer Invoice / Bill (Public)",
            description = "Provides public invoice details and itemization so customers can view or download their bill as PDF."
    )
    public ResponseEntity<InvoiceResponseDto> getPublicInvoice(
            @PathVariable("token") String token
    ) {
        return ResponseEntity.ok(trackingService.getPublicInvoice(token));
    }
}
