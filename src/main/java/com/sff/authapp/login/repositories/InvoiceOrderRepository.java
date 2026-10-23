package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.InvoiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceOrderRepository extends JpaRepository<InvoiceOrder, Long> {

    List<InvoiceOrder> findByOrderId(Long orderId);

    List<InvoiceOrder> findByInvoiceId(Long invoiceId);

    @Query("""
            SELECT io FROM InvoiceOrder io
            JOIN io.invoice inv
            WHERE io.order.id = :orderId AND inv.isCancelled = false
            """)
    List<InvoiceOrder> findActiveInvoiceOrdersByOrderId(@Param("orderId") Long orderId);

    @Query("""
            SELECT io.order.id FROM InvoiceOrder io
            JOIN io.invoice inv
            WHERE inv.isCancelled = false
            """)
    List<Long> findBilledOrderIds();
}
