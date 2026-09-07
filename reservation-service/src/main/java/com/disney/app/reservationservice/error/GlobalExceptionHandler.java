package com.disney.app.reservationservice.error;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(ReservationPersistenceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleReservationPersistenceException(ReservationPersistenceException ex) {
        ErrorResponse error = new ErrorResponse(
                "RESERVATION_PERSISTENCE_ERROR",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.internalServerError().body(error));
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleReservationNotFoundException(ReservationNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                "RESERVATION_NOT_FOUND",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(CruiseUnavailableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCruiseUnavailableException(CruiseUnavailableException ex) {
        ErrorResponse error = new ErrorResponse(
                "CRUISE_UNAVAILABLE",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    @ExceptionHandler(CruiseSearchServiceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCruiseSearchServiceException(CruiseSearchServiceException ex) {
        ErrorResponse error = new ErrorResponse(
                "CRUISE_SEARCH_SERVICE_ERROR",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error));
    }
}
