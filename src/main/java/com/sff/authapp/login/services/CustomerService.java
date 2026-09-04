package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.customer.CustomerCreateRequest;
import com.sff.authapp.login.dtos.customer.CustomerResponse;
import com.sff.authapp.login.dtos.customer.CustomerSummaryResponse;
import com.sff.authapp.login.dtos.customer.CustomerUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponse createCustomer(
            CustomerCreateRequest request
    );

    Page<CustomerSummaryResponse> getAllCustomers(
            Pageable pageable
    );

    CustomerResponse getCustomerById(
            Long customerId
    );

    CustomerResponse updateCustomer(
            Long customerId,
            CustomerUpdateRequest request
    );

    void deleteCustomer(
            Long customerId
    );

    Page<CustomerSummaryResponse> searchCustomers(
            String keyword,
            Pageable pageable
    );
}