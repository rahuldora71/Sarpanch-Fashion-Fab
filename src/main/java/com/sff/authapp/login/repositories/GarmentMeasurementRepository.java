package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.GarmentMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GarmentMeasurementRepository
        extends JpaRepository<GarmentMeasurement, Long> {

    Optional<GarmentMeasurement> findByGarmentId(
            Long garmentId
    );

    boolean existsByGarmentId(
            Long garmentId
    );
}