package com.disney.app.reservationservice.service;

import com.disney.app.reservationservice.client.CruiseSearchClient;
import com.disney.app.reservationservice.client.CruiseSearchResponse;
import com.disney.app.reservationservice.dto.ReservationRequest;
import com.disney.app.reservationservice.dto.ReservationResponse;
import com.disney.app.reservationservice.error.CruiseUnavailableException;
import com.disney.app.reservationservice.error.ReservationNotFoundException;
import com.disney.app.reservationservice.error.ReservationPersistenceException;
import com.disney.app.reservationservice.mapper.ReservationMapper;
import com.disney.app.reservationservice.messaging.ReservationCreatedEventPublisher;
import com.disney.app.reservationservice.model.CruiseStatus;
import com.disney.app.reservationservice.repository.ReservationRepository;
import com.mongodb.MongoException;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
        log.info("Creating reservation for guest {} on cruise {} for {} guests", request.guestId(), request.cruiseId(), request.numberOfGuests());

        return cruiseSearchClient.getCruise(request.cruiseId())
                .filter(cruise -> isAvailable(cruise, request.numberOfGuests()))
                .switchIfEmpty(Mono.error(new CruiseUnavailableException(request.cruiseId())))
                .map(cruise -> mapper.toReservationEntity(request, cruise))
                .flatMap(entity -> repository.save(entity).transformDeferred(RetryOperator.of(mongoRetry)))
                .map(mapper::toReservationResponse)
                .flatMap(response -> eventPublisher.publish(mapper.toReservationCreatedEvent(response)).thenReturn(response))
                .doOnNext(response -> log.info("Reservation {} created for guest {} on cruise {} with status {} and total price {}",
                        response.id(), response.guestId(), response.cruiseId(), response.status(), response.totalPrice()))
                .doOnError(error -> log.warn("Could not create reservation for guest {} on cruise {}: {}",
                        request.guestId(), request.cruiseId(), error.getClass().getSimpleName()))
                .onErrorMap(this::isDatabaseException, ex -> new ReservationPersistenceException("Unable to save reservation"));
    }

    public Mono<ReservationResponse> getReservation(String id) {
        log.info("Looking up reservation {}", id);
        return repository.findById(id)
                .transformDeferred(RetryOperator.of(mongoRetry))
                .switchIfEmpty(Mono.error(new ReservationNotFoundException(id)))
                .map(mapper::toReservationResponse)
                .doOnNext(response -> log.info("Reservation {} found with status {}", response.id(), response.status()))
                .doOnError(error -> log.warn("Could not find reservation {}: {}", id, error.getClass().getSimpleName()))
                .onErrorMap(this::isDatabaseException, ex -> new ReservationPersistenceException("Unable to find reservation"));
    }

    private boolean isAvailable(CruiseSearchResponse cruise, Integer numberOfGuests) {
        return CruiseStatus.AVAILABLE.name().equalsIgnoreCase(cruise.status())
                && cruise.availableCabins() != null
                && cruise.availableCabins() >= numberOfGuests
                && cruise.basePrice() != null;
    }

    private boolean isDatabaseException(Throwable throwable) {
        return throwable instanceof DataAccessException || throwable instanceof MongoException;
    }
}
