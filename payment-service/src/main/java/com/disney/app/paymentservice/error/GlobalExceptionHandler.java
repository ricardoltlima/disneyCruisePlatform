package com.disney.app.paymentservice.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<List<ErrorResponse>>> handleValidationException(WebExchangeBindException ex) {
        List<ErrorResponse> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ErrorResponse(
                        "BAD_REQUEST",
                        fieldError.getField() + ": " + fieldError.getDefaultMessage(),
                        LocalDateTime.now()
                ))
                .toList();

        return Mono.just(ResponseEntity.badRequest().body(errors));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                "PAYMENT_NOT_FOUND",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(PaymentPersistenceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePaymentPersistenceException(PaymentPersistenceException ex) {
        ErrorResponse error = new ErrorResponse(
                "PAYMENT_PERSISTENCE_ERROR",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.internalServerError().body(error));
    }
}
