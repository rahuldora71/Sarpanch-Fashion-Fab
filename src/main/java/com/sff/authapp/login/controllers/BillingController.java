package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.billing.BillingDtos.*;
import com.sff.authapp.login.entities.MonthEndClosing;
import com.sff.authapp.login.services.billing.BillingConfigService;
import com.sff.authapp.login.services.billing.BillingService;
import com.sff.authapp.login.services.billing.FabricService;
import com.sff.authapp.login.services.billing.TaxRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Billing & Tax Management", description = "Enterprise Tailoring Billing, Tax, Fabric Inventory & Reports")
public class BillingController {

    private final BillingService billingService;
    private final BillingConfigService billingConfigService;
    private final TaxRuleService taxRuleService;
    private final FabricService fabricService;

    // ============================================================
    // CONFIGURATION & TOGGLES
    // ============================================================

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get shop billing configuration and toggles")
    public ResponseEntity<BillingConfigDto> getConfig() {
        return ResponseEntity.ok(billingConfigService.getConfigDto());
    }

    @PutMapping("/config")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Update shop billing configuration and toggles")
    public ResponseEntity<BillingConfigDto> updateConfig(@RequestBody BillingConfigDto dto) {
        return ResponseEntity.ok(billingConfigService.updateConfig(dto));
    }

    // ============================================================
    // TAX RULES
    // ============================================================

