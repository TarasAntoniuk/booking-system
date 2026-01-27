--liquibase formatted sql

--changeset taras:7-add-booking-overlap-constraint
-- Create a function to check for booking overlaps
-- This provides database-level protection against race conditions
CREATE OR REPLACE FUNCTION check_booking_overlap()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM bookings
        WHERE unit_id = NEW.unit_id
        AND id != COALESCE(NEW.id, -1)
        AND status IN ('PENDING', 'CONFIRMED')
        AND (start_date <= NEW.end_date AND end_date >= NEW.start_date)
    ) THEN
        RAISE EXCEPTION 'Booking overlaps with existing booking for this unit';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--changeset taras:8-create-booking-overlap-trigger
-- Create trigger that fires before insert or update on bookings
CREATE TRIGGER trg_check_booking_overlap
    BEFORE INSERT OR UPDATE ON bookings
    FOR EACH ROW
    WHEN (NEW.status IN ('PENDING', 'CONFIRMED'))
    EXECUTE FUNCTION check_booking_overlap();

--changeset taras:9-add-index-for-overlap-check
-- Add index to optimize the overlap check query
CREATE INDEX idx_bookings_unit_status_dates
ON bookings(unit_id, status, start_date, end_date)
WHERE status IN ('PENDING', 'CONFIRMED');
