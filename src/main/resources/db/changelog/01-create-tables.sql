--liquibase formatted sql

--changeset taras:1-create-users-table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--changeset taras:2-create-units-table
CREATE TABLE units (
    id BIGSERIAL PRIMARY KEY,
    number_of_rooms INTEGER NOT NULL,
    accommodation_type VARCHAR(50) NOT NULL,
    floor INTEGER NOT NULL,
    base_cost DECIMAL(10,2) NOT NULL,
    description TEXT,
    owner_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--changeset taras:3-create-bookings-table
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT REFERENCES units(id),
    user_id BIGINT REFERENCES users(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

--changeset taras:4-create-payments-table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT REFERENCES bookings(id),
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--changeset taras:5-create-events-table
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    event_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--changeset taras:6-create-indexes
CREATE INDEX idx_bookings_unit_dates ON bookings(unit_id, start_date, end_date);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_expires_at ON bookings(expires_at);
CREATE INDEX idx_units_owner ON units(owner_id);
