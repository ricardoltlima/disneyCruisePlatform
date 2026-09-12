package com.disney.app.paymentservice.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentPersistenceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePaymentPersistenceException(PaymentPersistenceException ex, ServerWebExchange exchange) {
        logRequestError(exchange, ex, HttpStatus.INTERNAL_SERVER_ERROR);

        ErrorResponse error = new ErrorResponse(
                "PAYMENT_PERSISTENCE_ERROR",
                "We could not save the payment. Please try again later.",
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.internalServerError().body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnexpectedException(Exception ex, ServerWebExchange exchange) {
        logRequestError(exchange, ex, HttpStatus.INTERNAL_SERVER_ERROR);

        ErrorResponse error = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Something went wrong while processing the payment request. Please try again later.",
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.internalServerError().body(error));
    }

    private void logRequestError(ServerWebExchange exchange, Throwable ex, HttpStatus status) {
        log.error("Payment service request failed. method={} path={} query={} status={} error={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getURI().getQuery(),
                status.value(),
                ex.getClass().getSimpleName(),
                ex);
    }
}
