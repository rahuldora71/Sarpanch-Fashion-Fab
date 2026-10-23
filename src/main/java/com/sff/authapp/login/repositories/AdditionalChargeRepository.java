package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.AdditionalCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdditionalChargeRepository
        extends JpaRepository<AdditionalCharge, Long> {

    List<AdditionalCharge>
    findByOrderIdOrderByCreatedAtAscIdAsc(
            Long orderId
    );

    @Query("""
            SELECT ac
            FROM AdditionalCharge ac
            JOIN FETCH ac.order o
            WHERE o.customer.id = :customerId
            ORDER BY ac.createdAt ASC, ac.id ASC
            """)
    List<AdditionalCharge> findAllByCustomerIdForProfile(
            @Param("customerId") Long customerId
    );

    void deleteAllByOrderId(Long orderId);
}