package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.field.FieldRequestDto;
import com.sff.authapp.login.dtos.field.FieldResponse;
import com.sff.authapp.login.dtos.field.FieldUpdateRequestDto;
import com.sff.authapp.login.entities.Field;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.FieldRepository;
import com.sff.authapp.login.services.FieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FieldServiceImpl implements FieldService {


    private final FieldRepository fieldRepository;


    // ============================================================
    // CREATE
    // ============================================================

    @Override
    public FieldResponse createField(
            FieldRequestDto request
    ) {

        String name = normalize(request.name());


        if (name == null) {

            throw new IllegalArgumentException(
                    "Field name is required"
            );
        }


        // --------------------------------------------------------
        // CHECK DUPLICATE
        // --------------------------------------------------------

        if (fieldRepository.existsByNameIgnoreCase(name)) {

            throw new IllegalArgumentException(
                    "A measurement field with this name already exists"
            );
        }


        // --------------------------------------------------------
        // CREATE ENTITY
        // --------------------------------------------------------

        Field field = Field.builder()
                .name(name)
                .build();


        // --------------------------------------------------------
        // SAVE
        // --------------------------------------------------------

        Field savedField =
                fieldRepository.save(field);


        return toResponse(savedField);
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public FieldResponse getFieldById(
            Long fieldId
    ) {

        Field field =
                findById(fieldId);

        return toResponse(field);
    }


    // ============================================================
    // GET ALL
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<FieldResponse> getAllFields(
            Pageable pageable
    ) {

        return fieldRepository
                .findAll(pageable)
                .map(this::toResponse);
    }


    // ============================================================
    // SEARCH
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<FieldResponse> searchFields(
            String keyword,
            Pageable pageable
    ) {

        if (keyword == null || keyword.isBlank()) {

            return getAllFields(pageable);
        }


        String search =
                keyword.trim();


        return fieldRepository
                .findByNameContainingIgnoreCase(
                        search,
                        pageable
                )
                .map(this::toResponse);
    }


    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public FieldResponse updateField(
            Long fieldId,
            FieldUpdateRequestDto request
    ) {

        Field existingField =
                findById(fieldId);


        // --------------------------------------------------------
        // NAME
        // --------------------------------------------------------

        if (request.name() != null) {

            String name =
                    normalize(request.name());


            if (name == null) {

                throw new IllegalArgumentException(
                        "Field name cannot be empty"
                );
            }


            // ----------------------------------------------------
            // CHECK IF NAME CHANGED
            // ----------------------------------------------------

            if (!name.equalsIgnoreCase(
                    existingField.getName()
            )) {

                if (fieldRepository
                        .existsByNameIgnoreCase(name)) {

                    throw new IllegalArgumentException(
                            "A measurement field with this name already exists"
                    );
                }
            }


            existingField.setName(name);
        }


        // --------------------------------------------------------
        // SAVE
        // --------------------------------------------------------

        Field updatedField =
                fieldRepository.save(
                        existingField
                );


        return toResponse(updatedField);
    }


    // ============================================================
    // DELETE
    // ============================================================

    @Override
    public void deleteField(
            Long fieldId
    ) {

        Field field =
                findById(fieldId);


        fieldRepository.delete(field);
    }


    // ============================================================
    // FIND BY ID
    // ============================================================

    private Field findById(
            Long fieldId
    ) {

        return fieldRepository
                .findById(fieldId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Measurement field not found with id: "
                                        + fieldId
                        )
                );
    }


    // ============================================================
    // NORMALIZE
    // ============================================================

    private String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }


        String trimmed =
                value.trim();


        return trimmed.isBlank()
                ? null
                : trimmed;
    }


    // ============================================================
    // RESPONSE MAPPING
    // ============================================================

    private FieldResponse toResponse(
            Field field
    ) {

        return new FieldResponse(
                field.getId(),
                field.getName()
        );
    }
}