package com.disney.app.reservationservice.error;

public class CruiseUnavailableException extends RuntimeException {

    public CruiseUnavailableException(String cruiseId) {
        super("Cruise is unavailable for reservation: " + cruiseId);
    }
}
