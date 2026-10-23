package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.customer.CustomerCreateRequest;
import com.sff.authapp.login.dtos.customer.CustomerResponse;
import com.sff.authapp.login.dtos.customer.CustomerProfileResponse;
import com.sff.authapp.login.dtos.customer.CustomerSummaryResponse;
import com.sff.authapp.login.dtos.customer.CustomerUpdateRequest;
import com.sff.authapp.login.dtos.order.OrderDetailsResponse;
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
//        print the pageable object for debugging
        System.out.println("Pageable: " + pageable);
        Page<CustomerSummaryResponse> allCustomers = customerService.getAllCustomers(pageable);
        System.out.println("All Customers: " + allCustomers.getContent().stream().iterator().next());
        return ResponseEntity.ok(
                allCustomers
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
    // CUSTOMER PROFILE
    // ============================================================

    @GetMapping("/{customerId}/profile")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<CustomerProfileResponse> getCustomerProfile(
            @PathVariable Long customerId
    ) {

        return ResponseEntity.ok(
                customerService.getCustomerProfile(customerId)
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


    // ============================================================
    // PROFILE PICTURE
    // ============================================================

    @PutMapping("/{customerId}/profile-picture")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<CustomerResponse> updateProfilePicture(
            @PathVariable Long customerId,
            @Valid @RequestBody com.sff.authapp.login.dtos.customer.CustomerProfilePictureRequest request
    ) {
        return ResponseEntity.ok(
                customerService.updateProfilePicture(customerId, request)
        );
    }

    @DeleteMapping("/{customerId}/profile-picture")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<CustomerResponse> removeProfilePicture(
            @PathVariable Long customerId
    ) {
        return ResponseEntity.ok(
                customerService.removeProfilePicture(customerId)
        );
    }


    // ============================================================
    // CUSTOMER IMAGES (POSTURE / FITTING / REFERENCE)
    // ============================================================

    @GetMapping("/{customerId}/images")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<java.util.List<com.sff.authapp.login.dtos.customer.UserImageDto>> getCustomerImages(
            @PathVariable Long customerId
    ) {
        return ResponseEntity.ok(
                customerService.getCustomerImages(customerId)
        );
    }

    @PostMapping("/{customerId}/images")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<com.sff.authapp.login.dtos.customer.UserImageDto> addCustomerImage(
            @PathVariable Long customerId,
            @Valid @RequestBody com.sff.authapp.login.dtos.customer.UserImageCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                customerService.addCustomerImage(customerId, request)
        );
    }

    @DeleteMapping("/{customerId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Void> deleteCustomerImage(
            @PathVariable Long customerId,
            @PathVariable Long imageId
    ) {
        customerService.deleteCustomerImage(customerId, imageId);
        return ResponseEntity.noContent().build();
    }
}