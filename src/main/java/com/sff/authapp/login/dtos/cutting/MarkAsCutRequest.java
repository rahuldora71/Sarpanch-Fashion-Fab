package com.sff.authapp.login.dtos.cutting;

/**
 * Request DTO to mark a garment as cut.
 * The cutting worker confirms they have cut the garment.
 */
public record MarkAsCutRequest(
        // Optional: note from the cutter (e.g., fabric marking confirmation)
        String cuttingNote,

        // Optional: fabric marking confirmation message
        String fabricMarkingConfirmation,

        // Optional: override who performed the cut.
        // If null, the authenticated user from the security context is used.
        // Backend should prefer the security context — this field is informational.
        Long cuttingByUserId
) {}
