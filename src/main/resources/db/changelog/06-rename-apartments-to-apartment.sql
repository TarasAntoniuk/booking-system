--liquibase formatted sql

--changeset taras:10-rename-apartments-to-apartment
UPDATE units SET accommodation_type = 'APARTMENT' WHERE accommodation_type = 'APARTMENTS';
