//package com.sff.authapp.login.controllers;
//
//import com.sff.authapp.login.dtos.fabric.FabricSampleCreateRequest;
//import com.sff.authapp.login.dtos.fabric.FabricSampleResponse;
//import com.sff.authapp.login.entities.FabricSample;
//import com.sff.authapp.login.services.FabricSampleService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.web.PageableDefault;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/fabric-samples")
//@RequiredArgsConstructor
//public class FabricSampleController {
//
//    private final FabricSampleService fabricSampleService;
//
//
//    // ============================================================
//    // CREATE
//    // ============================================================
//
//    @PostMapping
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    public ResponseEntity<FabricSampleResponse> createFabricSample(
//            @Valid @RequestBody FabricSampleCreateRequest request
//    ) {
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(
//                        fabricSampleService.createFabricSample(request)
//                );
//    }
//
//
//    // ============================================================
//    // GET ALL
//    // ============================================================
//
//    @GetMapping
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    public ResponseEntity<Page<FabricSampleResponse>> getAllFabricSamples(
//
//            @PageableDefault(
//                    size = 20,
//                    sort = "sampleCode",
//                    direction = Sort.Direction.ASC
//            )
//            Pageable pageable
//
//    ) {
//
//        return ResponseEntity.ok(
//                fabricSampleService.getAllFabricSamples(pageable)
//        );
//    }
//
//
//    // ============================================================
//    // SEARCH
//    // ============================================================
//
//    @GetMapping("/search")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    public ResponseEntity<Page<FabricSampleResponse>> searchFabricSamples(
//
//            @RequestParam String keyword,
//
//            @PageableDefault(
//                    size = 20,
//                    sort = "sampleCode",
//                    direction = Sort.Direction.ASC
//            )
//            Pageable pageable
//
//    ) {
//
//        return ResponseEntity.ok(
//                fabricSampleService.searchFabricSamples(
//                        keyword,
//                        pageable
//                )
//        );
//    }
//
//
//    // ============================================================
//    // FILTER BY STATUS
//    // ============================================================
//
//    @GetMapping("/status/{status}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    public ResponseEntity<Page<FabricSampleResponse>>
//    getFabricSamplesByStatus(
//
//            @PathVariable FabricSample.Status status,
//
//            @PageableDefault(
//                    size = 20,
//                    sort = "sampleCode",
//                    direction = Sort.Direction.ASC
//            )
//            Pageable pageable
//
//    ) {
//
//        return ResponseEntity.ok(
//                fabricSampleService.getFabricSamplesByStatus(
//                        status,
//                        pageable
//                )
//        );
//    }
//
//
//    // ============================================================
//    // GET BY CUSTOMER
//    // ============================================================
//
//    @GetMapping("/customer/{customerId}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    public ResponseEntity<Page<FabricSampleResponse>>
//    getCustomerFabricSamples(
//
//            @PathVariable Long customerId,
//
//            @PageableDefault(
//                    size = 20,
//                    sort = "createdAt",
//                    direction = Sort.Direction.DESC
//            )
//            Pageable pageable
//
//    ) {
//
//        return ResponseEntity.ok(
//                fabricSampleService.getCustomerFabricSamples(
//                        customerId,
//                        pageable
//                )
//        );
//    }
//
//
//    // ============================================================
//    // GET BY ORDER
//    // ============================================================
//
//    @GetMapping("/order/{orderId}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    public ResponseEntity<Page<FabricSampleResponse>>
//    getOrderFabricSamples(
//
//            @PathVariable Long orderId,
//
//            @PageableDefault(
//                    size = 20,
//                    sort = "createdAt",
//                    direction = Sort.Direction.DESC
//            )
//            Pageable pageable
//
//    ) {
//
//        return ResponseEntity.ok(
//                fabricSampleService.getOrderFabricSamples(
//                        orderId,
//                        pageable
//                )
//        );
//    }
//
//
//    // ============================================================
//    // GET BY ID
//    // ============================================================
//
//    @GetMapping("/{fabricSampleId}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
//    public ResponseEntity<FabricSampleResponse>
//    getFabricSampleById(
//
//            @PathVariable Long fabricSampleId
//
//    ) {
//
//        return ResponseEntity.ok(
//                fabricSampleService.getFabricSampleById(
//                        fabricSampleId
//                )
//        );
//    }
//
//
//    // ============================================================
//    // UPDATE
//    // ============================================================
//
//    @PutMapping("/{fabricSampleId}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
//    public ResponseEntity<FabricSampleResponse>
//    updateFabricSample(
//
//            @PathVariable Long fabricSampleId,
//
//            @Valid
//            @RequestBody
//            FabricSampleCreateRequest request
//
//    ) {
//
//        return ResponseEntity.ok(
//                fabricSampleService.updateFabricSample(
//                        fabricSampleId,
//                        request
//                )
//        );
//    }
//
//
//    // ============================================================
//    // DELETE
//    // ============================================================
//
//    @DeleteMapping("/{fabricSampleId}")
//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
//    public ResponseEntity<Void> deleteFabricSample(
//
//            @PathVariable Long fabricSampleId
//
//    ) {
//
//        fabricSampleService.deleteFabricSample(
//                fabricSampleId
//        );
//
//        return ResponseEntity.noContent().build();
//    }
//}