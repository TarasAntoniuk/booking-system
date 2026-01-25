package com.tarasantoniuk.initialization.service;

import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataInitializationService Unit Tests")
class DataInitializationServiceTest {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private DataInitializationService dataInitializationService;

    private List<User> testUsers;
    private List<Unit> testUnits;

    @BeforeEach
    void setUp() {
        testUsers = new ArrayList<>();
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        testUsers.add(user1);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        testUsers.add(user2);

        testUnits = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Unit unit = new Unit();
            unit.setId((long) i);
            unit.setNumberOfRooms(2);
            unit.setAccommodationType(AccommodationType.FLAT);
            unit.setFloor(3);
            unit.setBaseCost(BigDecimal.valueOf(100));
            unit.setDescription("Test unit " + i);
            unit.setOwner(user1);
            testUnits.add(unit);
        }
    }

    @Test
    @DisplayName("Should skip initialization when units already exist (100+)")
    void shouldSkipInitializationWhenUnitsAlreadyExist() {
        // Given
        when(unitRepository.count()).thenReturn(100L);

        // When
        dataInitializationService.initializeData();

        // Then
        verify(unitRepository).count();
        verify(userRepository, never()).findAll();
        verify(unitRepository, never()).saveAll(anyList());
        verify(eventService, never()).createEvent(any(), any());
    }

    @Test
    @DisplayName("Should skip initialization when no users found")
    void shouldSkipInitializationWhenNoUsersFound() {
        // Given
        when(unitRepository.count()).thenReturn(10L);
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        // When
        dataInitializationService.initializeData();

        // Then
        verify(unitRepository).count();
        verify(userRepository).findAll();
        verify(unitRepository, never()).saveAll(anyList());
        verify(eventService, never()).createEvent(any(), any());
    }

    @Test
    @DisplayName("Should initialize data successfully when less than 100 units exist")
    void shouldInitializeDataSuccessfully() {
        // Given
        when(unitRepository.count()).thenReturn(10L).thenReturn(100L);
        when(userRepository.findAll()).thenReturn(testUsers);
        when(unitRepository.saveAll(anyList())).thenReturn(testUnits);

        // When
        dataInitializationService.initializeData();

        // Then
        verify(unitRepository, times(2)).count();
        verify(userRepository).findAll();
        verify(unitRepository).saveAll(argThat(units -> {
            assertThat(units).hasSize(90);

            units.forEach(unit -> {
                assertThat(unit.getNumberOfRooms()).isBetween(1, 5);
                assertThat(unit.getAccommodationType()).isIn((Object[]) AccommodationType.values());
                assertThat(unit.getFloor()).isBetween(1, 10);
                assertThat(unit.getBaseCost()).isBetween(BigDecimal.valueOf(50), BigDecimal.valueOf(500));
                assertThat(unit.getDescription()).isNotEmpty();
                assertThat(unit.getOwner()).isIn(testUsers);
            });

            return true;
        }));

        verify(eventService, times(10)).createEvent(eq(EventType.UNIT_CREATED), anyLong());
    }

    @Test
    @DisplayName("Should create exactly correct number of units to reach 100")
    void shouldCreateCorrectNumberOfUnits() {
        // Given
        when(unitRepository.count()).thenReturn(50L).thenReturn(100L);
        when(userRepository.findAll()).thenReturn(testUsers);
        when(unitRepository.saveAll(anyList())).thenReturn(testUnits);

        // When
        dataInitializationService.initializeData();

        // Then
        verify(unitRepository).saveAll(argThat(units -> {
            assertThat(units).hasSize(50);
            return true;
        }));
    }

    @Test
    @DisplayName("Should generate units with varied properties")
    void shouldGenerateUnitsWithVariedProperties() {
        // Given
        when(unitRepository.count()).thenReturn(0L).thenReturn(100L);
        when(userRepository.findAll()).thenReturn(testUsers);

        List<Unit> capturedUnits = new ArrayList<>();
        when(unitRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Unit> units = invocation.getArgument(0);
            capturedUnits.addAll(units);
            return testUnits.subList(0, Math.min(units.size(), testUnits.size()));
        });

        // When
        dataInitializationService.initializeData();

        // Then
        assertThat(capturedUnits).hasSize(100);

        long distinctRoomCounts = capturedUnits.stream()
                .map(Unit::getNumberOfRooms)
                .distinct()
                .count();
        assertThat(distinctRoomCounts).isGreaterThan(1);

        long distinctTypes = capturedUnits.stream()
                .map(Unit::getAccommodationType)
                .distinct()
                .count();
        assertThat(distinctTypes).isGreaterThan(1);

        assertThat(capturedUnits).allMatch(unit -> unit.getDescription() != null && !unit.getDescription().isEmpty());
    }

    @Test
    @DisplayName("Should distribute units among all available users")
    void shouldDistributeUnitsAmongUsers() {
        // Given
        User user3 = new User();
        user3.setId(3L);
        user3.setUsername("user3");
        testUsers.add(user3);

        when(unitRepository.count()).thenReturn(0L).thenReturn(100L);
        when(userRepository.findAll()).thenReturn(testUsers);

        List<Unit> capturedUnits = new ArrayList<>();
        when(unitRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Unit> units = invocation.getArgument(0);
            capturedUnits.addAll(units);
            return testUnits.subList(0, Math.min(units.size(), testUnits.size()));
        });

        // When
        dataInitializationService.initializeData();

        // Then
        long distinctOwners = capturedUnits.stream()
                .map(Unit::getOwner)
                .distinct()
                .count();
        assertThat(distinctOwners).isGreaterThan(1);
    }
}
