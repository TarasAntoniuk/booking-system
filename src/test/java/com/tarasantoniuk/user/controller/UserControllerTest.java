package com.tarasantoniuk.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.user.controller.UserController;
import com.tarasantoniuk.user.dto.UserRequestDto;
import com.tarasantoniuk.user.dto.UserResponseDto;
import com.tarasantoniuk.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Should create user and return 201")
    void shouldCreateUserAndReturn201() throws Exception {
        // Given
        UserRequestDto request = new UserRequestDto("newuser", "newuser@example.com");
        UserResponseDto response = new UserResponseDto(1L, "newuser", "newuser@example.com", LocalDateTime.now());

        when(userService.createUser(any(UserRequestDto.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));

        verify(userService).createUser(any(UserRequestDto.class));
    }

    @Test
    @DisplayName("Should get user by id")
    void shouldGetUserById() throws Exception {
        // Given
        UserResponseDto response = new UserResponseDto(1L, "testuser", "test@example.com", LocalDateTime.now());
        when(userService.getUserById(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).getUserById(1L);
    }

    @Test
    @DisplayName("Should get all users")
    void shouldGetAllUsers() throws Exception {
        // Given
        List<UserResponseDto> users = List.of(
                new UserResponseDto(1L, "user1", "user1@example.com", LocalDateTime.now()),
                new UserResponseDto(2L, "user2", "user2@example.com", LocalDateTime.now())
        );
        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].username").value("user2"));

        verify(userService).getAllUsers();
    }

    @Test
    @DisplayName("Should get user by username")
    void shouldGetUserByUsername() throws Exception {
        // Given
        UserResponseDto response = new UserResponseDto(1L, "testuser", "test@example.com", LocalDateTime.now());
        when(userService.getUserByUsername("testuser")).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/users/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).getUserByUsername("testuser");
    }
}