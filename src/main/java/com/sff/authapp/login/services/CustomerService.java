package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.customer.CustomerCreateRequest;
import com.sff.authapp.login.dtos.customer.CustomerResponse;
import com.sff.authapp.login.dtos.customer.CustomerSummaryResponse;
import com.sff.authapp.login.dtos.customer.CustomerUpdateRequest;
import com.sff.authapp.login.dtos.customer.CustomerProfileResponse;
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

    CustomerProfileResponse getCustomerProfile(
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
    //    Set a new Customer code to a user
    CustomerResponse setNewCustomerCode(
            Long customerId
    );

    // ============================================================
    // CUSTOMER IMAGES & PROFILE PICTURE
    // ============================================================

    com.sff.authapp.login.dtos.customer.CustomerResponse updateProfilePicture(
            Long customerId,
            com.sff.authapp.login.dtos.customer.CustomerProfilePictureRequest request
    );

    com.sff.authapp.login.dtos.customer.CustomerResponse removeProfilePicture(
            Long customerId
    );

    java.util.List<com.sff.authapp.login.dtos.customer.UserImageDto> getCustomerImages(
            Long customerId
    );

    com.sff.authapp.login.dtos.customer.UserImageDto addCustomerImage(
            Long customerId,
            com.sff.authapp.login.dtos.customer.UserImageCreateRequest request
    );

    void deleteCustomerImage(
            Long customerId,
            Long imageId
    );

}