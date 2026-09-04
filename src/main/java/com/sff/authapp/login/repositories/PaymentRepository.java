package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByPaidAtAscIdAsc(
            Long orderId
    );
}