package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByInvoiceDateBetweenOrderByInvoiceDateDesc(LocalDate start, LocalDate end);

    @Query("""
            SELECT i FROM Invoice i
            WHERE (:search IS NULL OR :search = '' 
                   OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.customerPhone) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.customerGstin) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR :status = '' OR i.status = :status)
              AND (:classification IS NULL OR :classification = '' OR i.classification = :classification)
              AND (:paymentStatus IS NULL OR :paymentStatus = '' OR i.paymentStatus = :paymentStatus)
              AND (:startDate IS NULL OR i.invoiceDate >= :startDate)
              AND (:endDate IS NULL OR i.invoiceDate <= :endDate)
              AND (:isCancelled IS NULL OR i.isCancelled = :isCancelled)
            ORDER BY i.invoiceDate DESC, i.id DESC
            """)
    Page<Invoice> findFilteredInvoices(
            @Param("search") String search,
            @Param("status") String status,
            @Param("classification") String classification,
            @Param("paymentStatus") String paymentStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("isCancelled") Boolean isCancelled,
            Pageable pageable
    );

    @Query("""
            SELECT i FROM Invoice i
            WHERE (:startDate IS NULL OR i.invoiceDate >= :startDate)
              AND (:endDate IS NULL OR i.invoiceDate <= :endDate)
              AND (:isCancelled IS NULL OR i.isCancelled = :isCancelled)
            ORDER BY i.invoiceDate ASC, i.id ASC
            """)
    List<Invoice> findInvoicesForReport(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("isCancelled") Boolean isCancelled
    );

    // Dashboard aggregation queries
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceDate = :date AND i.isCancelled = false")
    long countByInvoiceDateAndNotCancelled(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.invoiceDate = :date AND i.isCancelled = false")
    BigDecimal sumGrandTotalByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(i.taxableAmount), 0) FROM Invoice i WHERE i.invoiceDate = :date AND i.isCancelled = false")
    BigDecimal sumTaxableByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(i.totalTax), 0) FROM Invoice i WHERE i.invoiceDate = :date AND i.isCancelled = false")
    BigDecimal sumTaxByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end AND i.isCancelled = false")
    BigDecimal sumGrandTotalBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(i.taxableAmount), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end AND i.isCancelled = false")
    BigDecimal sumTaxableBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(i.totalTax), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end AND i.isCancelled = false")
    BigDecimal sumTaxBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end AND i.isCancelled = false")
    long countBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end AND i.isCancelled = true")
    long countCancelledBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end AND i.isCancelled = false")
    BigDecimal sumPaidBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(i.balanceAmount), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end AND i.isCancelled = false")
    BigDecimal sumBalanceBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(i.balanceAmount), 0) FROM Invoice i WHERE i.isCancelled = false AND i.paymentStatus != 'PAID'")
    BigDecimal sumTotalOutstanding();
}
