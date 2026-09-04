package com.sff.authapp.login.dtos;

public record LoginRequest(
        String email,
        String password
) {
}
