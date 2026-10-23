package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.ProductionCapacity;
import com.sff.authapp.login.entities.ProductionCapacity.ProductionStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductionCapacityRepository
        extends JpaRepository<ProductionCapacity, Long> {

    List<ProductionCapacity> findByCapacityDateAndActiveTrue(LocalDate date);

    List<ProductionCapacity> findByCapacityDateBetweenAndActiveTrue(
            LocalDate start,
            LocalDate end
    );

    Optional<ProductionCapacity> findByCapacityDateAndProductionStageAndWorkerIsNull(
            LocalDate date,
            ProductionStage stage
    );

    List<ProductionCapacity> findByCapacityDateAndProductionStageAndActiveTrue(
            LocalDate date,
            ProductionStage stage
    );

    @Query("""
        SELECT pc FROM ProductionCapacity pc
        WHERE pc.capacityDate = :date
          AND pc.productionStage = :stage
          AND pc.worker.id = :workerId
          AND pc.active = true
        """)
    Optional<ProductionCapacity> findByDateStageAndWorker(
            @Param("date") LocalDate date,
            @Param("stage") ProductionStage stage,
            @Param("workerId") Long workerId
    );
}
