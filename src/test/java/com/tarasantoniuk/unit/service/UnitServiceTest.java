package com.tarasantoniuk.unit.service;

import com.tarasantoniuk.common.TestFixtures;
import com.tarasantoniuk.common.exception.ResourceNotFoundException;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.statistic.service.UnitStatisticsService;
import com.tarasantoniuk.unit.dto.CreateUnitRequestDto;
import com.tarasantoniuk.unit.dto.UnitResponseDto;
import com.tarasantoniuk.unit.dto.UnitSearchCriteriaDto;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnitService Unit Tests")
class UnitServiceTest {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventService eventService;

    @Mock
    private UnitStatisticsService unitStatisticsService;

    @InjectMocks
    private UnitService unitService;

    private User testUser;
    private Unit testUnit;

    @BeforeEach
    void setUp() {
        testUser = TestFixtures.createTestUser();
        testUnit = TestFixtures.createTestUnit();
        testUnit.setOwner(testUser);
    }

    @Test
    @DisplayName("Should create unit successfully")
    void shouldCreateUnitSuccessfully() {
        // Given
        CreateUnitRequestDto request = new CreateUnitRequestDto();
        request.setNumberOfRooms(2);
        request.setAccommodationType(AccommodationType.FLAT);
        request.setFloor(3);
        request.setBaseCost(BigDecimal.valueOf(100));
        request.setDescription("New unit");
        request.setOwnerId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(unitRepository.save(any(Unit.class))).thenReturn(testUnit);

        // When
        UnitResponseDto response = unitService.createUnit(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNumberOfRooms()).isEqualTo(2);
        assertThat(response.getTotalCost()).isEqualByComparingTo(BigDecimal.valueOf(115.00)); // 100 + 15%

        verify(userRepository).findById(1L);
        verify(unitRepository).save(any(Unit.class));
        verify(eventService).createEvent(EventType.UNIT_CREATED, 1L);
        verify(unitStatisticsService).invalidateAvailableUnitsCache();
    }

    @Test
    @DisplayName("Should throw exception when owner not found")
    void shouldThrowExceptionWhenOwnerNotFound() {
        // Given
        CreateUnitRequestDto request = new CreateUnitRequestDto();
        request.setOwnerId(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> unitService.createUnit(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Owner not found");

        verify(unitRepository, never()).save(any(Unit.class));
        verify(eventService, never()).createEvent(any(), any());
    }

    @Test
    @DisplayName("Should get unit by id successfully")
    void shouldGetUnitByIdSuccessfully() {
        // Given
        when(unitRepository.findById(1L)).thenReturn(Optional.of(testUnit));

        // When
        UnitResponseDto response = unitService.getUnitById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNumberOfRooms()).isEqualTo(2);
        verify(unitRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when unit not found")
    void shouldThrowExceptionWhenUnitNotFound() {
        // Given
        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> unitService.getUnitById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Unit not found");
    }

    @Test
    @DisplayName("Should search units with criteria")
    void shouldSearchUnitsWithCriteria() {
        // Given
        UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
        criteria.setNumberOfRooms(2);
        criteria.setAccommodationType(AccommodationType.FLAT);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Unit> unitPage = new PageImpl<>(List.of(testUnit), pageable, 1);

        when(unitRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(unitPage);

        // When
        Page<UnitResponseDto> response = unitService.searchUnits(criteria, pageable);

        // Then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getNumberOfRooms()).isEqualTo(2);
        verify(unitRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Should map unit with null owner to response with null ownerId")
    void shouldMapUnitWithNullOwnerToResponse() {
        // Given
        Unit unitWithoutOwner = new Unit();
        unitWithoutOwner.setId(2L);
        unitWithoutOwner.setNumberOfRooms(1);
        unitWithoutOwner.setAccommodationType(AccommodationType.FLAT);
        unitWithoutOwner.setFloor(1);
        unitWithoutOwner.setBaseCost(BigDecimal.valueOf(80));
        unitWithoutOwner.setOwner(null);

        when(unitRepository.findById(2L)).thenReturn(Optional.of(unitWithoutOwner));

        // When
        UnitResponseDto response = unitService.getUnitById(2L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getOwnerId()).isNull();
    }

    @Test
    @DisplayName("Should get all units with pagination")
    void shouldGetAllUnitsWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Unit> unitPage = new PageImpl<>(List.of(testUnit), pageable, 1);

        when(unitRepository.findAll(pageable)).thenReturn(unitPage);

        // When
        Page<UnitResponseDto> response = unitService.getAllUnits(pageable);

        // Then
        assertThat(response.getContent()).hasSize(1);
        verify(unitRepository).findAll(pageable);
    }
}