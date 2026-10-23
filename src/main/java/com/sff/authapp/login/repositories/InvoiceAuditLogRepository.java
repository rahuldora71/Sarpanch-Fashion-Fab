package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.InvoiceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceAuditLogRepository extends JpaRepository<InvoiceAuditLog, Long> {
    List<InvoiceAuditLog> findByInvoiceIdOrderByTimestampDesc(Long invoiceId);
}
