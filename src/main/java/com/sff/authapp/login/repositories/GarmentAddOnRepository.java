package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.GarmentAddOn;
import org.springframework.data.jpa.repository.JpaRepository;

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
}