package com.disney.app.reservationservice.mapper;

import com.disney.app.reservationservice.client.CruiseSearchResponse;
import com.disney.app.reservationservice.dto.ReservationRequest;
import com.disney.app.reservationservice.dto.ReservationResponse;
import com.disney.app.reservationservice.entity.ReservationEntity;
import com.disney.app.reservationservice.event.ReservationCreatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    ReservationResponse toReservationResponse(ReservationEntity cruiseEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPrice", expression = "java(cruise.basePrice().multiply(java.math.BigDecimal.valueOf(request.numberOfGuests())))")
    @Mapping(target = "status", constant = "PENDING_PAYMENT")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    ReservationEntity toReservationEntity(ReservationRequest request, CruiseSearchResponse cruise);

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "reservationId", source = "id")
    @Mapping(target = "amount", source = "totalPrice")
    @Mapping(target = "occurredAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "version", constant = "1")
    ReservationCreatedEvent toReservationCreatedEvent(ReservationResponse response);
}
