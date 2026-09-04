package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GarmentRepository
        extends JpaRepository<Garment, Long> {

    List<Garment> findByOrderIdOrderByIdAsc(
            Long orderId
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
}