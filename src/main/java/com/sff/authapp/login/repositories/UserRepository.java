package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Provider;
import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ============================================================
    // AUTHENTICATION
    // ============================================================

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);


    // ============================================================
    // OAUTH
    // ============================================================

    Optional<User> findByProviderAndProviderId(
            Provider provider,
            String providerId
    );


    // ============================================================
    // CUSTOMER
    // ============================================================

    Optional<User> findByCustomerCode(
            String customerCode
    );

    boolean existsByCustomerCode(
            String customerCode
    );

    /*
     * Phone is NOT unique.
     */
    List<User> findAllByPhone(String phone);


    // ============================================================
    // CUSTOMER PAGINATION
    // ============================================================

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.roles r
            WHERE r.name = :roleName
            """)
    Page<User> findAllCustomers(
            @Param("roleName") String roleName,
            Pageable pageable
    );


    // ============================================================
    // CUSTOMER SEARCH + PAGINATION
    // ============================================================

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.roles r
            WHERE r.name = :roleName
            AND (
                LOWER(u.name) LIKE
                    LOWER(CONCAT('%', :keyword, '%'))

                OR LOWER(u.email) LIKE
                    LOWER(CONCAT('%', :keyword, '%'))

                OR u.phone LIKE
                    CONCAT('%', :keyword, '%')

                OR LOWER(u.customerCode) LIKE
                    LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<User> searchCustomers(
            @Param("keyword") String keyword,
            @Param("roleName") String roleName,
            Pageable pageable
    );
}