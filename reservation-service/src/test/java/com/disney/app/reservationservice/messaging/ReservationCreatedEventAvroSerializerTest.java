package com.disney.app.reservationservice.messaging;

import com.disney.app.reservationservice.config.AppKafkaProperties;
import com.disney.app.reservationservice.event.ReservationCreatedEvent;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.DatumReader;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationCreatedEventAvroSerializerTest {

    @Test
    void serializeCreatesAvroPayloadForReservationCreatedEvent() throws Exception {
        AppKafkaProperties properties = new AppKafkaProperties();
        properties.getSchemas().setReservationCreated("avro/reservation-created-event.avsc");
        ReservationCreatedEventAvroSerializer serializer = new ReservationCreatedEventAvroSerializer(properties);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 12, 10, 30);
        ReservationCreatedEvent event = new ReservationCreatedEvent(
                "event-1",
                "reservation-1",
                "cruise-1",
                "guest-1",
                new BigDecimal("2401.00"),
                occurredAt,
                1
        );

        byte[] payload = serializer.serialize(event);

        DatumReader<GenericRecord> reader = new GenericDatumReader<>(serializer.schema());
        GenericRecord record = reader.read(
                null,
                DecoderFactory.get().binaryDecoder(payload, null)
        );
        assertThat(record.get("eventId").toString()).isEqualTo("event-1");
        assertThat(record.get("reservationId").toString()).isEqualTo("reservation-1");
        assertThat(record.get("cruiseId").toString()).isEqualTo("cruise-1");
        assertThat(record.get("guestId").toString()).isEqualTo("guest-1");
        assertThat(record.get("amount").toString()).isEqualTo("2401.00");
        assertThat(record.get("occurredAt").toString()).isEqualTo("2026-09-12T10:30");
        assertThat(record.get("version")).isEqualTo(1);
    }
}
