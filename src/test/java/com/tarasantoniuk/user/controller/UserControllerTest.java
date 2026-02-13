package com.tarasantoniuk.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.user.dto.UserRequestDto;
import com.tarasantoniuk.user.dto.UserResponseDto;
import com.tarasantoniuk.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        mockMvc.perform(post("/api/v1/users")
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
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).getUserById(1L);
    }

    @Test
    @DisplayName("Should get all users with pagination")
    void shouldGetAllUsers() throws Exception {
        // Given
        List<UserResponseDto> users = List.of(
                new UserResponseDto(1L, "user1", "user1@example.com", LocalDateTime.now()),
                new UserResponseDto(2L, "user2", "user2@example.com", LocalDateTime.now())
        );
        Page<UserResponseDto> page = new PageImpl<>(users, PageRequest.of(0, 20), 2);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].username").value("user1"))
                .andExpect(jsonPath("$.content[1].username").value("user2"))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(userService).getAllUsers(any(Pageable.class));
    }

    @Test
    @DisplayName("Should cap page size at MAX_PAGE_SIZE for all users")
    void shouldCapPageSizeForAllUsers() throws Exception {
        // Given
        Page<UserResponseDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(emptyPage);

        // When
        mockMvc.perform(get("/api/v1/users")
                        .param("size", "500"))
                .andExpect(status().isOk());

        // Then
        verify(userService).getAllUsers(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should use custom pagination params for all users")
    void shouldUseCustomPaginationParamsForAllUsers() throws Exception {
        // Given
        Page<UserResponseDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 5), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(emptyPage);

        // When
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "username")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk());

        // Then
        verify(userService).getAllUsers(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(5);
        assertThat(captured.getSort().getOrderFor("username")).isNotNull();
        assertThat(captured.getSort().getOrderFor("username").isDescending()).isTrue();
    }

    @Test
    @DisplayName("Should get user by username")
    void shouldGetUserByUsername() throws Exception {
        // Given
        UserResponseDto response = new UserResponseDto(1L, "testuser", "test@example.com", LocalDateTime.now());
        when(userService.getUserByUsername("testuser")).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/users/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).getUserByUsername("testuser");
    }
}