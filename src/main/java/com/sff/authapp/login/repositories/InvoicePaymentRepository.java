package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {

    List<InvoicePayment> findByInvoiceId(Long invoiceId);

    @Query("""
            SELECT p.paymentMethod, COUNT(p), SUM(p.amount)
            FROM InvoicePayment p
            WHERE p.paymentDate BETWEEN :start AND :end
            GROUP BY p.paymentMethod
            """)
    List<Object[]> getPaymentMethodDistributionBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
