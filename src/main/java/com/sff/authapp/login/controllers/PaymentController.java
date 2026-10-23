package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.payment.*;
import com.sff.authapp.login.entities.PaymentMethod;
import com.sff.authapp.login.entities.PaymentType;
import com.sff.authapp.login.services.AdditionalChargeService;
import com.sff.authapp.login.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(
        name = "Payment Management",
        description = """
                APIs for managing order payments,
                payment history, payment summaries,
                and additional order charges.
                """
)
public class PaymentController {


    private final PaymentService paymentService;

    private final AdditionalChargeService
            additionalChargeService;


    // ============================================================
    // GET ALL PAYMENTS (WITH FILTERS & PAGINATION)
    // ============================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get payment transactions",
            description = "Returns paginated payments with optional date range, method, type, and keyword filters."
    )
    public ResponseEntity<Page<PaymentTransactionResponse>> getPayments(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) PaymentType type,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "paidAt") Pageable pageable
    ) {
        Page<PaymentTransactionResponse> page = paymentService.getPaymentTransactions(
                startDate,
                endDate,
                method,
                type,
                keyword,
                pageable
        );
//        System.out.println("Retrieved " + page.getNumberOfElements() + " payments out of " + page.getTotalElements() + " total payments.");

        return ResponseEntity.ok(page);
    }


    // ============================================================
    // GET PAYMENT ANALYTICS
    // ============================================================

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get payment analytics",
            description = "Returns today's collections, date range collections, and method breakdown."
    )
    public ResponseEntity<PaymentAnalyticsResponse> getPaymentAnalytics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) PaymentType type,
            @RequestParam(required = false) String keyword
    ) {

        PaymentAnalyticsResponse response = paymentService.getPaymentAnalytics(
                startDate,
                endDate,
                method,
                type,
                keyword
        );
        System.out.println("Payment Analytics: " + response.toString());
        return ResponseEntity.ok(
                response
        );
    }


    // ============================================================
    // GET DELIVERED UNPAID ORDERS
    // ============================================================

    @GetMapping("/delivered-unpaid")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get delivered orders with pending balance",
            description = "Returns orders that have delivered garments but still have an outstanding balance due."
    )
    public ResponseEntity<Page<DeliveredUnpaidOrderResponse>> getDeliveredUnpaidOrders(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                paymentService.getDeliveredUnpaidOrders(keyword, pageable)
        );
    }


    // ============================================================
    // CREATE PAYMENT
    // ============================================================

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    @Operation(
            summary = "Add payment",
            description = """
                    Records a payment against an order.

                    Payments may be recorded on the order
                    confirmation date or on any later date.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment recorded successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payment or amount exceeds balance"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found"
            )
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid
            @RequestBody
            PaymentRequestDto request
    ) {

        return ResponseEntity.ok(
                paymentService.createPayment(request)
        );
    }


    // ============================================================
    // GET PAYMENT
    // ============================================================

    @GetMapping("/{paymentId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long paymentId
    ) {

        return ResponseEntity.ok(
                paymentService.getPaymentById(paymentId)
        );
    }


    // ============================================================
    // GET ORDER PAYMENTS
    // ============================================================

    @GetMapping("/order/{orderId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<List<PaymentResponse>>
    getOrderPayments(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                paymentService.getOrderPayments(orderId)
        );
    }


    // ============================================================
    // GET PAYMENT SUMMARY
    // ============================================================

    @GetMapping("/order/{orderId}/summary")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<PaymentSummaryResponse>
    getPaymentSummary(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                paymentService.getOrderPaymentSummary(
                        orderId
                )
        );
    }


    // ============================================================
    // DELETE PAYMENT
    // ============================================================

    @DeleteMapping("/{paymentId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN')"
    )
    public ResponseEntity<Void> deletePayment(
            @PathVariable Long paymentId
    ) {

        paymentService.deletePayment(paymentId);

        return ResponseEntity.noContent().build();
    }


    // ============================================================
    // ADD ADDITIONAL CHARGE
    // ============================================================

    @PostMapping("/order/{orderId}/charges")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    @Operation(
            summary = "Add additional charge"
    )
    public ResponseEntity<AdditionalChargeResponse>
    addCharge(
            @PathVariable Long orderId,

            @Valid
            @RequestBody
            AdditionalChargeRequestDto request
    ) {

        return ResponseEntity.ok(
                additionalChargeService.addCharge(
                        orderId,
                        request
                )
        );
    }


    // ============================================================
    // GET ADDITIONAL CHARGES
    // ============================================================

    @GetMapping("/order/{orderId}/charges")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<List<AdditionalChargeResponse>>
    getCharges(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                additionalChargeService.getOrderCharges(
                        orderId
                )
        );
    }


    // ============================================================
    // GET CHARGE
    // ============================================================

    @GetMapping("/charges/{chargeId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<AdditionalChargeResponse>
    getCharge(
            @PathVariable Long chargeId
    ) {

        return ResponseEntity.ok(
                additionalChargeService.getChargeById(
                        chargeId
                )
        );
    }


    // ============================================================
    // UPDATE CHARGE
    // ============================================================

    @PatchMapping("/charges/{chargeId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<AdditionalChargeResponse>
    updateCharge(
            @PathVariable Long chargeId,

            @Valid
            @RequestBody
            AdditionalChargeRequestDto request
    ) {

        return ResponseEntity.ok(
                additionalChargeService.updateCharge(
                        chargeId,
                        request
                )
        );
    }


    // ============================================================
    // DELETE CHARGE
    // ============================================================

    @DeleteMapping("/charges/{chargeId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN')"
    )
    public ResponseEntity<Void> deleteCharge(
            @PathVariable Long chargeId
    ) {

        additionalChargeService.deleteCharge(
                chargeId
        );

        return ResponseEntity.noContent().build();
    }
}