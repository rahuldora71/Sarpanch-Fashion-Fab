package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.OrderAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OrderAccessLogRepository extends JpaRepository<OrderAccessLog, Long> {

    Page<OrderAccessLog> findAllByOrderByAccessedAtDesc(Pageable pageable);

    @Query("""
            SELECT a FROM OrderAccessLog a
            WHERE (:search IS NULL OR LOWER(a.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR STR(a.orderNumber) LIKE CONCAT('%', :search, '%')
                   OR LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.userAgent) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:deviceType IS NULL OR a.deviceType = :deviceType)
              AND (:startDate IS NULL OR a.accessedAt >= :startDate)
              AND (:endDate IS NULL OR a.accessedAt <= :endDate)
            ORDER BY a.accessedAt DESC
            """)
    Page<OrderAccessLog> filterAccessLogs(
            @Param("search") String search,
            @Param("deviceType") String deviceType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    long countByAccessedAtAfter(LocalDateTime dateTime);

    long countByOrderId(Long orderId);

    long countByTrackingToken(String trackingToken);
}
