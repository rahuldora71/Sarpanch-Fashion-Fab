package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.UserDto;
import com.sff.authapp.login.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("@perm.can('users', 'CREATE') or @perm.isAdmin()")
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
    }

    @GetMapping
    @PreAuthorize("@perm.can('users', 'VIEW') or @perm.isAdmin()")
    public ResponseEntity<Iterable<UserDto>> getAllUser() {
        Iterable<UserDto> users = userService.getAllUsers();
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    @GetMapping("/test")
    public String test() {
        return "Working";
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("@perm.can('users', 'VIEW') or @perm.isAdmin()")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable("email") String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@perm.can('users', 'DELETE') or @perm.isAdmin()")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@perm.can('users', 'EDIT') or @perm.isAdmin()")
    public ResponseEntity<UserDto> updateUser(@RequestBody UserDto userDto, @PathVariable Long userId) {
        return ResponseEntity.ok(userService.updateUser(userDto, userId));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@perm.can('users', 'VIEW') or @perm.isAdmin() or @perm.isCurrentUser(#userId)")
    public ResponseEntity<UserDto> getUserById(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}
