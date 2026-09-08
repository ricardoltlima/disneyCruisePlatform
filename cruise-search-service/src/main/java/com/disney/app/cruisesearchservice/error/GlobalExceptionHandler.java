package com.disney.app.cruisesearchservice.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<List<ErrorResponse>>> handleValidationException(WebExchangeBindException ex, ServerWebExchange exchange) {
        logRequestFailure(exchange, ex, HttpStatus.BAD_REQUEST);

        List<ErrorResponse> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ErrorResponse("BAD_REQUEST", fieldError.getField() + ": " + fieldError.getDefaultMessage(), LocalDateTime.now()))
                .toList();
        return Mono.just(ResponseEntity.badRequest().body(errors));
    }

    @ExceptionHandler(CruisePersistenceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePersistenceException(CruisePersistenceException ex, ServerWebExchange exchange) {
        logRequestError(exchange, ex, HttpStatus.INTERNAL_SERVER_ERROR);

        ErrorResponse error = new ErrorResponse("DATABASE_ERROR", ex.getMessage(), LocalDateTime.now());
        return Mono.just(ResponseEntity.internalServerError().body(error));
    }

    @ExceptionHandler(CruiseAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCruiseAlreadyExists(CruiseAlreadyExistsException ex, ServerWebExchange exchange) {
        logRequestFailure(exchange, ex, HttpStatus.CONFLICT);

        ErrorResponse error = new ErrorResponse("CRUISE_ALREADY_EXISTS", ex.getMessage(), LocalDateTime.now());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    @ExceptionHandler(CruiseNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCruiseNotFound(CruiseNotFoundException ex, ServerWebExchange exchange) {
        logRequestFailure(exchange, ex, HttpStatus.NOT_FOUND);

        ErrorResponse error = new ErrorResponse("CRUISE_NOT_FOUND", ex.getMessage(), LocalDateTime.now());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    private void logRequestFailure(ServerWebExchange exchange, Throwable ex, HttpStatus status) {
        log.warn("Request failed. method={} path={} query={} status={} error={} message={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getURI().getQuery(),
                status.value(),
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }

    private void logRequestError(ServerWebExchange exchange, Throwable ex, HttpStatus status) {
        log.error("Request failed. method={} path={} query={} status={} error={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getURI().getQuery(),
                status.value(),
                ex.getClass().getSimpleName(),
                ex);
    }
}
