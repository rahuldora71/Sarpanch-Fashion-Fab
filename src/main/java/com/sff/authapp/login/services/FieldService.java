package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.field.FieldRequestDto;
import com.sff.authapp.login.dtos.field.FieldResponse;
import com.sff.authapp.login.dtos.field.FieldUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FieldService {

    // ============================================================
    // CREATE
    // ============================================================

    FieldResponse createField(
            FieldRequestDto request
    );


    // ============================================================
    // GET BY ID
    // ============================================================

    FieldResponse getFieldById(
            Long fieldId
    );


    // ============================================================
    // GET ALL
    // ============================================================

    Page<FieldResponse> getAllFields(
            Pageable pageable
    );


    // ============================================================
    // SEARCH
    // ============================================================

    Page<FieldResponse> searchFields(
            String keyword,
            Pageable pageable
    );


    // ============================================================
    // UPDATE
    // ============================================================

    FieldResponse updateField(
            Long fieldId,
            FieldUpdateRequestDto request
    );


    // ============================================================
    // DELETE
    // ============================================================

    void deleteField(
            Long fieldId
    );
}