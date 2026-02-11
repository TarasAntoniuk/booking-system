--liquibase formatted sql

--changeset taras:7-update-sequence-allocation-size
--comment: Increase sequence INCREMENT to 50 to match JPA allocationSize=50, reducing DB round-trips per INSERT
ALTER SEQUENCE users_id_seq INCREMENT BY 50;
ALTER SEQUENCE units_id_seq INCREMENT BY 50;
ALTER SEQUENCE bookings_id_seq INCREMENT BY 50;
ALTER SEQUENCE payments_id_seq INCREMENT BY 50;
ALTER SEQUENCE events_id_seq INCREMENT BY 50;
