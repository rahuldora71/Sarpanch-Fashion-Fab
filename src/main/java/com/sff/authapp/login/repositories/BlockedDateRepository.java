package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.BlockedDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BlockedDateRepository
        extends JpaRepository<BlockedDate, Long> {

    Optional<BlockedDate> findByBlockedDate(LocalDate date);

    boolean existsByBlockedDate(LocalDate date);

    List<BlockedDate> findByBlockedDateBetweenOrderByBlockedDateAsc(
            LocalDate start,
            LocalDate end
    );

    List<BlockedDate> findByBlockedDateGreaterThanEqualOrderByBlockedDateAsc(
            LocalDate fromDate
    );
}
