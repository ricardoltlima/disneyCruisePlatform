package com.disney.app.reservationservice.service;

import com.disney.app.cruisesearchservice.model.CruiseStatus;
import com.disney.app.reservationservice.client.CruiseSearchClient;
import com.disney.app.reservationservice.client.CruiseSearchResponse;
import com.disney.app.reservationservice.dto.ReservationRequest;
import com.disney.app.reservationservice.dto.ReservationResponse;
import com.disney.app.reservationservice.entity.ReservationEntity;
import com.disney.app.reservationservice.entity.ReservationStatus;
import com.disney.app.reservationservice.error.CruiseUnavailableException;
import com.disney.app.reservationservice.error.ReservationNotFoundException;
import com.disney.app.reservationservice.error.ReservationPersistenceException;
import com.disney.app.reservationservice.event.ReservationCreatedEvent;
import com.disney.app.reservationservice.mapper.ReservationMapper;
import com.disney.app.reservationservice.messaging.ReservationCreatedEventPublisher;
import com.disney.app.reservationservice.repository.ReservationRepository;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class ReservationService {

    private final ReservationRepository repository;
    private final ReservationMapper mapper;
    private final CruiseSearchClient cruiseSearchClient;
    private final ReservationCreatedEventPublisher eventPublisher;
    private final Retry mongoRetry;

    public ReservationService(
            ReservationRepository repository,
            ReservationMapper mapper,
            CruiseSearchClient cruiseSearchClient,
            ReservationCreatedEventPublisher eventPublisher,
            @Qualifier("mongoRetry") Retry mongoRetry
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.cruiseSearchClient = cruiseSearchClient;
        this.eventPublisher = eventPublisher;
        this.mongoRetry = mongoRetry;
    }

    public Mono<ReservationResponse> createReservation(ReservationRequest request) {
        log.info("Creating reservation for guest {} on cruise {} for {} guests",
                request.guestId(),
                request.cruiseId(),
                request.numberOfGuests());

        return cruiseSearchClient.getCruise(request.cruiseId())
                .filter(cruise -> isAvailable(cruise, request.numberOfGuests()))
                .switchIfEmpty(Mono.error(new CruiseUnavailableException(request.cruiseId())))
                .map(cruise -> toReservationEntity(request, cruise))
                .flatMap(entity -> repository.save(entity).transformDeferred(RetryOperator.of(mongoRetry)))
                .map(mapper::toReservationResponse)
                .flatMap(response -> eventPublisher.publish(toReservationCreatedEvent(response)).thenReturn(response))
                .doOnNext(response -> log.info("Reservation {} created for guest {} on cruise {} with status {} and total price {}",
                        response.id(),
                        response.guestId(),
                        response.cruiseId(),
                        response.status(),
                        response.totalPrice()))
                .doOnError(error -> log.warn("Could not create reservation for guest {} on cruise {}: {}",
                        request.guestId(),
                        request.cruiseId(),
                        error.getClass().getSimpleName()))
                .onErrorMap(DataAccessException.class, ex -> new ReservationPersistenceException("Unable to save reservation"));
    }

    public Mono<ReservationResponse> getReservation(String id) {
        log.info("Looking up reservation {}", id);
        return repository.findById(id)
                .transformDeferred(RetryOperator.of(mongoRetry))
                .switchIfEmpty(Mono.error(new ReservationNotFoundException(id)))
                .map(mapper::toReservationResponse)
                .doOnNext(response -> log.info("Reservation {} found with status {}", response.id(), response.status()))
                .doOnError(error -> log.warn("Could not find reservation {}: {}", id, error.getClass().getSimpleName()))
                .onErrorMap(DataAccessException.class, ex -> new ReservationPersistenceException("Unable to find reservation"));
    }

    private boolean isAvailable(CruiseSearchResponse cruise, Integer numberOfGuests) {
        return CruiseStatus.AVAILABLE.name().equalsIgnoreCase(cruise.status())
                && cruise.availableCabins() != null
                && cruise.availableCabins() >= numberOfGuests;
    }

    private ReservationEntity toReservationEntity(ReservationRequest request, CruiseSearchResponse cruise) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalPrice = cruise.basePrice().multiply(BigDecimal.valueOf(request.numberOfGuests()));

        return new ReservationEntity(
                null,
                request.cruiseId(),
                request.guestId(),
                request.guestName(),
                request.numberOfGuests(),
                totalPrice,
                ReservationStatus.PENDING_PAYMENT,
                now,
                now
        );
    }

    private ReservationCreatedEvent toReservationCreatedEvent(ReservationResponse response) {
        return new ReservationCreatedEvent(
                UUID.randomUUID().toString(),
                response.id(),
                response.cruiseId(),
                response.guestId(),
                response.totalPrice(),
                LocalDateTime.now(),
                1
        );
    }
}
