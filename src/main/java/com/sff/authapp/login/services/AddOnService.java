package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.addon.AddOnRequestDto;
import com.sff.authapp.login.dtos.addon.AddOnResponse;
import com.sff.authapp.login.dtos.addon.AddOnUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddOnService {

    // ============================================================
    // CREATE
    // ============================================================

    AddOnResponse createAddOn(
            AddOnRequestDto request
    );


    // ============================================================
    // GET
    // ============================================================

    AddOnResponse getAddOnById(
            Long addOnId
    );


    // ============================================================
    // GET ALL
    // ============================================================

    Page<AddOnResponse> getAllAddOns(
            Pageable pageable
    );


    // ============================================================
    // SEARCH
    // ============================================================

    Page<AddOnResponse> searchAddOns(
            String keyword,
            Pageable pageable
    );




    // ============================================================
    // ACTIVE
    // ============================================================

    Page<AddOnResponse> getActiveAddOns(
            Pageable pageable
    );


    // ============================================================
    // INACTIVE
    // ============================================================

    Page<AddOnResponse> getInactiveAddOns(
            Pageable pageable
    );







    // ============================================================
    // UPDATE
    // ============================================================

    AddOnResponse updateAddOn(
            Long addOnId,
            AddOnUpdateRequestDto request
    );


    // ============================================================
    // STATUS
    // ============================================================

    AddOnResponse updateAddOnStatus(
            Long addOnId,
            boolean active
    );


    // ============================================================
    // DELETE
    // ============================================================

    void deleteAddOn(
            Long addOnId
    );
}