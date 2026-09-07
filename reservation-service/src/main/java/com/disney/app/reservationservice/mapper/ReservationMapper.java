package com.disney.app.reservationservice.mapper;

import com.disney.app.reservationservice.dto.ReservationResponse;
import com.disney.app.reservationservice.entity.ReservationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    ReservationResponse toReservationResponse(ReservationEntity cruiseEntity);
}
