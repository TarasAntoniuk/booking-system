package com.tarasantoniuk.initialization.service;

import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService {

    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final EventService eventService;

    private final Random random = new Random();

    @PostConstruct
    @Transactional
    public void initializeData() {
        // Check if we already have units (10 from Liquibase)
        long existingUnits = unitRepository.count();

        if (existingUnits >= 100) {
            log.info("Data already initialized. Found {} units in database.", existingUnits);
            return;
        }

        log.info("Starting data initialization. Current units count: {}", existingUnits);

        // Get existing users (3 users from Liquibase)
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            log.error("No users found in database. Cannot initialize units.");
            return;
        }

        // Generate 90 additional units
        int unitsToCreate = 100 - (int) existingUnits;
        List<Unit> newUnits = new ArrayList<>();

        for (int i = 0; i < unitsToCreate; i++) {
            Unit unit = generateRandomUnit(users);
            newUnits.add(unit);
        }

        // Save all units
        List<Unit> savedUnits = unitRepository.saveAll(newUnits);

        // Create events for each unit
        for (Unit unit : savedUnits) {
            eventService.createEvent(EventType.UNIT_CREATED, unit.getId());
        }

        log.info("Data initialization completed. Created {} new units. Total units: {}",
                savedUnits.size(), unitRepository.count());
    }

    private Unit generateRandomUnit(List<User> users) {
        Unit unit = new Unit();

        // Random number of rooms (1-5)
        unit.setNumberOfRooms(random.nextInt(5) + 1);

        // Random accommodation type
        AccommodationType[] types = AccommodationType.values();
        unit.setAccommodationType(types[random.nextInt(types.length)]);

        // Random floor (1-10)
        unit.setFloor(random.nextInt(10) + 1);

        // Random cost (50-500)
        double cost = 50 + (random.nextDouble() * 450);
        unit.setBaseCost(BigDecimal.valueOf(Math.round(cost * 100.0) / 100.0));

        // Random description
        unit.setDescription(generateDescription(unit));

        // Random owner from existing users
        unit.setOwner(users.get(random.nextInt(users.size())));

        return unit;
    }

    private String generateDescription(Unit unit) {
        String[] adjectives = {
                "Cozy", "Spacious", "Modern", "Comfortable", "Luxurious",
                "Bright", "Quiet", "Central", "Beautiful", "Charming"
        };

        String[] features = {
                "with balcony", "near metro", "with parking", "newly renovated",
                "with great view", "fully furnished", "with garden", "pet-friendly",
                "with terrace", "close to city center"
        };

        String adjective = adjectives[random.nextInt(adjectives.length)];
        String feature = features[random.nextInt(features.length)];

        return String.format("%s %d-room %s %s",
                adjective,
                unit.getNumberOfRooms(),
                unit.getAccommodationType().toString().toLowerCase(),
                feature
        );
    }
}