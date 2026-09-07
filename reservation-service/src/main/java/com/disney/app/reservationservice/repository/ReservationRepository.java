package com.disney.app.reservationservice.repository;

import com.disney.app.reservationservice.entity.ReservationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ReservationRepository extends ReactiveCrudRepository<ReservationEntity, String> {
}
