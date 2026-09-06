package com.disney.app.cruisesearchservice.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
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

    @ExceptionHandler(CruisePersistenceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePersistenceException(CruisePersistenceException ex) {
        ErrorResponse error = new ErrorResponse(
                "DATABASE_ERROR",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.internalServerError().body(error));
    }

    @ExceptionHandler(CruiseAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCruiseAlreadyExists(CruiseAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse(
                "CRUISE_ALREADY_EXISTS",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIdempotencyKeyConflict(IdempotencyKeyConflictException ex) {
        ErrorResponse error = new ErrorResponse(
                "IDEMPOTENCY_KEY_CONFLICT",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException ex) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_IDEMPOTENCY_KEY",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.badRequest().body(error));
    }

    @ExceptionHandler(CruiseNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCruiseNotFound(CruiseNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                "CRUISE_NOT_FOUND",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }
}
