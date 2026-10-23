package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.TaxRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxRuleRepository extends JpaRepository<TaxRule, Long> {
    List<TaxRule> findByActiveTrue();
    Optional<TaxRule> findByCategoryAndIsDefaultTrue(String category);
    Optional<TaxRule> findByHsnSacCode(String hsnSacCode);
}
