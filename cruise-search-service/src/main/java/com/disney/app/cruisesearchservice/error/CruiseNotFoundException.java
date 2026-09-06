package com.disney.app.cruisesearchservice.error;

public class CruiseNotFoundException extends RuntimeException {

    public CruiseNotFoundException(String id) {
        super("Cruise not found with id: " + id);
    }
}
