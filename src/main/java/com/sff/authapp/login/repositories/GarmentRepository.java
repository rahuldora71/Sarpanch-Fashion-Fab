package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GarmentRepository
        extends JpaRepository<Garment, Long> {

    List<Garment> findByOrderIdOrderByIdAsc(
            Long orderId
    );

    @Query("""
            SELECT g
            FROM Garment g
            JOIN FETCH g.order
            JOIN FETCH g.service
            LEFT JOIN FETCH g.garmentMeasurement
            WHERE g.order.customer.id = :customerId
            ORDER BY g.order.id DESC, g.id ASC
            """)
    List<Garment> findAllByCustomerIdForProfile(
            @Param("customerId") Long customerId
    );

    Page<Garment> findByOrderId(
            Long orderId,
            Pageable pageable
    );

    Page<Garment> findByStatus(
            GarmentStatus status,
            Pageable pageable
    );

    Page<Garment> findByServiceId(
            Long serviceId,
            Pageable pageable
    );

    @Query("""
        SELECT g
        FROM Garment g
        WHERE g.order.customer.id = :customerId
        ORDER BY g.createdAt DESC
        """)
    Page<Garment> findByCustomerId(
            @Param("customerId") Long customerId,
            Pageable pageable
    );

    boolean existsByOrderIdAndServiceId(
            Long orderId,
            Long serviceId
    );

    Optional<Garment> findFirstByGarmentMeasurementIdOrderByIdAsc(
            Long measurementId
    );

    List<Garment> findByGarmentMeasurementId(
            Long measurementId
    );
    @Query("""
        SELECT DISTINCT g
        FROM Garment g
        JOIN FETCH g.service
        LEFT JOIN FETCH g.garmentMeasurement
        LEFT JOIN FETCH g.addOns ga
        LEFT JOIN FETCH ga.addOn
        WHERE g.order.id = :orderId
        ORDER BY g.id ASC
        """)
    List<Garment> findCompleteGarmentsByOrderId(
            @Param("orderId") Long orderId
    );

    @Query(
        value = """
            SELECT g
            FROM Garment g
            JOIN FETCH g.order o
            JOIN FETCH o.customer c
            JOIN FETCH g.service s
            LEFT JOIN FETCH g.garmentMeasurement gm
            WHERE (:status IS NULL OR g.status = :status)
              AND (:startDate IS NULL OR g.alterationDeliveryDate >= :startDate)
              AND (:endDate IS NULL OR g.alterationDeliveryDate <= :endDate)
              AND (
                  :search IS NULL OR :search = ''
                  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR CONCAT('', o.id) = :search
                  OR CONCAT('', g.id) = :search
              )
            ORDER BY
              CASE WHEN g.alterationDeliveryDate IS NULL THEN 1 ELSE 0 END,
              g.alterationDeliveryDate ASC,
              g.id ASC
            """,
        countQuery = """
            SELECT COUNT(g)
            FROM Garment g
            JOIN g.order o
            JOIN o.customer c
            WHERE (:status IS NULL OR g.status = :status)
              AND (:startDate IS NULL OR g.alterationDeliveryDate >= :startDate)
              AND (:endDate IS NULL OR g.alterationDeliveryDate <= :endDate)
              AND (
                  :search IS NULL OR :search = ''
                  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR CONCAT('', o.id) = :search
                  OR CONCAT('', g.id) = :search
              )
            """
    )
    Page<Garment> findAlterationQueue(
            @Param("status") GarmentStatus status,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    long countByStatus(GarmentStatus status);

    @Query("""
        SELECT COUNT(g)
        FROM Garment g
        WHERE g.status = :status
          AND g.alterationDeliveryDate < :today
    """)
    long countOverdueAlterations(
            @Param("status") GarmentStatus status,
            @Param("today") java.time.LocalDate today
    );

    @Query("""
        SELECT COUNT(g)
        FROM Garment g
        WHERE g.status = :status
          AND g.alterationDeliveryDate = :date
    """)
    long countAlterationsOnDate(
            @Param("status") GarmentStatus status,
            @Param("date") java.time.LocalDate date
    );

    @Query("""
        SELECT COUNT(g)
        FROM Garment g
        WHERE g.status = :status
          AND g.alterationDeliveryDate > :date
    """)
    long countAlterationsAfterDate(
            @Param("status") GarmentStatus status,
            @Param("date") java.time.LocalDate date
    );
}