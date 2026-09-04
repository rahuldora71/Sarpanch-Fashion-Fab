package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.AdditionalCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdditionalChargeRepository
        extends JpaRepository<AdditionalCharge, Long> {

    List<AdditionalCharge>
    findByOrderIdOrderByCreatedAtAscIdAsc(
            Long orderId
    );
}