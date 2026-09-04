package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.customer.CustomerCreateRequest;
import com.sff.authapp.login.dtos.customer.CustomerResponse;
import com.sff.authapp.login.dtos.customer.CustomerSummaryResponse;
import com.sff.authapp.login.dtos.customer.CustomerUpdateRequest;
import com.sff.authapp.login.entities.Provider;
import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.RoleRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.services.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final String CUSTOMER_ROLE =
            "ROLE_CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;


    // ============================================================
    // CREATE CUSTOMER
    // ============================================================

    @Override
    @Transactional
    public CustomerResponse createCustomer(
            CustomerCreateRequest request
    ) {

        String email =
                normalizeEmail(request.email());


        /*
         * Email is unique when supplied.
         */
        if (email != null
                && userRepository.existsByEmail(email)) {

            throw new IllegalArgumentException(
                    "User with this email already exists"
            );
        }


        Role customerRole =
                getCustomerRole();


        /*
         * Phone is intentionally NOT checked for uniqueness.
         *
         * Father and son can share the same phone number.
         */
        User customer =
                User.builder()
                        .name(request.name().trim())
                        .phone(request.phone())
                        .alternatePhone(
                                request.alternatePhone()
                        )
                        .email(email)
                        .address(request.address())
                        .city(request.city())
                        .notes(request.notes())
                        .password(null)
                        .enable(true)
                        .provider(Provider.LOCAL)
                        .roles(new HashSet<>())
                        .build();


        customer.getRoles()
                .add(customerRole);


        /*
         * Customer code will be generated
         * when the first order is created.
         */
        customer.setCustomerCode(null);


        User savedCustomer =
                userRepository.save(customer);


        return mapToResponse(savedCustomer);
    }


    // ============================================================
    // GET ALL CUSTOMERS - PAGINATED
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> getAllCustomers(
            Pageable pageable
    ) {

        Role customerRole =
                getCustomerRole();


        Page<User> customers =
                userRepository.findAllCustomers(
                        customerRole.getName(),
                        pageable
                );


        return customers.map(
                this::mapToSummaryResponse
        );
    }


    // ============================================================
    // SEARCH CUSTOMERS - PAGINATED
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> searchCustomers(
            String keyword,
            Pageable pageable
    ) {

        if (keyword == null
                || keyword.isBlank()) {

            return getAllCustomers(pageable);
        }


        Page<User> customers =
                userRepository.searchCustomers(
                        keyword.trim(),
                        CUSTOMER_ROLE,
                        pageable
                );


        return customers.map(
                this::mapToSummaryResponse
        );
    }


    // ============================================================
    // GET CUSTOMER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(
            Long customerId
    ) {

        User customer =
                getCustomer(customerId);

        return mapToResponse(customer);
    }


    // ============================================================
    // UPDATE CUSTOMER
    // ============================================================

    @Override
    @Transactional
    public CustomerResponse updateCustomer(
            Long customerId,
            CustomerUpdateRequest request
    ) {

        User customer =
                getCustomer(customerId);


        if (request.name() != null
                && !request.name().isBlank()) {

            customer.setName(
                    request.name().trim()
            );
        }


        /*
         * Phone does NOT need to be unique.
         */
        if (request.phone() != null
                && !request.phone().isBlank()) {

            customer.setPhone(
                    request.phone()
            );
        }


        if (request.alternatePhone() != null) {

            customer.setAlternatePhone(
                    request.alternatePhone()
            );
        }


        if (request.email() != null) {

            String email =
                    normalizeEmail(
                            request.email()
                    );


            if (email != null
                    && !email.equals(
                    customer.getEmail()
            )) {

                if (userRepository.existsByEmail(email)) {

                    throw new IllegalArgumentException(
                            "Email already belongs to another user"
                    );
                }
            }

            customer.setEmail(email);
        }


        if (request.address() != null) {

            customer.setAddress(
                    request.address()
            );
        }


        if (request.city() != null) {

            customer.setCity(
                    request.city()
            );
        }


        if (request.notes() != null) {

            customer.setNotes(
                    request.notes()
            );
        }


        User updatedCustomer =
                userRepository.save(customer);


        return mapToResponse(updatedCustomer);
    }


    // ============================================================
    // DELETE CUSTOMER
    // ============================================================

    @Override
    @Transactional
    public void deleteCustomer(
            Long customerId
    ) {

        User customer =
                getCustomer(customerId);

        userRepository.delete(customer);
    }


    // ============================================================
    // GET CUSTOMER
    // ============================================================

    private User getCustomer(
            Long customerId
    ) {

        User user =
                userRepository
                        .findById(customerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Customer not found with id: "
                                                + customerId
                                )
                        );


        boolean isCustomer =
                user.getRoles()
                        .stream()
                        .anyMatch(role ->
                                CUSTOMER_ROLE.equals(
                                        role.getName()
                                )
                        );


        if (!isCustomer) {

            throw new ResourceNotFoundException(
                    "Customer not found with id: "
                            + customerId
            );
        }


        return user;
    }


    // ============================================================
    // GET CUSTOMER ROLE
    // ============================================================

    private Role getCustomerRole() {

        return roleRepository
                .findByName(CUSTOMER_ROLE)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "ROLE_CUSTOMER not found"
                        )
                );
    }


    // ============================================================
    // RESPONSE MAPPING
    // ============================================================

    private CustomerResponse mapToResponse(
            User user
    ) {

        return new CustomerResponse(
                user.getId(),
                user.getCustomerCode(),
                user.getName(),
                user.getPhone(),
                user.getAlternatePhone(),
                user.getEmail(),
                user.getImage(),
                user.getAddress(),
                user.getCity(),
                user.getNotes(),
                user.isEnable(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }


    private CustomerSummaryResponse mapToSummaryResponse(
            User user
    ) {

        return new CustomerSummaryResponse(
                user.getId(),
                user.getCustomerCode(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getImage()
        );
    }


    // ============================================================
    // EMAIL NORMALIZATION
    // ============================================================

    private String normalizeEmail(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            return null;
        }

        return email
                .trim()
                .toLowerCase();
    }
}