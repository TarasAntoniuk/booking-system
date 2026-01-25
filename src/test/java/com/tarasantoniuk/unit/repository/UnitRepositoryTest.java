package com.tarasantoniuk.unit.repository;

import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository layer tests for UnitRepository using H2 in-memory database.
 * Tests JPA queries and custom repository methods.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("UnitRepository Tests")
class UnitRepositoryTest {

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    private User testOwner;

    @BeforeEach
    void setUp() {
        // Create test owner
        testOwner = new User();
        testOwner.setUsername("testowner");
        testOwner.setEmail("owner@test.com");
        testOwner = userRepository.save(testOwner);
    }

    @Test
    @DisplayName("Should save and find unit by id")
    void shouldSaveAndFindUnitById() {
        // Given
        Unit unit = createUnit(2, AccommodationType.FLAT, 100.0);

        // When
        Unit saved = unitRepository.save(unit);
        Unit found = unitRepository.findById(saved.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getNumberOfRooms()).isEqualTo(2);
        assertThat(found.getAccommodationType()).isEqualTo(AccommodationType.FLAT);
    }

    @Test
    @DisplayName("Should find all units with pagination")
    void shouldFindAllUnitsWithPagination() {
        // Given
        unitRepository.save(createUnit(1, AccommodationType.FLAT, 80.0));
        unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));
        unitRepository.save(createUnit(3, AccommodationType.APARTMENTS, 150.0));

        // When
        Page<Unit> page = unitRepository.findAll(PageRequest.of(0, 2));

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find units by owner id")
    void shouldFindUnitsByOwnerId() {
        // Given
        unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));
        unitRepository.save(createUnit(3, AccommodationType.FLAT, 120.0));

        // When
        List<Unit> units = unitRepository.findAll();

        // Then
        assertThat(units).hasSize(2);
        assertThat(units).allMatch(u -> u.getOwner().getId().equals(testOwner.getId()));
    }

    @Test
    @DisplayName("Should calculate total cost correctly")
    void shouldCalculateTotalCostCorrectly() {
        // Given
        Unit unit = createUnit(2, AccommodationType.FLAT, 100.0);

        // When
        Unit saved = unitRepository.save(unit);

        // Then
        BigDecimal expectedTotal = new BigDecimal("115.00"); // 100 * 1.15
        assertThat(saved.getTotalCost()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("Should persist all unit properties")
    void shouldPersistAllUnitProperties() {
        // Given
        Unit unit = createUnit(3, AccommodationType.APARTMENTS, 200.0);
        unit.setFloor(5);
        unit.setDescription("Luxury apartment with great view");

        // When
        Unit saved = unitRepository.save(unit);
        unitRepository.flush();
        Unit found = unitRepository.findById(saved.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getNumberOfRooms()).isEqualTo(3);
        assertThat(found.getAccommodationType()).isEqualTo(AccommodationType.APARTMENTS);
        assertThat(found.getFloor()).isEqualTo(5);
        assertThat(found.getBaseCost()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(found.getDescription()).isEqualTo("Luxury apartment with great view");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    private Unit createUnit(int rooms, AccommodationType type, double baseCost) {
        Unit unit = new Unit();
        unit.setOwner(testOwner);
        unit.setNumberOfRooms(rooms);
        unit.setAccommodationType(type);
        unit.setFloor(1);
        unit.setBaseCost(new BigDecimal(baseCost));
        unit.setDescription("Test unit");
        return unit;
    }
}