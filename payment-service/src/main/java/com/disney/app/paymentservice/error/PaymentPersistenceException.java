package com.disney.app.paymentservice.error;

public class PaymentPersistenceException extends RuntimeException {

    public PaymentPersistenceException(String message) {
        super(message);
    }
}
