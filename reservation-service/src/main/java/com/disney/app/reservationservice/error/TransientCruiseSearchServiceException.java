package com.disney.app.reservationservice.error;

public class TransientCruiseSearchServiceException extends CruiseSearchServiceException {

    public TransientCruiseSearchServiceException(String message) {
        super(message);
    }
}
