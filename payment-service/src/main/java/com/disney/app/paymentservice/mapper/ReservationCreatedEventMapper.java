package com.disney.app.paymentservice.mapper;

import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import org.apache.avro.generic.GenericRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationCreatedEventMapper {

    @Mapping(target = "eventId", expression = "java(record.get(\"eventId\").toString())")
    @Mapping(target = "reservationId", expression = "java(record.get(\"reservationId\").toString())")
    @Mapping(target = "cruiseId", expression = "java(record.get(\"cruiseId\").toString())")
    @Mapping(target = "guestId", expression = "java(record.get(\"guestId\").toString())")
    @Mapping(target = "amount", expression = "java(new java.math.BigDecimal(record.get(\"amount\").toString()))")
    @Mapping(target = "occurredAt", expression = "java(java.time.LocalDateTime.parse(record.get(\"occurredAt\").toString()))")
    @Mapping(target = "version", expression = "java((Integer) record.get(\"version\"))")
    ReservationCreatedEvent toReservationCreatedEvent(GenericRecord record);
}
