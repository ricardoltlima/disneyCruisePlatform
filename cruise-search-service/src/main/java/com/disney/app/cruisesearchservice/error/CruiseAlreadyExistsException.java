package com.disney.app.cruisesearchservice.error;

public class CruiseAlreadyExistsException extends RuntimeException {
    public CruiseAlreadyExistsException(String message) {
        super(message);
    }
}
