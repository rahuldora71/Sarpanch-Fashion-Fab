package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.fabric.FabricSampleCreateRequest;
import com.sff.authapp.login.dtos.fabric.FabricSampleResponse;
import com.sff.authapp.login.entities.FabricSample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FabricSampleService {

    // ============================================================
    // CREATE
    // ============================================================

    FabricSampleResponse createFabricSample(
            FabricSampleCreateRequest request
    );


    // ============================================================
    // GET BY ID
    // ============================================================

    FabricSampleResponse getFabricSampleById(
            Long fabricSampleId
    );


    // ============================================================
    // GET ALL
    // ============================================================

    Page<FabricSampleResponse> getAllFabricSamples(
            Pageable pageable
    );


    // ============================================================
    // SEARCH
    // ============================================================

    Page<FabricSampleResponse> searchFabricSamples(
            String keyword,
            Pageable pageable
    );


    // ============================================================
    // FILTER BY STATUS
    // ============================================================

    Page<FabricSampleResponse> getFabricSamplesByStatus(
            FabricSample.Status status,
            Pageable pageable
    );


    // ============================================================
    // CUSTOMER
    // ============================================================

    Page<FabricSampleResponse> getCustomerFabricSamples(
            Long customerId,
            Pageable pageable
    );


    // ============================================================
    // ORDER
    // ============================================================

    Page<FabricSampleResponse> getOrderFabricSamples(
            Long orderId,
            Pageable pageable
    );


    // ============================================================
    // UPDATE
    // ============================================================

    FabricSampleResponse updateFabricSample(
            Long fabricSampleId,
            FabricSampleCreateRequest request
    );


    // ============================================================
    // DELETE
    // ============================================================

    void deleteFabricSample(
            Long fabricSampleId
    );
}