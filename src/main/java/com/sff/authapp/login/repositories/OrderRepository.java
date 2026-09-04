package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.GarmentStatus;
import com.sff.authapp.login.entities.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ============================================================
    // CUSTOMER
    // ============================================================

    Page<Order> findByCustomerId(
            Long customerId,
            Pageable pageable
    );


    // ============================================================
    // ORDER DATE
    // ============================================================

    Page<Order> findByOrderDate(
            LocalDate orderDate,
            Pageable pageable
    );

    Page<Order> findByOrderDateBetween(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );


    // ============================================================
    // SAMPLE BOOK PAGE
    // ============================================================

    Page<Order> findBySampleBookPageNumberContainingIgnoreCase(
            String sampleBookPageNumber,
            Pageable pageable
    );


    // ============================================================
    // GARMENT STATUS
    // ============================================================

    /**
     * Returns orders containing at least one garment
     * with the requested status.
     *
     * DISTINCT is important because one order can contain
     * multiple garments with the same status.
     */
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN o.garments g
            WHERE g.status = :status
            """)
    Page<Order> findByGarmentStatus(
            @Param("status") GarmentStatus status,
            Pageable pageable
    );
}