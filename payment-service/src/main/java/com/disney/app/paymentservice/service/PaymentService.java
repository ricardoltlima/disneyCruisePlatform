package com.disney.app.paymentservice.service;

import com.disney.app.paymentservice.dto.PaymentRequest;
import com.disney.app.paymentservice.dto.PaymentResponse;
import com.disney.app.paymentservice.entity.PaymentEntity;
import com.disney.app.paymentservice.entity.PaymentStatus;
import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import com.disney.app.paymentservice.error.PaymentNotFoundException;
import com.disney.app.paymentservice.error.PaymentPersistenceException;
import com.disney.app.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Mono<PaymentResponse> createPayment(PaymentRequest request) {
        log.info("payment_create_requested reservationId={} amount={}", request.reservationId(), request.amount());

        return Mono.just(request)
                .map(this::toPaymentEntity)
                .flatMap(paymentRepository::save)
                .map(this::toPaymentResponse)
                .doOnNext(response -> log.info("payment_created paymentId={} reservationId={} status={}",
                        response.id(),
                        response.reservationId(),
                        response.status()))
                .doOnError(error -> log.warn("payment_create_failed reservationId={} error={}",
                        request.reservationId(),
                        error.getClass().getSimpleName()))
                .onErrorMap(DataAccessException.class,
                        ex -> new PaymentPersistenceException("Unable to save payment"));
    }

    public Mono<PaymentResponse> createPaymentFromReservation(ReservationCreatedEvent event) {
        log.info("payment_create_from_reservation_event_requested eventId={} reservationId={} amount={}",
                event.eventId(),
                event.reservationId(),
                event.amount());

        return paymentRepository.findFirstByReservationId(event.reservationId())
                .map(this::toPaymentResponse)
                .switchIfEmpty(Mono.defer(() -> paymentRepository.save(toPaymentEntity(event))
                        .map(this::toPaymentResponse)))
                .doOnNext(response -> log.info("payment_create_from_reservation_event_completed eventId={} reservationId={} paymentId={} status={}",
                        event.eventId(),
                        response.reservationId(),
                        response.id(),
                        response.status()))
                .doOnError(error -> log.warn("payment_create_from_reservation_event_failed eventId={} reservationId={} error={}",
                        event.eventId(),
                        event.reservationId(),
                        error.getClass().getSimpleName()))
                .onErrorMap(DataAccessException.class,
                        ex -> new PaymentPersistenceException("Unable to save payment from reservation event"));
    }

    public Mono<PaymentResponse> getPayment(String id) {
        log.info("payment_lookup_requested paymentId={}", id);

        return paymentRepository.findById(id)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException(id)))
                .map(this::toPaymentResponse)
                .doOnNext(response -> log.info("payment_lookup_succeeded paymentId={} status={}",
                        response.id(),
                        response.status()))
                .doOnError(error -> log.warn("payment_lookup_failed paymentId={} error={}",
                        id,
                        error.getClass().getSimpleName()))
                .onErrorMap(DataAccessException.class,
                        ex -> new PaymentPersistenceException("Unable to find payment"));
    }

    private PaymentEntity toPaymentEntity(PaymentRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PaymentStatus status = simulatePaymentStatus(request.paymentMethodToken());

        return new PaymentEntity(
                null,
                request.reservationId(),
                request.amount(),
                status,
                UUID.randomUUID().toString(),
                now,
                now
        );
    }

    private PaymentEntity toPaymentEntity(ReservationCreatedEvent event) {
        LocalDateTime now = LocalDateTime.now();

        return new PaymentEntity(
                null,
                event.reservationId(),
                event.amount(),
                PaymentStatus.COMPLETED,
                UUID.randomUUID().toString(),
                now,
                now
        );
    }

    private PaymentStatus simulatePaymentStatus(String paymentMethodToken) {
        return "fail".equalsIgnoreCase(paymentMethodToken)
                ? PaymentStatus.FAILED
                : PaymentStatus.COMPLETED;
    }

    private PaymentResponse toPaymentResponse(PaymentEntity entity) {
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
