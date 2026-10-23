package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.WorkerAttendance;
import com.sff.authapp.login.entities.WorkerAttendance.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkerAttendanceRepository
        extends JpaRepository<WorkerAttendance, Long> {

    Optional<WorkerAttendance> findByWorkerIdAndAttendanceDate(
            Long workerId,
            LocalDate date
    );

    List<WorkerAttendance> findByAttendanceDateOrderByWorkerIdAsc(
            LocalDate date
    );

    List<WorkerAttendance> findByWorkerIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long workerId,
            LocalDate start,
            LocalDate end
    );

    List<WorkerAttendance> findByWorkerIdOrderByAttendanceDateDesc(Long workerId);

    @Query("""
        SELECT wa FROM WorkerAttendance wa
        WHERE wa.attendanceDate = :date
          AND wa.status NOT IN (:absentStatuses)
        """)
    List<WorkerAttendance> findPresentWorkersOnDate(
            @Param("date") LocalDate date,
            @Param("absentStatuses") List<AttendanceStatus> absentStatuses
    );
}
