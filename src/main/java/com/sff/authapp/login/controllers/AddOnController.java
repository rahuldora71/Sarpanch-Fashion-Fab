package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.addon.AddOnRequestDto;
import com.sff.authapp.login.dtos.addon.AddOnResponse;
import com.sff.authapp.login.dtos.addon.AddOnUpdateRequestDto;
import com.sff.authapp.login.services.AddOnService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/addons")
@RequiredArgsConstructor
public class AddOnController {

    private final AddOnService addOnService;


    // ============================================================
    // CREATE
    // ============================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<AddOnResponse> createAddOn(
            @Valid @RequestBody AddOnRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        addOnService.createAddOn(request)
                );
    }


    // ============================================================
    // GET ALL
    // ============================================================

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<Page<AddOnResponse>> getAllAddOns(
            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                addOnService.getAllAddOns(pageable)
        );
    }


    // ============================================================
    // SEARCH
    // ============================================================

    @GetMapping("/search")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<Page<AddOnResponse>> searchAddOns(
            @RequestParam String keyword,
            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                addOnService.searchAddOns(
                        keyword,
                        pageable
                )
        );
    }


    // ============================================================
    // ACTIVE
    // ============================================================

    @GetMapping("/active")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<Page<AddOnResponse>> getActiveAddOns(
            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                addOnService.getActiveAddOns(
                        pageable
                )
        );
    }


    // ============================================================
    // INACTIVE
    // ============================================================

    @GetMapping("/inactive")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Page<AddOnResponse>> getInactiveAddOns(
            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                addOnService.getInactiveAddOns(
                        pageable
                )
        );
    }







    // ============================================================
    // GET BY ID
    // ============================================================

    @GetMapping("/{addOnId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<AddOnResponse> getAddOnById(
            @PathVariable Long addOnId
    ) {

        return ResponseEntity.ok(
                addOnService.getAddOnById(addOnId)
        );
    }


    // ============================================================
    // UPDATE
    // ============================================================

    @PatchMapping("/{addOnId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN')"
    )
    public ResponseEntity<AddOnResponse> updateAddOn(
            @PathVariable Long addOnId,

            @Valid
            @RequestBody AddOnUpdateRequestDto request
    ) {

        return ResponseEntity.ok(
                addOnService.updateAddOn(
                        addOnId,
                        request
                )
        );
    }


    // ============================================================
    // UPDATE STATUS
    // ============================================================

    @PatchMapping("/{addOnId}/status")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN')"
    )
    public ResponseEntity<AddOnResponse> updateAddOnStatus(
            @PathVariable Long addOnId,

            @RequestParam boolean active
    ) {

        return ResponseEntity.ok(
                addOnService.updateAddOnStatus(
                        addOnId,
                        active
                )
        );
    }


    // ============================================================
    // DELETE
    // ============================================================

    @DeleteMapping("/{addOnId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN')"
    )
    public ResponseEntity<Void> deleteAddOn(
            @PathVariable Long addOnId
    ) {

        addOnService.deleteAddOn(addOnId);

        return ResponseEntity.noContent().build();
    }
}