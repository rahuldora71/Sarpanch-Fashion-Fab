package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.addon.AddOnRequestDto;
import com.sff.authapp.login.dtos.addon.AddOnResponse;
import com.sff.authapp.login.dtos.addon.AddOnUpdateRequestDto;
import com.sff.authapp.login.entities.AddOn;
import com.sff.authapp.login.entities.TailoringService;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.AddOnRepository;
import com.sff.authapp.login.repositories.TailoringServiceRepository;
import com.sff.authapp.login.services.AddOnService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddOnServiceImpl implements AddOnService {

    private final AddOnRepository addOnRepository;

    private final TailoringServiceRepository
            tailoringServiceRepository;


    // ============================================================
    // CREATE
    // ============================================================

    @Override
    public AddOnResponse createAddOn(
            AddOnRequestDto request
    ) {

        String name = normalize(request.name());

        if (addOnRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "Add-on already exists with name: " + name
            );
        }

        AddOn addOn = AddOn.builder()
                .name(name)
                .price(request.price())
                .tailorCommission(request.tailorCommission())
                .imageUrl(normalize(request.imageUrl()))
                .imagePublicId(normalize(request.imagePublicId()))
                .beforeCutting(request.beforeCutting())
                .active(request.active())
                .build();

        AddOn savedAddOn =
                addOnRepository.save(addOn);

        return toResponse(savedAddOn);
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public AddOnResponse getAddOnById(
            Long addOnId
    ) {

        return toResponse(
                findAddOnById(addOnId)
        );
    }


    // ============================================================
    // GET ALL
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AddOnResponse> getAllAddOns(
            Pageable pageable
    ) {

        return addOnRepository
                .findAll(pageable)
                .map(this::toResponse);
    }


    // ============================================================
    // SEARCH
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AddOnResponse> searchAddOns(
            String keyword,
            Pageable pageable
    ) {

        String normalizedKeyword =
                normalize(keyword);

        if (normalizedKeyword == null) {
            return getAllAddOns(pageable);
        }

        return addOnRepository
                .findByNameContainingIgnoreCase(
                        normalizedKeyword,
                        pageable
                )
                .map(this::toResponse);
    }





    // ============================================================
    // ACTIVE ADD-ONS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AddOnResponse> getActiveAddOns(
            Pageable pageable
    ) {

        return addOnRepository
                .findByActiveTrue(pageable)
                .map(this::toResponse);
    }


    // ============================================================
    // INACTIVE ADD-ONS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AddOnResponse> getInactiveAddOns(
            Pageable pageable
    ) {

        return addOnRepository
                .findByActiveFalse(pageable)
                .map(this::toResponse);
    }






    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public AddOnResponse updateAddOn(
            Long addOnId,
            AddOnUpdateRequestDto request
    ) {

        AddOn addOn =
                findAddOnById(addOnId);


        // --------------------------------------------------------
        // NAME
        // --------------------------------------------------------

        if (request.name() != null) {

            String name =
                    normalize(request.name());

            if (!name.equalsIgnoreCase(
                    addOn.getName()
            )
                    && addOnRepository
                    .existsByNameIgnoreCase(name)) {

                throw new IllegalArgumentException(
                        "Add-on already exists with name: "
                                + name
                );
            }

            addOn.setName(name);
        }


        // --------------------------------------------------------
        // PRICE
        // --------------------------------------------------------

        if (request.price() != null) {

            addOn.setPrice(
                    request.price()
            );
        }


        // --------------------------------------------------------
// IMAGE
// --------------------------------------------------------

        if (request.imageUrl() != null) {

            addOn.setImageUrl(
                    normalize(request.imageUrl())
            );
        }

        if (request.imagePublicId() != null) {

            addOn.setImagePublicId(
                    normalize(request.imagePublicId())
            );
        }

        if (request.tailorCommission() != null) {

            addOn.setTailorCommission(
                    request.tailorCommission()
            );
        }

        // --------------------------------------------------------
        // BEFORE CUTTING
        // --------------------------------------------------------

        if (request.beforeCutting() != null) {

            addOn.setBeforeCutting(
                    request.beforeCutting()
            );
        }


        // --------------------------------------------------------
        // ACTIVE
        // --------------------------------------------------------

        if (request.active() != null) {

            addOn.setActive(
                    request.active()
            );
        }


        AddOn updatedAddOn =
                addOnRepository.save(addOn);

        return toResponse(updatedAddOn);
    }


    // ============================================================
    // UPDATE STATUS
    // ============================================================

    @Override
    public AddOnResponse updateAddOnStatus(
            Long addOnId,
            boolean active
    ) {

        AddOn addOn =
                findAddOnById(addOnId);

        addOn.setActive(active);

        AddOn updatedAddOn =
                addOnRepository.save(addOn);

        return toResponse(updatedAddOn);
    }


    // ============================================================
    // DELETE
    // ============================================================

    @Override
    public void deleteAddOn(
            Long addOnId
    ) {

        AddOn addOn =
                findAddOnById(addOnId);

        /*
         * Soft delete.
         *
         * We keep the database record because an old order
         * may still reference this add-on.
         */
        addOn.setActive(false);

        addOnRepository.save(addOn);
    }


    // ============================================================
    // FIND ADD-ON
    // ============================================================

    private AddOn findAddOnById(
            Long addOnId
    ) {

        return addOnRepository
                .findById(addOnId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Add-on not found with id: "
                                        + addOnId
                        )
                );
    }


    // ============================================================
    // FIND SERVICE
    // ============================================================

    private TailoringService findServiceById(
            Long serviceId
    ) {

        return tailoringServiceRepository
                .findById(serviceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tailoring service not found with id: "
                                        + serviceId
                        )
                );
    }


    // ============================================================
    // MAPPER
    // ============================================================

    private AddOnResponse toResponse(
            AddOn addOn
    ) {



        return new AddOnResponse(
                addOn.getId(),
                addOn.getName(),
                addOn.getPrice(),
                addOn.getTailorCommission(),
                addOn.getImageUrl(),
                addOn.getImagePublicId(),
                addOn.isBeforeCutting(),
                addOn.isActive(),
                addOn.getCreatedAt(),
                addOn.getUpdatedAt()
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

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}