package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.MonthEndClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthEndClosingRepository extends JpaRepository<MonthEndClosing, Long> {
    Optional<MonthEndClosing> findByClosingMonthAndClosingYear(int closingMonth, int closingYear);
    boolean existsByClosingMonthAndClosingYear(int closingMonth, int closingYear);
}
