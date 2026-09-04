package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.UserDto;
import com.sff.authapp.login.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {


    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
    }

    //    get All User Api
    @GetMapping
    public ResponseEntity<Iterable<UserDto>> getAllUser() {

        System.out.println("Controller Hit");

        Iterable<UserDto> users = userService.getAllUsers();

//        System.out.println(users);

//        Also set the contnent type while returning

        return ResponseEntity.status(HttpStatus.OK).body(users);
    }
    @GetMapping("/test")
    public String test() {
        System.out.println("TEST ENDPOINT HIT");
        return "Working";
    }
    //    get user by email
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable("email") String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    //delete User
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

//    update user
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@RequestBody UserDto userDto, @PathVariable Long userId) {
         return ResponseEntity.ok(userService.updateUser(userDto,userId));
    }

//     get user by id
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }


}
