package com.tarasantoniuk.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.unit.dto.CreateUnitRequestDto;
import com.tarasantoniuk.unit.dto.UnitResponseDto;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.unit.service.UnitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UnitController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UnitController Unit Tests")
class UnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UnitService unitService;

    @Test
    @DisplayName("Should create unit and return 201")
    void shouldCreateUnitAndReturn201() throws Exception {
        // Given
        CreateUnitRequestDto request = new CreateUnitRequestDto();
        request.setNumberOfRooms(2);
        request.setAccommodationType(AccommodationType.FLAT);
        request.setFloor(3);
        request.setBaseCost(BigDecimal.valueOf(100));
        request.setDescription("Test unit");
        request.setOwnerId(1L);

        UnitResponseDto response = new UnitResponseDto();
        response.setId(1L);
        response.setNumberOfRooms(2);
        response.setAccommodationType(AccommodationType.FLAT);
        response.setFloor(3);
        response.setBaseCost(BigDecimal.valueOf(100));
        response.setTotalCost(BigDecimal.valueOf(115));
        response.setOwnerId(1L);
        response.setCreatedAt(LocalDateTime.now());

        when(unitService.createUnit(any(CreateUnitRequestDto.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numberOfRooms").value(2))
                .andExpect(jsonPath("$.totalCost").value(115));

        verify(unitService).createUnit(any(CreateUnitRequestDto.class));
    }

    @Test
    @DisplayName("Should get unit by id")
    void shouldGetUnitById() throws Exception {
        // Given
        UnitResponseDto response = new UnitResponseDto();
        response.setId(1L);
        response.setNumberOfRooms(2);
        response.setAccommodationType(AccommodationType.FLAT);

        when(unitService.getUnitById(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/units/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numberOfRooms").value(2));

        verify(unitService).getUnitById(1L);
    }

    @Test
    @DisplayName("Should get all units with pagination")
    void shouldGetAllUnitsWithPagination() throws Exception {
        // Given
        UnitResponseDto unit1 = new UnitResponseDto();
        unit1.setId(1L);
        unit1.setNumberOfRooms(2);

        UnitResponseDto unit2 = new UnitResponseDto();
        unit2.setId(2L);
        unit2.setNumberOfRooms(3);

        Page<UnitResponseDto> page = new PageImpl<>(List.of(unit1, unit2), PageRequest.of(0, 10), 2);

        when(unitService.getAllUnits(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/units")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(unitService).getAllUnits(any());
    }

    @Test
    @DisplayName("Should search units with criteria")
    void shouldSearchUnitsWithCriteria() throws Exception {
        // Given
        UnitResponseDto unit = new UnitResponseDto();
        unit.setId(1L);
        unit.setNumberOfRooms(2);
        unit.setAccommodationType(AccommodationType.FLAT);

        Page<UnitResponseDto> page = new PageImpl<>(List.of(unit), PageRequest.of(0, 20), 1);

        when(unitService.searchUnits(any(), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/units/search")
                        .param("numberOfRooms", "2")
                        .param("accommodationType", "FLAT")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].numberOfRooms").value(2));

        verify(unitService).searchUnits(any(), any());
    }

    @Test
    @DisplayName("Should get all units with custom sorting ascending")
    void shouldGetAllUnitsWithCustomSortingAscending() throws Exception {
        // Given
        Page<UnitResponseDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(unitService.getAllUnits(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/units")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "baseCost")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk());

        verify(unitService).getAllUnits(any());
    }

    @Test
    @DisplayName("Should get all units with custom sorting descending")
    void shouldGetAllUnitsWithCustomSortingDescending() throws Exception {
        // Given
        Page<UnitResponseDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(unitService.getAllUnits(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/units")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "numberOfRooms")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk());

        verify(unitService).getAllUnits(any());
    }

    @Test
    @DisplayName("Should use default pagination parameters")
    void shouldUseDefaultPaginationParameters() throws Exception {
        // Given
        Page<UnitResponseDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(unitService.getAllUnits(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/units"))
                .andExpect(status().isOk());

        verify(unitService).getAllUnits(any());
    }

    @Test
    @DisplayName("Should search units with sorting")
    void shouldSearchUnitsWithSorting() throws Exception {
        // Given
        Page<UnitResponseDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(unitService.searchUnits(any(), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/units/search")
                        .param("floor", "3")
                        .param("sortBy", "baseCost")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk());

        verify(unitService).searchUnits(any(), any());
    }
}