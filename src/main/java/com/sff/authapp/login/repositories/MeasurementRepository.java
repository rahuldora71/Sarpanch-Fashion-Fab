//package com.sff.authapp.login.repositories;
//
//import com.sff.authapp.login.entities.Measurement;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface MeasurementRepository
//        extends JpaRepository<Measurement, Long> {
//
//    // ============================================================
//    // CUSTOMER
//    // ============================================================
//
//    Page<Measurement> findByUserId(
//            Long userId,
//            Pageable pageable
//    );
//
//
//    // ============================================================
//    // CUSTOMER + SERVICE
//    // ============================================================
//
//    Page<Measurement> findByUserIdAndServiceId(
//            Long userId,
//            Long serviceId,
//            Pageable pageable
//    );
//
//
//    // ============================================================
//    // LATEST CUSTOMER + SERVICE MEASUREMENT
//    // ============================================================
//
//    Optional<Measurement>
//    findTopByUserIdAndServiceIdOrderByCreatedAtDescIdDesc(
//            Long userId,
//            Long serviceId
//    );
//
//
//    // ============================================================
//    // ALL CUSTOMER MEASUREMENTS
//    // ============================================================
//
//    List<Measurement> findByUserIdOrderByCreatedAtDescIdDesc(
//            Long userId
//    );
//
//
//    // ============================================================
//    // CUSTOMER'S OTHER SERVICE MEASUREMENTS
//    // ============================================================
//
//    List<Measurement>
//    findByUserIdAndServiceIdNotOrderByCreatedAtDescIdDesc(
//            Long userId,
//            Long serviceId
//    );
//
//
//    // ============================================================
//    // LATEST MEASUREMENT PER SERVICE
//    // ============================================================
//
//    /**
//     * Returns the latest measurement for every service
//     * belonging to the customer.
//     */
//    @Query("""
//            SELECT m
//            FROM Measurement m
//            WHERE m.user.id = :userId
//            AND NOT EXISTS (
//                SELECT newer.id
//                FROM Measurement newer
//                WHERE newer.user.id = :userId
//                AND newer.service.id = m.service.id
//                AND (
//                    newer.createdAt > m.createdAt
//                    OR (
//                        newer.createdAt = m.createdAt
//                        AND newer.id > m.id
//                    )
//                )
//            )
//            ORDER BY m.createdAt DESC
//            """)
//    List<Measurement> findLatestMeasurementsPerService(
//            @Param("userId") Long userId
//    );
//}