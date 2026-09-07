package com.disney.app.reservationservice.error;

public class ReservationPersistenceException extends RuntimeException {

    public ReservationPersistenceException(String message) {
        super(message);
    }
}
