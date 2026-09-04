package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.service.ServiceResponse;
import com.sff.authapp.login.dtos.service.TailoringServiceRequestDto;
import com.sff.authapp.login.dtos.service.TailoringServiceUpdateRequestDto;
import com.sff.authapp.login.services.TailoringService;
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
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class TailoringServiceController {

    private final TailoringService tailoringService;


    // ============================================================
    // CREATE
    // ============================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ServiceResponse> createService(
            @Valid
            @RequestBody
            TailoringServiceRequestDto request
    ) {

        ServiceResponse response =
                tailoringService.createService(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // GET ALL
    // ============================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Page<ServiceResponse>> getAllServices(

            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                tailoringService.getAllServices(pageable)
        );
    }


    // ============================================================
    // SEARCH
    // ============================================================

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Page<ServiceResponse>> searchServices(

            @RequestParam
            String keyword,

            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                tailoringService.searchServices(
                        keyword,
                        pageable
                )
        );
    }


    // ============================================================
    // ACTIVE SERVICES
    // ============================================================

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Page<ServiceResponse>> getActiveServices(

            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                tailoringService.getActiveServices(pageable)
        );
    }


    // ============================================================
    // INACTIVE SERVICES
    // ============================================================

    @GetMapping("/inactive")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Page<ServiceResponse>> getInactiveServices(

            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                tailoringService.getInactiveServices(pageable)
        );
    }


    // ============================================================
    // GET BY CATEGORY
    // ============================================================

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Page<ServiceResponse>> getServicesByCategory(

            @PathVariable
            String category,

            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                tailoringService.getServicesByCategory(
                        category,
                        pageable
                )
        );
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @GetMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<ServiceResponse> getServiceById(

            @PathVariable
            Long serviceId

    ) {

        return ResponseEntity.ok(
                tailoringService.getServiceById(serviceId)
        );
    }


    // ============================================================
    // PARTIAL UPDATE
    // ============================================================

    @PatchMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ServiceResponse> updateService(

            @PathVariable
            Long serviceId,

            @Valid
            @RequestBody
            TailoringServiceUpdateRequestDto request

    ) {

        return ResponseEntity.ok(
                tailoringService.updateService(
                        serviceId,
                        request
                )
        );
    }


    // ============================================================
    // ACTIVATE / DEACTIVATE
    // ============================================================

    @PatchMapping("/{serviceId}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ServiceResponse> updateServiceStatus(

            @PathVariable
            Long serviceId,

            @RequestParam
            boolean active

    ) {

        return ResponseEntity.ok(
                tailoringService.updateServiceStatus(
                        serviceId,
                        active
                )
        );
    }


    // ============================================================
    // DELETE
    // ============================================================

    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteService(

            @PathVariable
            Long serviceId

    ) {

        tailoringService.deleteService(serviceId);

        return ResponseEntity
                .noContent()
                .build();
    }
}