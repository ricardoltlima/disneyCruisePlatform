package com.disney.app.reservationservice.controller;

import com.disney.app.reservationservice.dto.ReservationRequest;
import com.disney.app.reservationservice.dto.ReservationResponse;
import com.disney.app.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<ReservationResponse>> createReservation(@Valid @RequestBody ReservationRequest request) {
        log.info("Received create reservation request for guest {} on cruise {}", request.guestId(), request.cruiseId());

        return service.createReservation(request)
                .map(reservationResponse -> ResponseEntity
                        .created(URI.create("/api/v1/reservations/" + reservationResponse.id()))
                        .body(reservationResponse))
                .doOnNext(response -> log.info("Create reservation request completed with status {}", response.getStatusCode()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ReservationResponse>> getReservation(@PathVariable String id) {
        log.info("Received get reservation request for reservation {}", id);

        return service.getReservation(id)
                .map(ResponseEntity::ok)
                .doOnNext(response -> log.info("Get reservation request completed for reservation {} with status {}", id, response.getStatusCode()));
    }
}
