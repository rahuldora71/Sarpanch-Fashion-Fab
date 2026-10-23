package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.billing.BillingDtos.InvoiceResponseDto;
import com.sff.authapp.login.dtos.order.PublicOrderTrackingDtos.*;

public interface PublicOrderTrackingService {

    /**
     * Retrieves public customer-facing tracking details for an order
     * by its secure tracking token or order number.
     */
    PublicOrderTrackingResponse trackOrder(String token);

    /**
     * Submits a customer review for an order.
     * Enforces the one-review-per-order rule at service and database levels.
     */
    PublicOrderReviewDto submitReview(String token, PublicOrderReviewRequest request);

    /**
     * Retrieves the public invoice/bill representation for the order.
     */
    InvoiceResponseDto getPublicInvoice(String token);
}
