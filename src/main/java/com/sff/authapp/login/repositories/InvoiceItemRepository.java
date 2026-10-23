package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    @Query("""
            SELECT COALESCE(SUM(it.totalAmount), 0)
            FROM InvoiceItem it
            JOIN it.invoice inv
            WHERE inv.invoiceDate BETWEEN :start AND :end
              AND inv.isCancelled = false
              AND it.itemType = 'FABRIC'
            """)
    BigDecimal sumFabricSalesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT COALESCE(SUM(it.totalAmount), 0)
            FROM InvoiceItem it
            JOIN it.invoice inv
            WHERE inv.invoiceDate BETWEEN :start AND :end
              AND inv.isCancelled = false
              AND it.itemType IN ('STITCHING', 'ADD_ON')
            """)
    BigDecimal sumStitchingSalesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT it.hsnSacCode, 
                   SUM(it.quantity), 
                   SUM(it.taxableAmount), 
                   it.gstRate, 
                   SUM(it.cgstAmount), 
                   SUM(it.sgstAmount), 
                   SUM(it.igstAmount), 
                   SUM(it.totalTax), 
                   SUM(it.totalAmount)
            FROM InvoiceItem it
            JOIN it.invoice inv
            WHERE inv.invoiceDate BETWEEN :start AND :end
              AND inv.isCancelled = false
            GROUP BY it.hsnSacCode, it.gstRate
            ORDER BY it.hsnSacCode ASC
            """)
    List<Object[]> getHsnSummaryBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
