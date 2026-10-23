package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentStatus;
import com.sff.authapp.login.entities.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ============================================================
    // CUSTOMER
    // ============================================================

    Page<Order> findByCustomerId(
            Long customerId,
            Pageable pageable
    );


    List<Order> findByCustomerIdOrderByOrderDateDescIdDesc(
            Long customerId
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

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN o.garments g
            WHERE g.status = :status
            ORDER BY o.id DESC
            """)
    List<Order> findAllByGarmentStatus(
            @Param("status") GarmentStatus status
    );

    @Query("SELECT MAX(o.id) FROM Order o")
    Long findMaxOrderId();

    @Query("SELECT o.sampleBookPageNumber FROM Order o WHERE o.sampleBookPageNumber IS NOT NULL ORDER BY o.id DESC LIMIT 1")
    String findLatestSampleBookPageNumber();

    @Query("SELECT MAX(o.orderNumber) FROM Order o WHERE o.orderNumber IS NOT NULL")
    Long findMaxOrderNumber();

    Optional<Order> findByOrderNumber(Long orderNumber);

    Optional<Order> findByTrackingToken(String trackingToken);

    boolean existsByOrderNumber(Long orderNumber);

    @Query("SELECT o FROM Order o WHERE o.orderNumber IS NOT NULL ORDER BY o.orderNumber DESC")
    List<Order> findRecentHistoricalOrders(Pageable pageable);
}
