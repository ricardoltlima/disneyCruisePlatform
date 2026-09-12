package com.disney.app.paymentservice.service;

import com.disney.app.paymentservice.entity.PaymentEntity;
import com.disney.app.paymentservice.error.PaymentPersistenceException;
import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import com.disney.app.paymentservice.mapper.PaymentMapper;
import com.disney.app.paymentservice.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    public PaymentService(PaymentRepository paymentRepository, PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    public Mono<PaymentEntity> createPaymentFromReservation(ReservationCreatedEvent event) {
        log.info("Creating payment for reservation. eventId={} reservationId={} amount={}", event.eventId(), event.reservationId(), event.amount());

        return paymentRepository.findFirstByReservationId(event.reservationId())
                .flatMap(existing -> {
                    log.info("Payment already exists for reservation. eventId={} reservationId={} paymentId={}", event.eventId(), event.reservationId(), existing.id());
                    return Mono.just(existing);
                })
                .switchIfEmpty(Mono.defer(() -> paymentRepository.save(paymentMapper.toPaymentEntity(event))))
                .doOnNext(response -> log.info("Payment processed for reservation. eventId={} reservationId={} paymentId={} status={}", event.eventId(), response.reservationId(), response.id(), response.status()))
                .doOnError(error -> log.warn("Could not process payment for reservation. eventId={} reservationId={} error={}", event.eventId(), event.reservationId(), error.getClass().getSimpleName()))
                .onErrorMap(DataAccessException.class, ex -> new PaymentPersistenceException("We could not save the payment for this reservation."));
    }
}
