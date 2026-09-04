package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.FabricSample;
import com.sff.authapp.login.entities.FabricSample.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FabricSampleRepository
        extends JpaRepository<FabricSample, Long> {

    // ============================================================
    // SAMPLE CODE
    // ============================================================

    Optional<FabricSample> findBySampleCode(
            String sampleCode
    );

    boolean existsBySampleCode(
            String sampleCode
    );


    // ============================================================
    // SEARCH
    // ============================================================

    Page<FabricSample> findBySampleCodeContainingIgnoreCase(
            String sampleCode,
            Pageable pageable
    );


    Page<FabricSample> findByColorContainingIgnoreCase(
            String color,
            Pageable pageable
    );


    // ============================================================
    // CUSTOMER
    // ============================================================

    Page<FabricSample> findByCustomerId(
            Long customerId,
            Pageable pageable
    );


    List<FabricSample> findByCustomerId(
            Long customerId
    );


    // ============================================================
    // ORDER
    // ============================================================

    Page<FabricSample> findByOrderId(
            Long orderId,
            Pageable pageable
    );


    List<FabricSample> findByOrderId(
            Long orderId
    );


    // ============================================================
    // STATUS
    // ============================================================

    Page<FabricSample> findByStatus(
            Status status,
            Pageable pageable
    );


    List<FabricSample> findByStatus(
            Status status
    );


    // ============================================================
    // CUSTOMER + STATUS
    // ============================================================

    Page<FabricSample> findByCustomerIdAndStatus(
            Long customerId,
            Status status,
            Pageable pageable
    );


    // ============================================================
    // ORDER + STATUS
    // ============================================================

    Page<FabricSample> findByOrderIdAndStatus(
            Long orderId,
            Status status,
            Pageable pageable
    );


    // ============================================================
    // FIND SAMPLE FOR A PARTICULAR CUSTOMER
    // ============================================================

    Optional<FabricSample> findByIdAndCustomerId(
            Long id,
            Long customerId
    );


    // ============================================================
    // CHECK
    // ============================================================

    boolean existsByIdAndCustomerId(
            Long id,
            Long customerId
    );
    Page<FabricSample> findBySampleCodeContainingIgnoreCaseOrColorContainingIgnoreCase(
            String sampleCode,
            String color,
            Pageable pageable
    );
}