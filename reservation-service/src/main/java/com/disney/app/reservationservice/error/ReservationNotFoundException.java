package com.disney.app.reservationservice.error;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(String id) {
        super("Reservation not found with id: " + id);
    }
}
