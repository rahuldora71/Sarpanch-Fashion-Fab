package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository
        extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByOrderIdOrderByPaidAtAscIdAsc(
            Long orderId
    );

    @Query("""
            SELECT p
            FROM Payment p
            JOIN FETCH p.order o
            WHERE o.customer.id = :customerId
            ORDER BY p.paidAt DESC, p.id DESC
            """)
    List<Payment> findAllByCustomerIdForProfile(
            @Param("customerId") Long customerId
    );

    @Query("SELECT COUNT(p), SUM(p.amount) FROM Payment p WHERE p.paidAt >= :start AND p.paidAt <= :end")
    List<Object[]> getStatsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) FROM Payment p WHERE p.paidAt >= :start AND p.paidAt <= :end GROUP BY p.paymentMethod")
    List<Object[]> getMethodStatsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}