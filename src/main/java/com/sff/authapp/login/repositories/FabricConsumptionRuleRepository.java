package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.FabricConsumptionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FabricConsumptionRuleRepository extends JpaRepository<FabricConsumptionRule, Long> {
    List<FabricConsumptionRule> findByGarmentType(String garmentType);
    Optional<FabricConsumptionRule> findByGarmentTypeAndWidthInches(String garmentType, BigDecimal widthInches);
}
