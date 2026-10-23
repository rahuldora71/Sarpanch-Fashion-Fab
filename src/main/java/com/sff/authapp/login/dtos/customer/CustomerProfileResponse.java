package com.sff.authapp.login.dtos.customer;

import java.util.List;

public record CustomerProfileResponse(

        CustomerResponse customer,

        CustomerProfileSummary summary,

        List<CustomerProfileOrder> orders,

        List<CustomerProfileMeasurement> measurements,

        List<CustomerProfilePayment> payments,

        List<CustomerProfileAlteration> alterations,

        List<UserImageDto> images

) {
}