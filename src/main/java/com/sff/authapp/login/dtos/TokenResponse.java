package com.sff.authapp.login.dtos;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        LoginResponse user
) {

    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn,  LoginResponse user ) {
        return  new TokenResponse(accessToken,refreshToken, expiresIn,user);
    }

}
