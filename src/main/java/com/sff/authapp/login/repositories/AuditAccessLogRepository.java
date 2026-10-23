package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.AuditAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditAccessLogRepository extends JpaRepository<AuditAccessLog, Long> {

    List<AuditAccessLog> findTop100ByOrderByTimestampDesc();

    Page<AuditAccessLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
