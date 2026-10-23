package com.sff.authapp.login.services.billing;

import com.sff.authapp.login.dtos.billing.BillingDtos.*;
import com.sff.authapp.login.entities.BillingConfig;
import com.sff.authapp.login.entities.FabricConsumptionRule;
import com.sff.authapp.login.entities.TaxRule;
import com.sff.authapp.login.repositories.BillingConfigRepository;
import com.sff.authapp.login.repositories.FabricConsumptionRuleRepository;
import com.sff.authapp.login.repositories.TaxRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingConfigService {

    private final BillingConfigRepository billingConfigRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final FabricConsumptionRuleRepository consumptionRuleRepository;

    @PostConstruct
    @Transactional
    public void initDefaults() {
        // 1. Seed Billing Config if empty
        if (billingConfigRepository.count() == 0) {
            BillingConfig config = BillingConfig.builder()
                    .shopName("Sarpanch Fashion Fab")
                    .tagline("Custom Tailoring & Designer Fabrics")
                    .gstin("08AAAAA0000A1Z5")
                    .panNumber("AAAAA0000A")
                    .state("Rajasthan")
                    .stateCode("08")
                    .address("Shop No. 12, Main Bazaar, Jaipur, Rajasthan 302001")
                    .phone("+91 98765 43210")
                    .email("billing@sarpanchfab.com")
                    .bankName("State Bank of India")
                    .accountNumber("398712345678")
                    .ifscCode("SBIN0001234")
                    .upiId("sarpanchfab@upi")
                    .invoicePrefix("SFF")
                    .financialYear("2026-27")
                    .defaultTaxInclusive(false)
                    .includeStitchingOnly(true)
                    .allowMixedOrders(true)
                    .allowFabricOnly(true)
                    .allowManualInvoices(true)
                    .allowCustomLineItems(true)
                    .allowPartialBilling(true)
                    .enforceSequentialNumbering(true)
                    .termsAndConditions("1. Goods once sold/tailored will not be exchanged without invoice.\n2. Fitting / alteration complaints accepted within 7 days of delivery.\n3. Garments left unclaimed for over 90 days will be disposed of without liability.")
                    .footerNote("Thank you for choosing Sarpanch Fashion Fab! For custom orders call +91 98765 43210.")
                    .defaultInvoiceTemplate("A4")
                    .build();
            billingConfigRepository.save(config);
            log.info("Initialized default BillingConfig");
        }

        // 2. Seed Tax Rules if empty
        if (taxRuleRepository.count() == 0) {
            LocalDate now = LocalDate.now();
            TaxRule stitchingTax = TaxRule.builder()
                    .taxName("Tailoring & Stitching (SAC 9988) - 5%")
                    .taxType("GST")
                    .gstRate(new BigDecimal("5.00"))
                    .cgstRate(new BigDecimal("2.50"))
                    .sgstRate(new BigDecimal("2.50"))
                    .igstRate(new BigDecimal("5.00"))
                    .hsnSacCode("9988")
                    .category("STITCHING")
                    .effectiveFrom(now.minusYears(1))
                    .active(true)
                    .isDefault(true)
                    .description("Custom tailoring service and job work on textile garments")
                    .build();

            TaxRule fabricTax = TaxRule.builder()
                    .taxName("Woven Fabric (HSN 5208) - 5%")
                    .taxType("GST")
                    .gstRate(new BigDecimal("5.00"))
                    .cgstRate(new BigDecimal("2.50"))
                    .sgstRate(new BigDecimal("2.50"))
                    .igstRate(new BigDecimal("5.00"))
                    .hsnSacCode("5208")
                    .category("FABRIC")
                    .effectiveFrom(now.minusYears(1))
                    .active(true)
                    .isDefault(true)
                    .description("Cotton / Poly-blend shirting and suiting fabrics")
                    .build();

            TaxRule readymadeTax = TaxRule.builder()
                    .taxName("Ready Garments / Apparel (HSN 6205) - 12%")
                    .taxType("GST")
                    .gstRate(new BigDecimal("12.00"))
                    .cgstRate(new BigDecimal("6.00"))
                    .sgstRate(new BigDecimal("6.00"))
                    .igstRate(new BigDecimal("12.00"))
                    .hsnSacCode("6205")
                    .category("GENERAL")
                    .effectiveFrom(now.minusYears(1))
                    .active(true)
                    .isDefault(false)
                    .description("Garments valued above threshold or general accessories")
                    .build();

            TaxRule exemptTax = TaxRule.builder()
                    .taxName("Exempt / Non-GST")
                    .taxType("EXEMPT")
                    .gstRate(BigDecimal.ZERO)
                    .cgstRate(BigDecimal.ZERO)
                    .sgstRate(BigDecimal.ZERO)
                    .igstRate(BigDecimal.ZERO)
                    .hsnSacCode("0000")
                    .category("GENERAL")
                    .effectiveFrom(now.minusYears(1))
                    .active(true)
                    .isDefault(false)
                    .description("Non-taxable items or nil-rated tailors")
                    .build();

            taxRuleRepository.saveAll(List.of(stitchingTax, fabricTax, readymadeTax, exemptTax));
            log.info("Initialized default TaxRules");
        }

        // 3. Seed Fabric Consumption Rules if empty
        if (consumptionRuleRepository.count() == 0) {
            consumptionRuleRepository.saveAll(List.of(
                    FabricConsumptionRule.builder().garmentType("SHIRT").widthInches(new BigDecimal("44.00")).baseConsumptionMeters(new BigDecimal("1.60")).wastagePercent(new BigDecimal("5.00")).description("Full sleeves shirt on 44\" fabric").build(),
                    FabricConsumptionRule.builder().garmentType("SHIRT").widthInches(new BigDecimal("36.00")).baseConsumptionMeters(new BigDecimal("2.25")).wastagePercent(new BigDecimal("5.00")).description("Full sleeves shirt on 36\" narrow fabric").build(),
                    FabricConsumptionRule.builder().garmentType("SHIRT").widthInches(new BigDecimal("58.00")).baseConsumptionMeters(new BigDecimal("1.40")).wastagePercent(new BigDecimal("5.00")).description("Full sleeves shirt on 58\" wide fabric").build(),
                    FabricConsumptionRule.builder().garmentType("PANT").widthInches(new BigDecimal("58.00")).baseConsumptionMeters(new BigDecimal("1.25")).wastagePercent(new BigDecimal("5.00")).description("Formal trouser on 58\" suiting fabric").build(),
                    FabricConsumptionRule.builder().garmentType("PANT").widthInches(new BigDecimal("44.00")).baseConsumptionMeters(new BigDecimal("1.40")).wastagePercent(new BigDecimal("5.00")).description("Trouser on 44\" fabric").build(),
                    FabricConsumptionRule.builder().garmentType("SUIT_2PC").widthInches(new BigDecimal("58.00")).baseConsumptionMeters(new BigDecimal("3.00")).wastagePercent(new BigDecimal("6.00")).description("Blazer + Trouser on 58\" fabric").build(),
                    FabricConsumptionRule.builder().garmentType("SUIT_3PC").widthInches(new BigDecimal("58.00")).baseConsumptionMeters(new BigDecimal("3.60")).wastagePercent(new BigDecimal("6.00")).description("Blazer + Waistcoat + Trouser on 58\" fabric").build(),
                    FabricConsumptionRule.builder().garmentType("KURTA").widthInches(new BigDecimal("44.00")).baseConsumptionMeters(new BigDecimal("2.25")).wastagePercent(new BigDecimal("5.00")).description("Standard Men's Kurta on 44\" fabric").build(),
                    FabricConsumptionRule.builder().garmentType("PAJAMA").widthInches(new BigDecimal("44.00")).baseConsumptionMeters(new BigDecimal("2.00")).wastagePercent(new BigDecimal("5.00")).description("Standard Pajama on 44\" fabric").build(),
                    FabricConsumptionRule.builder().garmentType("WAISTCOAT").widthInches(new BigDecimal("58.00")).baseConsumptionMeters(new BigDecimal("0.80")).wastagePercent(new BigDecimal("5.00")).description("Nehru jacket / Modi coat on 58\" fabric").build(),
                    FabricConsumptionRule.builder().garmentType("SAFARI").widthInches(new BigDecimal("58.00")).baseConsumptionMeters(new BigDecimal("2.60")).wastagePercent(new BigDecimal("5.00")).description("Safari suit 2pc on 58\" fabric").build()
            ));
            log.info("Initialized default FabricConsumptionRules");
        }
    }

    @Transactional(readOnly = true)
    public BillingConfig getConfig() {
        return billingConfigRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    BillingConfig fallback = BillingConfig.builder().build();
                    return billingConfigRepository.save(fallback);
                });
    }

    @Transactional(readOnly = true)
    public BillingConfigDto getConfigDto() {
        BillingConfig c = getConfig();
        return BillingConfigDto.builder()
                .id(c.getId())
                .shopName(c.getShopName())
                .tagline(c.getTagline())
                .gstin(c.getGstin())
                .panNumber(c.getPanNumber())
                .state(c.getState())
                .stateCode(c.getStateCode())
                .address(c.getAddress())
                .phone(c.getPhone())
                .email(c.getEmail())
                .bankName(c.getBankName())
                .accountNumber(c.getAccountNumber())
                .ifscCode(c.getIfscCode())
                .upiId(c.getUpiId())
                .invoicePrefix(c.getInvoicePrefix())
                .financialYear(c.getFinancialYear())
                .defaultTaxInclusive(c.isDefaultTaxInclusive())
                .includeStitchingOnly(c.isIncludeStitchingOnly())
                .allowMixedOrders(c.isAllowMixedOrders())
                .allowFabricOnly(c.isAllowFabricOnly())
                .allowManualInvoices(c.isAllowManualInvoices())
                .allowCustomLineItems(c.isAllowCustomLineItems())
                .allowPartialBilling(c.isAllowPartialBilling())
                .enforceSequentialNumbering(c.isEnforceSequentialNumbering())
                .termsAndConditions(c.getTermsAndConditions())
                .footerNote(c.getFooterNote())
                .defaultInvoiceTemplate(c.getDefaultInvoiceTemplate())
                .build();
    }

    @Transactional
    public BillingConfigDto updateConfig(BillingConfigDto dto) {
        BillingConfig c = getConfig();
        if (dto.getShopName() != null) c.setShopName(dto.getShopName());
        if (dto.getTagline() != null) c.setTagline(dto.getTagline());
        if (dto.getGstin() != null) c.setGstin(dto.getGstin());
        if (dto.getPanNumber() != null) c.setPanNumber(dto.getPanNumber());
        if (dto.getState() != null) c.setState(dto.getState());
        if (dto.getStateCode() != null) c.setStateCode(dto.getStateCode());
        if (dto.getAddress() != null) c.setAddress(dto.getAddress());
        if (dto.getPhone() != null) c.setPhone(dto.getPhone());
        if (dto.getEmail() != null) c.setEmail(dto.getEmail());
        if (dto.getBankName() != null) c.setBankName(dto.getBankName());
        if (dto.getAccountNumber() != null) c.setAccountNumber(dto.getAccountNumber());
        if (dto.getIfscCode() != null) c.setIfscCode(dto.getIfscCode());
        if (dto.getUpiId() != null) c.setUpiId(dto.getUpiId());
        if (dto.getInvoicePrefix() != null) c.setInvoicePrefix(dto.getInvoicePrefix());
        if (dto.getFinancialYear() != null) c.setFinancialYear(dto.getFinancialYear());
        c.setDefaultTaxInclusive(dto.isDefaultTaxInclusive());
        c.setIncludeStitchingOnly(dto.isIncludeStitchingOnly());
        c.setAllowMixedOrders(dto.isAllowMixedOrders());
        c.setAllowFabricOnly(dto.isAllowFabricOnly());
        c.setAllowManualInvoices(dto.isAllowManualInvoices());
        c.setAllowCustomLineItems(dto.isAllowCustomLineItems());
        c.setAllowPartialBilling(dto.isAllowPartialBilling());
        c.setEnforceSequentialNumbering(dto.isEnforceSequentialNumbering());
        if (dto.getTermsAndConditions() != null) c.setTermsAndConditions(dto.getTermsAndConditions());
        if (dto.getFooterNote() != null) c.setFooterNote(dto.getFooterNote());
        if (dto.getDefaultInvoiceTemplate() != null) c.setDefaultInvoiceTemplate(dto.getDefaultInvoiceTemplate());

        c = billingConfigRepository.save(c);
        return getConfigDto();
    }
}
