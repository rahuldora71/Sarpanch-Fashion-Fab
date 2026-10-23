package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT l FROM AuditLog l
            WHERE (:search IS NULL OR LOWER(l.action) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(l.entityName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(l.details) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR (l.user IS NOT NULL AND LOWER(l.user.name) LIKE LOWER(CONCAT('%', :search, '%'))))
              AND (:action IS NULL OR l.action = :action)
              AND (:startDate IS NULL OR l.createdAt >= :startDate)
              AND (:endDate IS NULL OR l.createdAt <= :endDate)
            ORDER BY l.createdAt DESC
            """)
    Page<AuditLog> filterAuditLogs(
            @Param("search") String search,
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
