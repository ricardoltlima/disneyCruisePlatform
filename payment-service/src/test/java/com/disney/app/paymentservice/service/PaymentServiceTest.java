package com.disney.app.paymentservice.service;

import com.disney.app.paymentservice.entity.PaymentEntity;
import com.disney.app.paymentservice.entity.PaymentStatus;
import com.disney.app.paymentservice.error.PaymentPersistenceException;
import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import com.disney.app.paymentservice.mapper.PaymentMapper;
import com.disney.app.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.DataAccessResourceFailureException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentService = new PaymentService(
                paymentRepository,
                Mappers.getMapper(PaymentMapper.class)
        );
    }

    @Test
    void createPaymentFromReservationCreatesCompletedPayment() {
        ReservationCreatedEvent event = reservationCreatedEvent();
        PaymentEntity savedEntity = paymentEntity("payment-1", event.reservationId(), event.amount(), PaymentStatus.COMPLETED);

        when(paymentRepository.findFirstByReservationId(event.reservationId())).thenReturn(Mono.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(paymentService.createPaymentFromReservation(event))
                .expectNext(savedEntity)
                .verifyComplete();

        verify(paymentRepository).save(any(PaymentEntity.class));
    }

    @Test
    void createPaymentFromReservationReturnsExistingPaymentForDuplicateReservation() {
        ReservationCreatedEvent event = reservationCreatedEvent();
        PaymentEntity existingEntity = paymentEntity("payment-1", event.reservationId(), event.amount(), PaymentStatus.COMPLETED);

        when(paymentRepository.findFirstByReservationId(event.reservationId())).thenReturn(Mono.just(existingEntity));

        StepVerifier.create(paymentService.createPaymentFromReservation(event))
                .expectNext(existingEntity)
                .verifyComplete();

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPaymentFromReservationMapsPersistenceFailure() {
        ReservationCreatedEvent event = reservationCreatedEvent();

        when(paymentRepository.findFirstByReservationId(event.reservationId())).thenReturn(Mono.empty());
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenReturn(Mono.error(new DataAccessResourceFailureException("Mongo unavailable")));

        StepVerifier.create(paymentService.createPaymentFromReservation(event))
                .expectError(PaymentPersistenceException.class)
                .verify();
    }

    @Test
    void createPaymentFromReservationBuildsEntityFromReservationEvent() {
        ReservationCreatedEvent event = reservationCreatedEvent();

        when(paymentRepository.findFirstByReservationId(event.reservationId())).thenReturn(Mono.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(paymentService.createPaymentFromReservation(event))
                .assertNext(payment -> {
                    assertThat(payment.id()).isNull();
                    assertThat(payment.reservationId()).isEqualTo(event.reservationId());
                    assertThat(payment.amount()).isEqualByComparingTo(event.amount());
                    assertThat(payment.status()).isEqualTo(PaymentStatus.COMPLETED);
                    assertThat(payment.providerReference()).isNotBlank();
                    assertThat(payment.createdAt()).isNotNull();
                    assertThat(payment.updatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    private static ReservationCreatedEvent reservationCreatedEvent() {
        return new ReservationCreatedEvent(
                "event-1",
                "reservation-1",
                "cruise-1",
                "guest-1",
                new BigDecimal("2999.98"),
                LocalDateTime.of(2026, 9, 12, 10, 30),
                1
        );
    }

    private static PaymentEntity paymentEntity(String id, String reservationId, BigDecimal amount, PaymentStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 12, 10, 35);
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
}
