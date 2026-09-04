package com.sff.authapp.login.dtos.customer;

public record CustomerSummaryResponse(

        Long id,

        String customerCode,

        String name,

        String phone,

        String email,

        String image


) {
}