package com.disney.app.paymentservice.service;

import com.disney.app.paymentservice.dto.PaymentRequest;
import com.disney.app.paymentservice.dto.PaymentResponse;
import com.disney.app.paymentservice.entity.PaymentEntity;
import com.disney.app.paymentservice.entity.PaymentStatus;
import com.disney.app.paymentservice.error.PaymentNotFoundException;
import com.disney.app.paymentservice.error.PaymentPersistenceException;
import com.disney.app.paymentservice.repository.PaymentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentService = new PaymentService(paymentRepository, new SimpleMeterRegistry());
    }

    @Test
    void createPaymentCompletesPaymentForValidPaymentToken() {
        PaymentRequest request = new PaymentRequest("reservation-1", new BigDecimal("2999.98"), "valid-token");
        PaymentEntity savedEntity = paymentEntity("payment-1", request.reservationId(), request.amount(), PaymentStatus.COMPLETED);

        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(paymentService.createPayment(request))
                .expectNext(paymentResponse(savedEntity))
                .verifyComplete();
    }

    @Test
    void createPaymentFailsPaymentWhenTokenIsFail() {
        PaymentRequest request = new PaymentRequest("reservation-1", new BigDecimal("2999.98"), "fail");
        PaymentEntity savedEntity = paymentEntity("payment-1", request.reservationId(), request.amount(), PaymentStatus.FAILED);

        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(paymentService.createPayment(request))
                .expectNext(paymentResponse(savedEntity))
                .verifyComplete();
    }

    @Test
    void createPaymentMapsPersistenceFailure() {
        PaymentRequest request = new PaymentRequest("reservation-1", new BigDecimal("2999.98"), "valid-token");

        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenReturn(Mono.error(new DataAccessResourceFailureException("Mongo unavailable")));

        StepVerifier.create(paymentService.createPayment(request))
                .expectError(PaymentPersistenceException.class)
                .verify();
    }

    @Test
    void getPaymentReturnsPaymentWhenFound() {
        PaymentEntity entity = paymentEntity("payment-1", "reservation-1", new BigDecimal("2999.98"), PaymentStatus.COMPLETED);

        when(paymentRepository.findById("payment-1")).thenReturn(Mono.just(entity));

        StepVerifier.create(paymentService.getPayment("payment-1"))
                .expectNext(paymentResponse(entity))
                .verifyComplete();
    }

    @Test
    void getPaymentReturnsNotFoundWhenMissing() {
        when(paymentRepository.findById("missing-payment")).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.getPayment("missing-payment"))
                .expectError(PaymentNotFoundException.class)
                .verify();
    }

    private static PaymentEntity paymentEntity(String id, String reservationId, BigDecimal amount, PaymentStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 7, 8, 0);
        return new PaymentEntity(
                id,
                reservationId,
                amount,
                status,
                "provider-reference-1",
                now,
                now
        );
    }

    private static PaymentResponse paymentResponse(PaymentEntity entity) {
        return new PaymentResponse(
                entity.id(),
                entity.reservationId(),
                entity.amount(),
                entity.status(),
                entity.providerReference(),
                entity.createdAt(),
                entity.updatedAt()
        );
    }
}
