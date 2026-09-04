package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.LoginResponse;
import com.sff.authapp.login.dtos.UserDto;
import com.sff.authapp.login.services.AuthService;
import com.sff.authapp.login.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private  final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse registerUser(UserDto userDto) {

        UserDto normalizedUserDto = new UserDto(
                userDto.id(),
                userDto.email(),
                userDto.name(),
                passwordEncoder.encode(userDto.password()),
                userDto.image(),
                true,
                userDto.createdAt(),
                userDto.updatedAt(),
                userDto.provider(),
                userDto.roles()
        );

        UserDto createdUser = userService.createUser(normalizedUserDto);

        return new LoginResponse(
                createdUser.id(),
                createdUser.email(),
                createdUser.name(),
                createdUser.image(),
                createdUser.enable(),
                createdUser.createdAt(),
                createdUser.updatedAt(),
                createdUser.provider(),
                createdUser.roles()
        );

    }
}
