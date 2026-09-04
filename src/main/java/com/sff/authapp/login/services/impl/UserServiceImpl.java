package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.UserDto;
import com.sff.authapp.login.entities.Provider;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.UserRepository;
//import jakarta.transaction.Transactional;
import com.sff.authapp.login.services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {

        if (userDto.email() == null || userDto.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.existsByEmail(userDto.email())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        User user = new User();

        user.setEmail(userDto.email());
        user.setName(userDto.name());
        user.setPassword(userDto.password());
        user.setImage(userDto.image());
        user.setEnable(true);

        user.setProvider(
                userDto.provider() != null
                        ? userDto.provider()
                        : Provider.LOCAL
        );

        User savedUser = userRepository.save(user);

        return new UserDto(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getPassword(),
                savedUser.getImage(),
                savedUser.isEnable(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt(),
                savedUser.getProvider(),
                null
        );
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given email Id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, Long userId) {
        User existingUser = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
//       we are not going to update the email id for this project
        if (userDto.name() != null) existingUser.setName(userDto.name());
        if (userDto.image() != null) existingUser.setImage(userDto.image());
        if (userDto.provider() != null) existingUser.setProvider(userDto.provider());
//        TODO:change the password updation loading
        if (userDto.password() != null) existingUser.setPassword(userDto.password());
        existingUser.setEnable(userDto.enable());
        existingUser.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(existingUser);

        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
        userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Iterable<UserDto> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
//                .collect(Collectors.toList());
    }
}
