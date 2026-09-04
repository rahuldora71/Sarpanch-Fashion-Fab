//package com.sff.authapp.login.services.impl;
//
//import com.sff.authapp.login.dtos.fabric.FabricSampleCreateRequest;
//import com.sff.authapp.login.dtos.fabric.FabricSampleResponse;
//import com.sff.authapp.login.entities.FabricSample;
//import com.sff.authapp.login.entities.Order;
//import com.sff.authapp.login.entities.User;
//import com.sff.authapp.login.exceptions.ResourceNotFoundException;
//import com.sff.authapp.login.repositories.FabricSampleRepository;
//import com.sff.authapp.login.services.FabricSampleService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class FabricSampleServiceImpl
//        implements FabricSampleService {
//
//    private final FabricSampleRepository fabricSampleRepository;
//
//
//    // ============================================================
//    // CREATE
//    // ============================================================
//
//    @Override
//    public FabricSampleResponse createFabricSample(
//            FabricSampleCreateRequest request
//    ) {
//
//        FabricSample fabricSample =
//                FabricSample.builder()
//                        .sampleCode(generateSampleCode())
//                        .bookNumber(request.bookNumber())
//                        .pageNumber(request.pageNumber())
//                        .color(request.color())
//                        .description(request.description())
//                        .status(
//                                request.status() != null
//                                        ? request.status()
//                                        : FabricSample.Status.AVAILABLE
//                        )
//                        .build();
//
//
//        FabricSample savedSample =
//                fabricSampleRepository.save(
//                        fabricSample
//                );
//
//
//        return toResponse(savedSample);
//    }
//
//
//    // ============================================================
//    // GET BY ID
//    // ============================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public FabricSampleResponse getFabricSampleById(
//            Long fabricSampleId
//    ) {
//
//        FabricSample fabricSample =
//                findById(fabricSampleId);
//
//
//        return toResponse(fabricSample);
//    }
//
//
//    // ============================================================
//    // GET ALL
//    // ============================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<FabricSampleResponse> getAllFabricSamples(
//            Pageable pageable
//    ) {
//
//        return fabricSampleRepository
//                .findAll(pageable)
//                .map(this::toResponse);
//    }
//
//
//    // ============================================================
//    // SEARCH
//    // ============================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<FabricSampleResponse> searchFabricSamples(
//            String keyword,
//            Pageable pageable
//    ) {
//
//        if (keyword == null || keyword.isBlank()) {
//
//            return getAllFabricSamples(pageable);
//        }
//
//
//        String search =
//                keyword.trim();
//
//
//        return fabricSampleRepository
//                .findBySampleCodeContainingIgnoreCaseOrColorContainingIgnoreCase(
//                        search,
//                        search,
//                        pageable
//                )
//                .map(this::toResponse);
//    }
//
//
//    // ============================================================
//    // STATUS
//    // ============================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<FabricSampleResponse> getFabricSamplesByStatus(
//            FabricSample.Status status,
//            Pageable pageable
//    ) {
//
//        return fabricSampleRepository
//                .findByStatus(
//                        status,
//                        pageable
//                )
//                .map(this::toResponse);
//    }
//
//
//    // ============================================================
//    // CUSTOMER
//    // ============================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<FabricSampleResponse> getCustomerFabricSamples(
//            Long customerId,
//            Pageable pageable
//    ) {
//
//        return fabricSampleRepository
//                .findByCustomerId(
//                        customerId,
//                        pageable
//                )
//                .map(this::toResponse);
//    }
//
//
//    // ============================================================
//    // ORDER
//    // ============================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<FabricSampleResponse> getOrderFabricSamples(
//            Long orderId,
//            Pageable pageable
//    ) {
//
//        return fabricSampleRepository
//                .findByOrderId(
//                        orderId,
//                        pageable
//                )
//                .map(this::toResponse);
//    }
//
//
//    // ============================================================
//    // UPDATE
//    // ============================================================
//
//    @Override
//    public FabricSampleResponse updateFabricSample(
//            Long fabricSampleId,
//            FabricSampleCreateRequest request
//    ) {
//
//        FabricSample existingSample =
//                findById(fabricSampleId);
//
//
//        /*
//         * Sample code should never change.
//         */
//        existingSample.setBookNumber(
//                request.bookNumber()
//        );
//
//        existingSample.setPageNumber(
//                request.pageNumber()
//        );
//
//        existingSample.setColor(
//                request.color()
//        );
//
//        existingSample.setDescription(
//                request.description()
//        );
//
//
//        if (request.status() != null) {
//
//            existingSample.setStatus(
//                    request.status()
//            );
//        }
//
//
//        FabricSample updatedSample =
//                fabricSampleRepository.save(
//                        existingSample
//                );
//
//
//        return toResponse(updatedSample);
//    }
//
//
//    // ============================================================
//    // DELETE
//    // ============================================================
//
//    @Override
//    public void deleteFabricSample(
//            Long fabricSampleId
//    ) {
//
//        FabricSample fabricSample =
//                findById(fabricSampleId);
//
//
//        /*
//         * Don't delete a fabric sample that is already
//         * associated with an order.
//         *
//         * This protects your order history.
//         */
//        if (fabricSample.getOrder() != null) {
//
//            throw new IllegalStateException(
//                    "Fabric sample cannot be deleted because it is associated with an order"
//            );
//        }
//
//
//        fabricSampleRepository.delete(
//                fabricSample
//        );
//    }
//
//
//    // ============================================================
//    // FIND
//    // ============================================================
//
//    private FabricSample findById(
//            Long fabricSampleId
//    ) {
//
//        return fabricSampleRepository
//                .findById(fabricSampleId)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Fabric sample not found with id: "
//                                        + fabricSampleId
//                        )
//                );
//    }
//
//
//    // ============================================================
//    // SAMPLE CODE
//    // ============================================================
//
//    private String generateSampleCode() {
//
//        /*
//         * Simple initial implementation.
//         *
//         * Example:
//         * FS-000001
//         *
//         * For a high-volume production system,
//         * use a database sequence instead.
//         */
//
//        long nextId =
//                fabricSampleRepository.count() + 1;
//
//
//        String sampleCode =
//                String.format(
//                        "FS-%06d",
//                        nextId
//                );
//
//
//        /*
//         * Protect against collision.
//         */
//        while (
//                fabricSampleRepository
//                        .existsBySampleCode(sampleCode)
//        ) {
//
//            nextId++;
//
//            sampleCode =
//                    String.format(
//                            "FS-%06d",
//                            nextId
//                    );
//        }
//
//
//        return sampleCode;
//    }
//
//
//    // ============================================================
//    // RESPONSE MAPPING
//    // ============================================================
//
//    private FabricSampleResponse toResponse(
//            FabricSample fabricSample
//    ) {
//
//        User customer =
//                fabricSample.getCustomer();
//
//        Order order =
//                fabricSample.getOrder();
//
//
//        return new FabricSampleResponse(
//
//                fabricSample.getId(),
//
//                fabricSample.getSampleCode(),
//
//                fabricSample.getBookNumber(),
//
//                fabricSample.getPageNumber(),
//
//                fabricSample.getColor(),
//
//                fabricSample.getDescription(),
//
//                customer != null
//                        ? customer.getId()
//                        : null,
//
//                customer != null
//                        ? customer.getName()
//                        : null,
//
//                order != null
//                        ? order.getId()
//                        : null,
//
//                order != null
//                        ? order.getOrderNumber()
//                        : null,
//
//                fabricSample.getStatus(),
//
//                fabricSample.getCreatedAt()
//        );
//    }
//}