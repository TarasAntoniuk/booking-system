package com.tarasantoniuk.booking.exception;

public class UnitNotAvailableException extends RuntimeException {

    public UnitNotAvailableException(String message) {
        super(message);
    }

    public UnitNotAvailableException() {
        super("Unit is not available for the selected dates");
    }
}