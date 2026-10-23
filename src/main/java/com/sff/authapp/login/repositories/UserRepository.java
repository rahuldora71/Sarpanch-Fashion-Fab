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

//    user by id
    Optional<User> findUserById(Long id);

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

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.roles r
            WHERE r.name = 'ROLE_CUSTOMER'
            AND (
                u.phone LIKE CONCAT('%', :phone, '%')
                OR u.alternatePhone LIKE CONCAT('%', :phone, '%')
            )
            ORDER BY u.id DESC
            """)
    List<User> findCustomersByPhoneContaining(@Param("phone") String phone);


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

    // ============================================================
    // WORKERS
    // ============================================================

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.roles r
            WHERE r.name IN ('ROLE_WORKER', 'ROLE_TAILOR', 'WORKER', 'TAILOR')
            ORDER BY u.name ASC
            """)
    List<User> findAllWorkersList();

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.roles r
            WHERE r.name IN ('ROLE_WORKER', 'ROLE_TAILOR', 'WORKER', 'TAILOR')
            AND (
                LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR u.phone LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(u.workerSpecialty, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY u.name ASC
            """)
    List<User> searchWorkersList(@Param("keyword") String keyword);
}