package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.InvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSequence s WHERE s.prefix = :prefix AND s.financialYear = :financialYear")
    Optional<InvoiceSequence> findByPrefixAndFinancialYearWithLock(
            @Param("prefix") String prefix,
            @Param("financialYear") String financialYear
    );

    Optional<InvoiceSequence> findByPrefixAndFinancialYear(String prefix, String financialYear);
}
