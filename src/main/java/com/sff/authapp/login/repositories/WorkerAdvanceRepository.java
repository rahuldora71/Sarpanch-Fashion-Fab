package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.WorkerAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkerAdvanceRepository extends JpaRepository<WorkerAdvance, Long> {
    List<WorkerAdvance> findByWorkerIdAndAdvanceDateBetweenOrderByAdvanceDateAsc(Long workerId, LocalDate startDate, LocalDate endDate);
    List<WorkerAdvance> findByWorkerIdOrderByAdvanceDateDesc(Long workerId);
}
