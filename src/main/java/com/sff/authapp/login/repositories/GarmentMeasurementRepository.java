package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.GarmentMeasurement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GarmentMeasurementRepository
        extends JpaRepository<GarmentMeasurement, Long> {


    // ============================================================
    // FIND ALL WITH SEARCH
    // ============================================================

    @Query(
        value = """
            SELECT gm
            FROM GarmentMeasurement gm
            JOIN FETCH gm.service s
            JOIN FETCH gm.user u
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR (u.customerCode IS NOT NULL AND LOWER(u.customerCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY gm.createdAt DESC, gm.id DESC
            """,
        countQuery = """
            SELECT COUNT(gm)
            FROM GarmentMeasurement gm
            JOIN gm.user u
            JOIN gm.service s
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR (u.customerCode IS NOT NULL AND LOWER(u.customerCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """
    )
    Page<GarmentMeasurement> findAllWithSearch(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ============================================================
    // FIND BY CUSTOMER
    // ============================================================

    Page<GarmentMeasurement> findByUserIdOrderByCreatedAtDescIdDesc(
            Long userId,
            Pageable pageable
    );




    @Query("""
        SELECT gm
        FROM GarmentMeasurement gm
        JOIN FETCH gm.service
        WHERE gm.user.id = :userId
        ORDER BY gm.createdAt DESC, gm.id DESC
        """)
    List<GarmentMeasurement> findAllByUserIdForProfile(
            @Param("userId") Long userId
    );

    // ============================================================
    // FIND BY CUSTOMER + SERVICE
    // ============================================================

    Page<GarmentMeasurement>
    findByUserIdAndServiceIdOrderByCreatedAtDescIdDesc(
            Long userId,
            Long serviceId,
            Pageable pageable
    );


    // ============================================================
    // FIND LATEST MEASUREMENT
    // ============================================================

    Optional<GarmentMeasurement>
    findTopByUserIdAndServiceIdOrderByCreatedAtDescIdDesc(
            Long userId,
            Long serviceId
    );


    // ============================================================
    // FIND LATEST MEASUREMENT PER SERVICE
    // ============================================================

    @Query("""
        SELECT gm
        FROM GarmentMeasurement gm
        WHERE gm.user.id = :userId
          AND NOT EXISTS (
                SELECT newer.id
                FROM GarmentMeasurement newer
                WHERE newer.user.id = :userId
                  AND newer.service.id = gm.service.id
                  AND (
                        newer.createdAt > gm.createdAt
                        OR (
                            newer.createdAt = gm.createdAt
                            AND newer.id > gm.id
                        )
                  )
          )
        ORDER BY gm.createdAt DESC, gm.id DESC
        """)
    List<GarmentMeasurement> findLatestMeasurementsPerService(
            @Param("userId") Long userId
    );


    // ============================================================
    // FIND MEASUREMENT FOR COPY / REUSE
    // ============================================================

    /**
     * Finds a measurement only when it belongs to the
     * specified customer and tailoring service.
     *
     * This is used when a garment wants to reuse an
     * existing measurement by measurementId.
     */
    @Query("""
        SELECT gm
        FROM GarmentMeasurement gm
        WHERE gm.id = :measurementId
          AND gm.user.id = :userId
          AND gm.service.id = :serviceId
        """)
    Optional<GarmentMeasurement> findByIdAndUserIdAndServiceId(
            @Param("measurementId") Long measurementId,
            @Param("userId") Long userId,
            @Param("serviceId") Long serviceId
    );


    // ============================================================
    // CHECK CUSTOMER + SERVICE MEASUREMENT
    // ============================================================

    boolean existsByIdAndUserIdAndServiceId(
            Long measurementId,
            Long userId,
            Long serviceId
    );
}