    @GetMapping("/tax-rules")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "List all tax rules")
    public ResponseEntity<List<TaxRuleDto>> getAllTaxRules() {
        return ResponseEntity.ok(taxRuleService.getAllTaxRules());
    }

    @GetMapping("/tax-rules/active")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "List active tax rules")
    public ResponseEntity<List<TaxRuleDto>> getActiveTaxRules() {
        return ResponseEntity.ok(taxRuleService.getActiveTaxRules());
    }

    @PostMapping("/tax-rules")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Create or update a tax rule")
    public ResponseEntity<TaxRuleDto> saveTaxRule(@RequestBody TaxRuleDto dto) {
        return ResponseEntity.ok(taxRuleService.saveTaxRule(dto));
    }

    @PutMapping("/tax-rules/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Update an existing tax rule")
    public ResponseEntity<TaxRuleDto> updateTaxRule(@PathVariable Long id, @RequestBody TaxRuleDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(taxRuleService.saveTaxRule(dto));
    }

    // ============================================================
    // ELIGIBLE ORDERS FOR BILLING
    // ============================================================

    @GetMapping("/orders/eligible")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get eligible orders with derived classification for bill generation")
    public ResponseEntity<List<EligibleOrderDto>> getEligibleOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String billingStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String classification,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(billingService.getEligibleOrders(
                search, customer, orderNumber, billingStatus, paymentStatus, classification, startDate, endDate
        ));
    }

    @PutMapping("/garments/{id}/fabric-source")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Update garment fabric source (Customer Cloth vs Shop Fabric)")
    public ResponseEntity<Void> updateGarmentFabricSource(
            @PathVariable Long id,
            @RequestParam boolean customerFabric
    ) {
        billingService.updateGarmentFabricSource(id, customerFabric);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // INVOICE GENERATION & REGISTER
    // ============================================================

    @PostMapping("/invoices/generate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Generate tax invoice from selected orders")
    public ResponseEntity<InvoiceResponseDto> generateInvoice(
            @RequestBody GenerateInvoiceRequestDto req,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "STAFF";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.generateInvoiceFromOrders(req, username));
    }

    @PostMapping("/invoices/manual")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Create custom / manual invoice with arbitrary line items")
    public ResponseEntity<InvoiceResponseDto> createManualInvoice(
            @RequestBody CreateManualInvoiceRequestDto req,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "STAFF";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.createManualInvoice(req, username));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get paginated invoice register with search and filters")
    public ResponseEntity<Page<InvoiceResponseDto>> getInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String classification,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean isCancelled,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(billingService.getInvoices(
                search, status, classification, paymentStatus, startDate, endDate, isCancelled, pageable
        ));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get detailed invoice by ID")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.getInvoiceById(id));
    }

    @PostMapping("/invoices/{id}/payments")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Record a collection/payment against an invoice")
    public ResponseEntity<InvoiceResponseDto> recordPayment(
            @PathVariable Long id,
            @RequestBody RecordPaymentRequestDto req,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "STAFF";
        return ResponseEntity.ok(billingService.recordPayment(id, req, username));
    }

    @PostMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Void/cancel an invoice with reason and audit log")
    public ResponseEntity<InvoiceResponseDto> cancelInvoice(
            @PathVariable Long id,
            @RequestBody CancelInvoiceRequestDto req,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "ADMIN";
        return ResponseEntity.ok(billingService.cancelInvoice(id, req, username));
    }

    // ============================================================
    // DASHBOARD & REPORTS
    // ============================================================

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get billing dashboard KPI summary and charts")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        return ResponseEntity.ok(billingService.getDashboardSummary());
    }

    @GetMapping("/reports/monthly")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get monthly GST and turnover report")
    public ResponseEntity<MonthlyReportDto> getMonthlyReport(
            @RequestParam int month,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(billingService.getMonthlyReport(month, year));
    }

    @GetMapping("/reports/ca-export")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get Chartered Accountant (CA) GST export breakdown")
    public ResponseEntity<CaExportDto> getCaExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(billingService.getCaExport(startDate, endDate));
    }

    @PostMapping("/reports/close-month")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Freeze and close accounts for a specific month")
    public ResponseEntity<MonthEndClosing> closeMonth(
            @RequestBody MonthEndClosingRequestDto req,
            Principal principal
    ) {
        if (req.getClosedBy() == null && principal != null) {
            req.setClosedBy(principal.getName());
        }
        return ResponseEntity.ok(billingService.closeMonth(req));
    }

    // ============================================================
    // FABRIC MASTER & STOCK
    // ============================================================

    @GetMapping("/fabrics")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get fabrics list with pagination")
    public ResponseEntity<Page<FabricDto>> getFabrics(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(fabricService.getFabrics(search, active, pageable));
    }

    @GetMapping("/fabrics/active")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get all active fabrics for dropdowns")
    public ResponseEntity<List<FabricDto>> getActiveFabrics() {
        return ResponseEntity.ok(fabricService.getActiveFabrics());
    }

    @GetMapping("/fabrics/low-stock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get fabrics below minimum alert stock")
    public ResponseEntity<List<FabricDto>> getLowStockFabrics() {
        return ResponseEntity.ok(fabricService.getLowStockFabrics());
    }

    @GetMapping("/fabrics/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get fabric details by ID")
    public ResponseEntity<FabricDto> getFabricById(@PathVariable Long id) {
        return ResponseEntity.ok(fabricService.getFabricById(id));
    }

    @PostMapping("/fabrics")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Create new fabric in inventory master")
    public ResponseEntity<FabricDto> createFabric(@RequestBody FabricDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fabricService.createFabric(dto));
    }

    @PutMapping("/fabrics/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Update fabric in inventory master")
    public ResponseEntity<FabricDto> updateFabric(@PathVariable Long id, @RequestBody FabricDto dto) {
        return ResponseEntity.ok(fabricService.updateFabric(id, dto));
    }

    @PostMapping("/fabrics/adjust-stock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Record stock adjustment or movement")
    public ResponseEntity<FabricDto> adjustStock(
            @RequestBody FabricStockAdjustmentDto req,
            Principal principal
    ) {
        if (req.getPerformedBy() == null && principal != null) {
            req.setPerformedBy(principal.getName());
        }
        return ResponseEntity.ok(fabricService.adjustStock(req));
    }

    @GetMapping("/fabrics/{id}/transactions")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get stock transaction ledger for fabric")
    public ResponseEntity<Page<FabricStockTransactionDto>> getFabricTransactions(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(fabricService.getTransactions(id, pageable));
    }

    // ============================================================
    // FABRIC CONSUMPTION ESTIMATOR & RULES
    // ============================================================

    @GetMapping("/fabric-consumption/rules")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "List all fabric consumption rules")
    public ResponseEntity<List<FabricConsumptionRuleDto>> getConsumptionRules() {
        return ResponseEntity.ok(fabricService.getAllConsumptionRules());
    }

    @PostMapping("/fabric-consumption/rules")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Save or update a fabric consumption rule")
    public ResponseEntity<FabricConsumptionRuleDto> saveConsumptionRule(@RequestBody FabricConsumptionRuleDto dto) {
        return ResponseEntity.ok(fabricService.saveConsumptionRule(dto));
    }

    @PostMapping("/fabric-consumption/estimate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Estimate required fabric consumption based on garment and width")
    public ResponseEntity<FabricEstimateResponseDto> estimateFabric(@RequestBody FabricEstimateRequestDto req) {
        return ResponseEntity.ok(fabricService.estimateFabric(req));
    }
}
