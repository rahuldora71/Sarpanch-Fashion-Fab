package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Fabric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FabricRepository extends JpaRepository<Fabric, Long> {

    Optional<Fabric> findByFabricCode(String fabricCode);

    boolean existsByFabricCode(String fabricCode);

    List<Fabric> findByActiveTrue();

    @Query("""
            SELECT f FROM Fabric f
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(f.fabricCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(f.fabricType) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(f.color) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR f.active = :active)
            ORDER BY f.name ASC
            """)
    Page<Fabric> findFilteredFabrics(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("SELECT f FROM Fabric f WHERE f.active = true AND f.stockMeters <= f.minStockAlert")
    List<Fabric> findLowStockFabrics();
}
