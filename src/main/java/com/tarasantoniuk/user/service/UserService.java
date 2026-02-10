package com.tarasantoniuk.user.service;

import com.tarasantoniuk.common.exception.ResourceNotFoundException;
import com.tarasantoniuk.user.dto.UserRequestDto;
import com.tarasantoniuk.user.dto.UserResponseDto;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponseDto createUser(UserRequestDto request) {
        log.info("Creating user: username={}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Duplicate username attempt: {}", request.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Duplicate email attempt: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        User saved = userRepository.save(user);
        log.info("User created successfully: userId={}, username={}", saved.getId(), saved.getUsername());
        return UserResponseDto.from(saved);
    }

    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponseDto.from(user);
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
    }

    public UserResponseDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return UserResponseDto.from(user);
    }
}