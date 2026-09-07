package com.disney.app.paymentservice.repository;

import com.disney.app.paymentservice.entity.PaymentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentRepository extends ReactiveCrudRepository<PaymentEntity, String> {

    Mono<PaymentEntity> findFirstByReservationId(String reservationId);
}
