package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.WorkerSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerSettlementRepository extends JpaRepository<WorkerSettlement, Long> {
    List<WorkerSettlement> findByWorkerIdOrderBySettlementDateDesc(Long workerId);
    Optional<WorkerSettlement> findTopByWorkerIdOrderByEndDateDesc(Long workerId);
}
