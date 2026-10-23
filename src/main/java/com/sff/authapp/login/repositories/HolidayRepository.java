package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository
        extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByHolidayDate(LocalDate date);

    boolean existsByHolidayDate(LocalDate date);

    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(
            LocalDate start,
            LocalDate end
    );

    List<Holiday> findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(
            LocalDate fromDate
    );
}
