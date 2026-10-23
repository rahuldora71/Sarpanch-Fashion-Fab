package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.cutting.CuttingQueueResponse;
import com.sff.authapp.login.dtos.cutting.CuttingWorkspaceResponse;
import com.sff.authapp.login.dtos.cutting.MarkAsCutRequest;
import com.sff.authapp.login.dtos.garment.GarmentResponse;
import com.sff.authapp.login.entities.GarmentStatus;

import java.time.LocalDate;

public interface CuttingService {

    /**
     * Get the active cutting queue.
     * Returns all garments with FABRIC_CUT status (waiting to be cut),
     * grouped by delivery date, and then by order.
     *
     * In this domain model, FABRIC_CUT means it's ready/waiting to be cut.
     * Once it's cut, it moves to CUTTING or IN_STITCHING depending on the model mapped.
     */
    CuttingQueueResponse getCuttingQueue();

    /**
     * Get the cutting queue restricted to a specific date range.
     */
    CuttingQueueResponse getCuttingQueueByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get the completely loaded cutting workspace for an order.
     * Includes all garments of the order, highlighting their cutting instructions.
     */
    CuttingWorkspaceResponse getCuttingWorkspace(Long orderId);

    /**
     * Mark a garment as cut.
     * Transitions status from FABRIC_CUT to CUTTING/STITCHING.
     * Sets the cutting worker to the currently authenticated user (or specified in request).
     */
    GarmentResponse markAsCut(Long garmentId, MarkAsCutRequest request, Long authenticatedUserId);

    /**
     * Utility method: Reverts a cut garment back to the queue (if allowed).
     */
    GarmentResponse cancelCut(Long garmentId, Long authenticatedUserId);
}
