package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.customer.CustomerCreateRequest;
import com.sff.authapp.login.dtos.customer.CustomerResponse;
import com.sff.authapp.login.dtos.customer.CustomerSummaryResponse;
import com.sff.authapp.login.dtos.customer.CustomerUpdateRequest;
import com.sff.authapp.login.services.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;


    // ============================================================
    // CREATE
    // ============================================================

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid
            @RequestBody
            CustomerCreateRequest request
    ) {

        CustomerResponse response =
                customerService.createCustomer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // GET ALL - PAGINATED
    // ============================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Page<CustomerSummaryResponse>> getAllCustomers(
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                customerService.getAllCustomers(pageable)
        );
    }


    // ============================================================
    // SEARCH - PAGINATED
    // ============================================================

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Page<CustomerSummaryResponse>> searchCustomers(
            @RequestParam String keyword,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                customerService.searchCustomers(
                        keyword,
                        pageable
                )
        );
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @GetMapping("/{customerId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<CustomerResponse>
    getCustomerById(
            @PathVariable Long customerId
    ) {

        return ResponseEntity.ok(
                customerService.getCustomerById(
                        customerId
                )
        );
    }


    // ============================================================
    // UPDATE
    // ============================================================

    @PutMapping("/{customerId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN', 'WORKER')"
    )
    public ResponseEntity<CustomerResponse>
    updateCustomer(

            @PathVariable Long customerId,

            @Valid
            @RequestBody
            CustomerUpdateRequest request

    ) {

        return ResponseEntity.ok(
                customerService.updateCustomer(
                        customerId,
                        request
                )
        );
    }


    // ============================================================
    // DELETE
    // ============================================================

    @DeleteMapping("/{customerId}")
    @PreAuthorize(
            "hasAnyRole('OWNER', 'ADMIN')"
    )
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long customerId
    ) {

        customerService.deleteCustomer(
                customerId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}