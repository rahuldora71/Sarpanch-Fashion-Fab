package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.GarmentAddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GarmentAddOnRepository
        extends JpaRepository<GarmentAddOn, Long> {

    List<GarmentAddOn> findByGarmentIdOrderByIdAsc(
            Long garmentId
    );

    boolean existsByGarmentIdAndAddOnId(
            Long garmentId,
            Long addOnId
    );

    @Query("""
            SELECT ga
            FROM GarmentAddOn ga
            JOIN FETCH ga.garment g
            WHERE g.order.customer.id = :customerId
            ORDER BY ga.id ASC
            """)
    List<GarmentAddOn> findAllByCustomerIdForProfile(
            @Param("customerId") Long customerId
    );

    void deleteAllByGarmentId(Long garmentId);
}