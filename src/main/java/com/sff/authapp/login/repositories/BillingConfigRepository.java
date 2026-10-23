package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.BillingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingConfigRepository extends JpaRepository<BillingConfig, Long> {
}
