package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.garment.GarmentRequestDto;
import com.sff.authapp.login.dtos.garment.GarmentResponse;
import com.sff.authapp.login.dtos.garment.GarmentUpdateRequestDto;
import com.sff.authapp.login.entities.GarmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GarmentService {

    GarmentResponse createGarment(
            GarmentRequestDto request
    );

    GarmentResponse getGarmentById(
            Long garmentId
    );

    List<GarmentResponse> getGarmentsByOrder(
            Long orderId
    );

    Page<GarmentResponse> getGarmentsByOrder(
            Long orderId,
            Pageable pageable
    );

    Page<GarmentResponse> getGarmentsByStatus(
            GarmentStatus status,
            Pageable pageable
    );

    Page<GarmentResponse> getGarmentsByService(
            Long serviceId,
            Pageable pageable
    );

    Page<GarmentResponse> getCustomerGarments(
            Long customerId,
            Pageable pageable
    );

    GarmentResponse updateGarment(
            Long garmentId,
            GarmentUpdateRequestDto request
    );

    GarmentResponse updateStatus(
            Long garmentId,
            GarmentStatus status
    );

    void deleteGarment(
            Long garmentId
    );
}