//package com.sff.authapp.login.dtos.garment;
//
//import com.sff.authapp.login.entities.Garment;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//
//public record GarmentStatusUpdateRequest(
//
//        @NotNull(message = "Status is required")
//        Garment.Status status,
//
//        @Size(
//                max = 1000,
//                message = "Notes cannot exceed 1000 characters"
//        )
//        String notes
//) {
//}