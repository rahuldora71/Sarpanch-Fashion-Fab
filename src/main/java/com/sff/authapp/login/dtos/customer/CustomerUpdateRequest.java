package com.sff.authapp.login.dtos.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest(

        @Size(
                max = 400,
                message = "Name cannot exceed 400 characters"
        )
        String name,

        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Phone number must contain exactly 10 digits"
        )
        String phone,

        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Alternate phone number must contain exactly 10 digits"
        )
        String alternatePhone,

        @Email(message = "Invalid email address")
        @Size(
                max = 300,
                message = "Email cannot exceed 300 characters"
        )
        String email,

        @Size(
                max = 255,
                message = "Address cannot exceed 255 characters"
        )
        String address,

        @Size(
                max = 100,
                message = "City cannot exceed 100 characters"
        )
        String city,

        String notes,

        @Size(max = 500, message = "Image URL cannot exceed 500 characters")
        String image,

        @Size(max = 255, message = "Image Public ID cannot exceed 255 characters")
        String imagePublicId
) {

    public CustomerUpdateRequest(
            String name,
            String phone,
            String alternatePhone,
            String email,
            String address,
            String city,
            String notes
    ) {
        this(name, phone, alternatePhone, email, address, city, notes, null, null);
    }
}