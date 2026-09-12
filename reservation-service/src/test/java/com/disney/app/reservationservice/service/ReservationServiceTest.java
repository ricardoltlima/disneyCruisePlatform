package com.disney.app.reservationservice.service;

import com.disney.app.reservationservice.client.CruiseSearchClient;
import com.disney.app.reservationservice.client.CruiseSearchResponse;
import com.disney.app.reservationservice.dto.ReservationRequest;
import com.disney.app.reservationservice.dto.ReservationResponse;
import com.disney.app.reservationservice.entity.ReservationEntity;
import com.disney.app.reservationservice.entity.ReservationStatus;
import com.disney.app.reservationservice.error.CruiseUnavailableException;
import com.disney.app.reservationservice.error.ReservationNotFoundException;
import com.disney.app.reservationservice.event.ReservationCreatedEvent;
import com.disney.app.reservationservice.mapper.ReservationMapper;
import com.disney.app.reservationservice.messaging.ReservationCreatedEventPublisher;
import com.disney.app.reservationservice.repository.ReservationRepository;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final LocalDateTime DEPARTURE_DATE = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime RETURN_DATE = LocalDateTime.of(2026, 10, 8, 9, 0);

    @Mock
    private ReservationRepository repository;

    @Mock
    private CruiseSearchClient cruiseSearchClient;

    @Mock
    private ReservationCreatedEventPublisher eventPublisher;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        ReservationMapper mapper = Mappers.getMapper(ReservationMapper.class);

        service = new ReservationService(
                repository,
                mapper,
                cruiseSearchClient,
                eventPublisher,
                Retry.ofDefaults("testMongoRetry")
        );
    }

    @Test
    void createReservationSavesPendingPaymentReservationAndPublishesEvent() {
        ReservationRequest request = new ReservationRequest("cruise-1", "guest-1", "Mickey Mouse", 2);
        CruiseSearchResponse cruise = availableCruise("cruise-1", 5, "1200.50");

        when(cruiseSearchClient.getCruise("cruise-1")).thenReturn(Mono.just(cruise));
        when(repository.save(any(ReservationEntity.class))).thenAnswer(invocation -> {
            ReservationEntity entity = invocation.getArgument(0);
            return Mono.just(new ReservationEntity(
                    "reservation-1",
                    entity.cruiseId(),
                    entity.guestId(),
                    entity.guestName(),
                    entity.numberOfGuests(),
                    entity.totalPrice(),
                    entity.status(),
                    entity.createdAt(),
                    entity.updatedAt()
            ));
        });
        when(eventPublisher.publish(any(ReservationCreatedEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.createReservation(request))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo("reservation-1");
                    assertThat(response.cruiseId()).isEqualTo("cruise-1");
                    assertThat(response.guestId()).isEqualTo("guest-1");
                    assertThat(response.guestName()).isEqualTo("Mickey Mouse");
                    assertThat(response.numberOfGuests()).isEqualTo(2);
                    assertThat(response.totalPrice()).isEqualByComparingTo("2401.00");
                    assertThat(response.status()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
                    assertThat(response.createdAt()).isNotNull();
                    assertThat(response.updatedAt()).isNotNull();
                })
                .verifyComplete();

        ArgumentCaptor<ReservationEntity> reservationCaptor = ArgumentCaptor.forClass(ReservationEntity.class);
        verify(repository).save(reservationCaptor.capture());
        assertThat(reservationCaptor.getValue().id()).isNull();
        assertThat(reservationCaptor.getValue().status()).isEqualTo(ReservationStatus.PENDING_PAYMENT);

        ArgumentCaptor<ReservationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ReservationCreatedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().reservationId()).isEqualTo("reservation-1");
        assertThat(eventCaptor.getValue().amount()).isEqualByComparingTo("2401.00");
        assertThat(eventCaptor.getValue().version()).isEqualTo(1);
    }

    @Test
    void createReservationRejectsUnavailableCruiseWithoutSaving() {
        ReservationRequest request = new ReservationRequest("cruise-1", "guest-1", "Mickey Mouse", 2);

        when(cruiseSearchClient.getCruise("cruise-1"))
                .thenReturn(Mono.just(availableCruise("cruise-1", 1, "1200.50")));

        StepVerifier.create(service.createReservation(request))
                .expectError(CruiseUnavailableException.class)
                .verify();

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void getReservationReturnsReservationWhenFound() {
        LocalDateTime now = LocalDateTime.now();
        ReservationEntity entity = new ReservationEntity(
                "reservation-1",
                "cruise-1",
                "guest-1",
                "Mickey Mouse",
                2,
                new BigDecimal("2401.00"),
                ReservationStatus.PENDING_PAYMENT,
                now,
                now
        );

        when(repository.findById("reservation-1")).thenReturn(Mono.just(entity));

        StepVerifier.create(service.getReservation("reservation-1"))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo("reservation-1");
                    assertThat(response.status()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
                })
                .verifyComplete();
    }

    @Test
    void getReservationFailsWhenMissing() {
        when(repository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(service.getReservation("missing"))
                .expectError(ReservationNotFoundException.class)
                .verify();
    }

    private CruiseSearchResponse availableCruise(String id, Integer availableCabins, String basePrice) {
        return new CruiseSearchResponse(
                id,
                "Disney Wish",
                "Port Canaveral",
                "Bahamas",
                DEPARTURE_DATE,
                RETURN_DATE,
                availableCabins,
                new BigDecimal(basePrice),
                "AVAILABLE"
        );
    }
}
