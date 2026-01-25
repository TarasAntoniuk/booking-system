--liquibase formatted sql

--changeset taras:7-insert-test-users
INSERT INTO users (username, email) VALUES
    ('admin', 'admin@booking.com'),
    ('john_doe', 'john@example.com'),
    ('jane_smith', 'jane@example.com');

--changeset taras:8-insert-test-units
INSERT INTO units (number_of_rooms, accommodation_type, floor, base_cost, description, owner_id) VALUES
    (2, 'FLAT', 3, 100.00, 'Cozy 2-room flat on 3rd floor', 1),
    (3, 'APARTMENTS', 5, 150.00, 'Spacious 3-room apartment with balcony', 1),
    (1, 'HOME', 1, 80.00, 'Small house with garden', 2),
    (4, 'APARTMENTS', 2, 200.00, 'Luxury 4-room apartment', 2),
    (2, 'FLAT', 1, 90.00, 'Ground floor flat, pet-friendly', 3),
    (3, 'HOME', 1, 180.00, 'Family home with parking', 3),
    (1, 'FLAT', 4, 70.00, 'Studio flat, city center', 1),
    (2, 'APARTMENTS', 6, 120.00, 'Modern apartment with great view', 2),
    (5, 'HOME', 2, 250.00, 'Large house, perfect for families', 3),
    (3, 'FLAT', 2, 130.00, 'Comfortable flat near metro', 1);

--changeset taras:9-insert-unit-events
INSERT INTO events (event_type, entity_id, event_data)
SELECT 'UNIT_CREATED', id, CONCAT('{"unitId":', id, ',"type":"', accommodation_type, '"}')
FROM units;
