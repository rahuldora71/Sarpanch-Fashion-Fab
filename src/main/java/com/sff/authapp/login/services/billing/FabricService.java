package com.sff.authapp.login.services.billing;

import com.sff.authapp.login.dtos.billing.BillingDtos.*;
import com.sff.authapp.login.entities.Fabric;
import com.sff.authapp.login.entities.FabricConsumptionRule;
import com.sff.authapp.login.entities.FabricStockTransaction;
import com.sff.authapp.login.entities.TaxRule;
import com.sff.authapp.login.repositories.FabricConsumptionRuleRepository;
import com.sff.authapp.login.repositories.FabricRepository;
import com.sff.authapp.login.repositories.FabricStockTransactionRepository;
import com.sff.authapp.login.repositories.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FabricService {

    private final FabricRepository fabricRepository;
    private final FabricStockTransactionRepository stockTransactionRepository;
    private final FabricConsumptionRuleRepository consumptionRuleRepository;
    private final TaxRuleRepository taxRuleRepository;

    // --- FABRIC MASTER ---

    @Transactional(readOnly = true)
    public Page<FabricDto> getFabrics(String search, Boolean active, Pageable pageable) {
        return fabricRepository.findFilteredFabrics(search, active, pageable)
                .map(this::mapToFabricDto);
    }

    @Transactional(readOnly = true)
    public List<FabricDto> getActiveFabrics() {
        return fabricRepository.findByActiveTrue().stream()
                .map(this::mapToFabricDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FabricDto> getLowStockFabrics() {
        return fabricRepository.findLowStockFabrics().stream()
                .map(this::mapToFabricDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FabricDto getFabricById(Long id) {
        Fabric f = fabricRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fabric not found with id: " + id));
        return mapToFabricDto(f);
    }

    @Transactional
    public FabricDto createFabric(FabricDto dto) {
        if (dto.getFabricCode() == null || dto.getFabricCode().trim().isEmpty()) {
            dto.setFabricCode("FAB-" + System.currentTimeMillis() % 100000);
        }
        if (fabricRepository.existsByFabricCode(dto.getFabricCode())) {
            throw new IllegalArgumentException("Fabric code already exists: " + dto.getFabricCode());
        }

        TaxRule taxRule = null;
        if (dto.getTaxRuleId() != null) {
            taxRule = taxRuleRepository.findById(dto.getTaxRuleId()).orElse(null);
        }

        Fabric fabric = Fabric.builder()
                .name(dto.getName())
                .fabricCode(dto.getFabricCode().trim().toUpperCase())
                .fabricType(dto.getFabricType())
                .color(dto.getColor())
                .widthInches(dto.getWidthInches() != null ? dto.getWidthInches() : new BigDecimal("44.00"))
                .stockMeters(dto.getStockMeters() != null ? dto.getStockMeters() : BigDecimal.ZERO)
                .costPricePerMeter(dto.getCostPricePerMeter() != null ? dto.getCostPricePerMeter() : BigDecimal.ZERO)
                .sellingPricePerMeter(dto.getSellingPricePerMeter() != null ? dto.getSellingPricePerMeter() : BigDecimal.ZERO)
                .hsnCode(dto.getHsnCode() != null ? dto.getHsnCode() : "5208")
                .taxRule(taxRule)
                .minStockAlert(dto.getMinStockAlert() != null ? dto.getMinStockAlert() : new BigDecimal("10.00"))
                .supplier(dto.getSupplier())
                .notes(dto.getNotes())
                .active(dto.isActive())
                .build();

        fabric = fabricRepository.save(fabric);

        // Initial stock transaction if stock > 0
        if (fabric.getStockMeters().compareTo(BigDecimal.ZERO) > 0) {
            recordStockMovement(fabric, "PURCHASE", fabric.getStockMeters(), "INITIAL_STOCK", "MANUAL", "Initial stock opening", "SYSTEM");
        }

        return mapToFabricDto(fabric);
    }

    @Transactional
    public FabricDto updateFabric(Long id, FabricDto dto) {
        Fabric fabric = fabricRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fabric not found with id: " + id));

        fabric.setName(dto.getName());
        fabric.setFabricType(dto.getFabricType());
        fabric.setColor(dto.getColor());
        if (dto.getWidthInches() != null) fabric.setWidthInches(dto.getWidthInches());
        if (dto.getCostPricePerMeter() != null) fabric.setCostPricePerMeter(dto.getCostPricePerMeter());
        if (dto.getSellingPricePerMeter() != null) fabric.setSellingPricePerMeter(dto.getSellingPricePerMeter());
        if (dto.getHsnCode() != null) fabric.setHsnCode(dto.getHsnCode());
        if (dto.getMinStockAlert() != null) fabric.setMinStockAlert(dto.getMinStockAlert());
        fabric.setSupplier(dto.getSupplier());
        fabric.setNotes(dto.getNotes());
        fabric.setActive(dto.isActive());

        if (dto.getTaxRuleId() != null) {
            TaxRule taxRule = taxRuleRepository.findById(dto.getTaxRuleId()).orElse(null);
            fabric.setTaxRule(taxRule);
        }

        fabric = fabricRepository.save(fabric);
        return mapToFabricDto(fabric);
    }

    // --- STOCK MOVEMENTS & TRANSACTIONS ---

    @Transactional
    public FabricDto adjustStock(FabricStockAdjustmentDto req) {
        Fabric fabric = fabricRepository.findById(req.getFabricId())
                .orElseThrow(() -> new IllegalArgumentException("Fabric not found with id: " + req.getFabricId()));

        recordStockMovement(fabric, req.getTransactionType(), req.getQuantityMeters(), req.getReferenceNo(), req.getReferenceType(), req.getNotes(), req.getPerformedBy());
        return mapToFabricDto(fabric);
    }

    @Transactional
    public void recordStockMovement(
            Fabric fabric,
            String type,
            BigDecimal qtyMeters,
            String refNo,
            String refType,
            String notes,
            String performedBy
    ) {
        if (qtyMeters == null || qtyMeters.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal prevStock = fabric.getStockMeters();
        BigDecimal newStock;

        // "PURCHASE", "RETURN" increase stock
        // "SALE", "ORDER_CONSUMPTION", "WASTAGE" decrease stock
        // "ADJUSTMENT" can be positive or negative
        boolean isReduction = "SALE".equalsIgnoreCase(type)
                || "ORDER_CONSUMPTION".equalsIgnoreCase(type)
                || "WASTAGE".equalsIgnoreCase(type);

        if (isReduction) {
            BigDecimal deduct = qtyMeters.abs();
            newStock = prevStock.subtract(deduct);
        } else if ("PURCHASE".equalsIgnoreCase(type) || "RETURN".equalsIgnoreCase(type)) {
            newStock = prevStock.add(qtyMeters.abs());
        } else {
            // ADJUSTMENT
            newStock = prevStock.add(qtyMeters);
        }

        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            newStock = BigDecimal.ZERO;
        }

        fabric.setStockMeters(newStock);
        fabricRepository.save(fabric);

        FabricStockTransaction tx = FabricStockTransaction.builder()
                .fabric(fabric)
                .transactionType(type.toUpperCase())
                .quantityMeters(qtyMeters)
                .previousStock(prevStock)
                .newStock(newStock)
                .referenceNo(refNo)
                .referenceType(refType)
                .notes(notes)
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .transactionDate(LocalDateTime.now())
                .build();

        stockTransactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public Page<FabricStockTransactionDto> getTransactions(Long fabricId, Pageable pageable) {
        return stockTransactionRepository.findByFabricIdOrderByTransactionDateDesc(fabricId, pageable)
                .map(tx -> FabricStockTransactionDto.builder()
                        .id(tx.getId())
                        .fabricId(tx.getFabric().getId())
                        .fabricName(tx.getFabric().getName())
                        .fabricCode(tx.getFabric().getFabricCode())
                        .transactionType(tx.getTransactionType())
                        .quantityMeters(tx.getQuantityMeters())
                        .previousStock(tx.getPreviousStock())
                        .newStock(tx.getNewStock())
                        .referenceNo(tx.getReferenceNo())
                        .referenceType(tx.getReferenceType())
                        .notes(tx.getNotes())
                        .performedBy(tx.getPerformedBy())
                        .transactionDate(tx.getTransactionDate())
                        .build()
                );
    }

    // --- FABRIC CONSUMPTION RULES & ESTIMATOR ---

    @Transactional(readOnly = true)
    public List<FabricConsumptionRuleDto> getAllConsumptionRules() {
        return consumptionRuleRepository.findAll().stream()
                .map(r -> FabricConsumptionRuleDto.builder()
                        .id(r.getId())
                        .garmentType(r.getGarmentType())
                        .widthInches(r.getWidthInches())
                        .baseConsumptionMeters(r.getBaseConsumptionMeters())
                        .wastagePercent(r.getWastagePercent())
                        .description(r.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public FabricConsumptionRuleDto saveConsumptionRule(FabricConsumptionRuleDto dto) {
        FabricConsumptionRule rule;
        if (dto.getId() != null) {
            rule = consumptionRuleRepository.findById(dto.getId())
                    .orElse(new FabricConsumptionRule());
        } else {
            rule = new FabricConsumptionRule();
        }

        rule.setGarmentType(dto.getGarmentType().toUpperCase());
        rule.setWidthInches(dto.getWidthInches());
        rule.setBaseConsumptionMeters(dto.getBaseConsumptionMeters());
        rule.setWastagePercent(dto.getWastagePercent());
        rule.setDescription(dto.getDescription());

        rule = consumptionRuleRepository.save(rule);
        dto.setId(rule.getId());
        return dto;
    }

    public FabricEstimateResponseDto estimateFabric(FabricEstimateRequestDto req) {
        int qty = req.getQuantity() > 0 ? req.getQuantity() : 1;
        BigDecimal width = req.getWidthInches() != null ? req.getWidthInches() : new BigDecimal("44.00");
        String gType = req.getGarmentType() != null ? req.getGarmentType().toUpperCase() : "SHIRT";

        // Find best matching rule
        FabricConsumptionRule rule = consumptionRuleRepository.findByGarmentTypeAndWidthInches(gType, width)
                .orElseGet(() -> consumptionRuleRepository.findByGarmentType(gType).stream().findFirst()
                        .orElse(FabricConsumptionRule.builder()
                                .garmentType(gType)
                                .widthInches(width)
                                .baseConsumptionMeters(new BigDecimal("1.80"))
                                .wastagePercent(new BigDecimal("5.00"))
                                .build())
                );

        BigDecimal basePerGarment = rule.getBaseConsumptionMeters();

        // Optional adjustments for larger sizes (chest > 44 or length > 40)
        if (req.getChestSizeInches() != null && req.getChestSizeInches().compareTo(new BigDecimal("44")) > 0) {
            basePerGarment = basePerGarment.add(new BigDecimal("0.20"));
        }
        if (req.getLengthInches() != null && req.getLengthInches().compareTo(new BigDecimal("42")) > 0) {
            basePerGarment = basePerGarment.add(new BigDecimal("0.25"));
        }

        BigDecimal baseMeters = basePerGarment.multiply(new BigDecimal(qty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal wastageMeters = baseMeters.multiply(rule.getWastagePercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalMeters = baseMeters.add(wastageMeters).setScale(2, RoundingMode.HALF_UP);

        return FabricEstimateResponseDto.builder()
                .garmentType(gType)
                .widthInches(width)
                .quantity(qty)
                .baseMeters(baseMeters)
                .wastageMeters(wastageMeters)
                .totalEstimatedMeters(totalMeters)
                .notes(String.format("Estimated for %d %s on %s\" fabric (Includes %s%% waste allowance).",
                        qty, gType, width.toPlainString(), rule.getWastagePercent().toPlainString()))
                .build();
    }

    private FabricDto mapToFabricDto(Fabric f) {
        boolean isLow = f.getStockMeters().compareTo(f.getMinStockAlert()) <= 0;
        return FabricDto.builder()
                .id(f.getId())
                .name(f.getName())
                .fabricCode(f.getFabricCode())
                .fabricType(f.getFabricType())
                .color(f.getColor())
                .widthInches(f.getWidthInches())
                .stockMeters(f.getStockMeters())
                .costPricePerMeter(f.getCostPricePerMeter())
                .sellingPricePerMeter(f.getSellingPricePerMeter())
                .hsnCode(f.getHsnCode())
                .taxRuleId(f.getTaxRule() != null ? f.getTaxRule().getId() : null)
                .taxRuleName(f.getTaxRule() != null ? f.getTaxRule().getTaxName() : null)
                .minStockAlert(f.getMinStockAlert())
                .supplier(f.getSupplier())
                .notes(f.getNotes())
                .active(f.isActive())
                .isLowStock(isLow)
                .build();
    }
}
