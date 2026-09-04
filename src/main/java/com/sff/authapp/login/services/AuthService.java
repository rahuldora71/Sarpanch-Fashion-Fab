package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.LoginResponse;
import com.sff.authapp.login.dtos.UserDto;

public interface AuthService {
    LoginResponse registerUser(UserDto userDto);

//    Login User
}
