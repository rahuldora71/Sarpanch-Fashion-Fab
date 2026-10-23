package com.sff.authapp.login.services.billing;

import com.sff.authapp.login.dtos.billing.BillingDtos.TaxRuleDto;
import com.sff.authapp.login.entities.TaxRule;
import com.sff.authapp.login.repositories.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaxRuleService {

    private final TaxRuleRepository taxRuleRepository;

    @Transactional(readOnly = true)
    public List<TaxRuleDto> getAllTaxRules() {
        return taxRuleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaxRuleDto> getActiveTaxRules() {
        return taxRuleRepository.findByActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaxRuleDto saveTaxRule(TaxRuleDto dto) {
        TaxRule rule;
        if (dto.getId() != null) {
            rule = taxRuleRepository.findById(dto.getId())
                    .orElse(new TaxRule());
        } else {
            rule = new TaxRule();
        }

        BigDecimal gst = dto.getGstRate() != null ? dto.getGstRate() : BigDecimal.ZERO;
        BigDecimal cgst = gst.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal sgst = gst.subtract(cgst);

        rule.setTaxName(dto.getTaxName());
        rule.setTaxType(dto.getTaxType() != null ? dto.getTaxType() : "GST");
        rule.setGstRate(gst);
        rule.setCgstRate(cgst);
        rule.setSgstRate(sgst);
        rule.setIgstRate(gst);
        rule.setHsnSacCode(dto.getHsnSacCode());
        rule.setCategory(dto.getCategory() != null ? dto.getCategory().toUpperCase() : "GENERAL");
        rule.setEffectiveFrom(dto.getEffectiveFrom());
        rule.setEffectiveTo(dto.getEffectiveTo());
        rule.setActive(dto.isActive());
        rule.setDefault(dto.isDefault());
        rule.setDescription(dto.getDescription());

        rule = taxRuleRepository.save(rule);
        return mapToDto(rule);
    }

    private TaxRuleDto mapToDto(TaxRule r) {
        return TaxRuleDto.builder()
                .id(r.getId())
                .taxName(r.getTaxName())
                .taxType(r.getTaxType())
                .gstRate(r.getGstRate())
                .cgstRate(r.getCgstRate())
                .sgstRate(r.getSgstRate())
                .igstRate(r.getIgstRate())
                .hsnSacCode(r.getHsnSacCode())
                .category(r.getCategory())
                .effectiveFrom(r.getEffectiveFrom())
                .effectiveTo(r.getEffectiveTo())
                .active(r.isActive())
                .isDefault(r.isDefault())
                .description(r.getDescription())
                .build();
    }
}
