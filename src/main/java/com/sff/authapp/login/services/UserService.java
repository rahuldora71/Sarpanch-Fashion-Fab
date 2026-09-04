package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.UserDto;

public interface UserService {
    //create User
    UserDto createUser(UserDto userDto);
    //get user by email id
    UserDto getUserByEmail(String email);
    //update user
    UserDto updateUser(UserDto userDto, Long userId);
//    delete User
    void deleteUser(Long userId);
//    get user by id
    UserDto getUserById(Long id);
//    get all users
    Iterable<UserDto> getAllUsers();

}
