package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.service.ServiceResponse;
import com.sff.authapp.login.dtos.service.TailoringServiceRequestDto;
import com.sff.authapp.login.dtos.service.TailoringServiceUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TailoringService {

    // ============================================================
    // CREATE
    // ============================================================

    ServiceResponse createService(
            TailoringServiceRequestDto request
    );


    // ============================================================
    // GET BY ID
    // ============================================================

    ServiceResponse getServiceById(
            Long serviceId
    );


    // ============================================================
    // GET ALL
    // ============================================================

    Page<ServiceResponse> getAllServices(
            Pageable pageable
    );


    // ============================================================
    // SEARCH
    // ============================================================

    Page<ServiceResponse> searchServices(
            String keyword,
            Pageable pageable
    );


    // ============================================================
    // ACTIVE SERVICES
    // ============================================================

    Page<ServiceResponse> getActiveServices(
            Pageable pageable
    );


    // ============================================================
    // INACTIVE SERVICES
    // ============================================================

    Page<ServiceResponse> getInactiveServices(
            Pageable pageable
    );


    // ============================================================
    // CATEGORY
    // ============================================================

    Page<ServiceResponse> getServicesByCategory(
            String category,
            Pageable pageable
    );


    // ============================================================
    // UPDATE
    // ============================================================

    ServiceResponse updateService(
            Long serviceId,
            TailoringServiceUpdateRequestDto request
    );


    // ============================================================
    // ACTIVATE / DEACTIVATE
    // ============================================================

    ServiceResponse updateServiceStatus(
            Long serviceId,
            boolean active
    );


    // ============================================================
    // DELETE
    // ============================================================

    void deleteService(
            Long serviceId
    );
}