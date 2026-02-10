--liquibase formatted sql

--changeset taras:add-entity-type-to-events
ALTER TABLE events ADD COLUMN entity_type VARCHAR(50);

-- Backfill existing data based on event_type prefix
UPDATE events SET entity_type = 'BOOKING' WHERE event_type LIKE 'BOOKING_%';
UPDATE events SET entity_type = 'UNIT' WHERE event_type LIKE 'UNIT_%';
UPDATE events SET entity_type = 'PAYMENT' WHERE event_type LIKE 'PAYMENT_%';

-- Now make it NOT NULL
ALTER TABLE events ALTER COLUMN entity_type SET NOT NULL;
