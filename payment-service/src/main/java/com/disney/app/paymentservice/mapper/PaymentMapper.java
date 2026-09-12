package com.disney.app.paymentservice.mapper;

import com.disney.app.paymentservice.entity.PaymentEntity;
import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "COMPLETED")
    @Mapping(target = "providerReference", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    PaymentEntity toPaymentEntity(ReservationCreatedEvent event);
}
