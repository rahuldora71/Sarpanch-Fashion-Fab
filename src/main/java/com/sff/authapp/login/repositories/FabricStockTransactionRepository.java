package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.FabricStockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FabricStockTransactionRepository extends JpaRepository<FabricStockTransaction, Long> {
    List<FabricStockTransaction> findByFabricIdOrderByTransactionDateDesc(Long fabricId);
    Page<FabricStockTransaction> findByFabricIdOrderByTransactionDateDesc(Long fabricId, Pageable pageable);
}
