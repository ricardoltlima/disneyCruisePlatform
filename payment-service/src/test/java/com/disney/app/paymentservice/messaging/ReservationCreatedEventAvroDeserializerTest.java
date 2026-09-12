package com.disney.app.paymentservice.messaging;

import com.disney.app.paymentservice.config.AppKafkaProperties;
import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationCreatedEventAvroDeserializerTest {

    @Test
    void deserializeReadsReservationCreatedEventPayload() throws Exception {
        AppKafkaProperties properties = new AppKafkaProperties();
        properties.getSchemas().setReservationCreated("avro/reservation-created-event.avsc");
        ReservationCreatedEventAvroDeserializer deserializer = new ReservationCreatedEventAvroDeserializer(properties);

        ReservationCreatedEvent event = deserializer.deserialize(payload());

        assertThat(event.eventId()).isEqualTo("event-1");
        assertThat(event.reservationId()).isEqualTo("reservation-1");
        assertThat(event.cruiseId()).isEqualTo("cruise-1");
        assertThat(event.guestId()).isEqualTo("guest-1");
        assertThat(event.amount()).isEqualByComparingTo("2999.98");
        assertThat(event.occurredAt().toString()).isEqualTo("2026-09-12T10:30");
        assertThat(event.version()).isEqualTo(1);
    }

    private byte[] payload() throws Exception {
        Schema schema = new Schema.Parser()
                .parse(new ClassPathResource("avro/reservation-created-event.avsc").getInputStream());
        GenericRecord record = new GenericData.Record(schema);
        record.put("eventId", "event-1");
        record.put("reservationId", "reservation-1");
        record.put("cruiseId", "cruise-1");
        record.put("guestId", "guest-1");
        record.put("amount", "2999.98");
        record.put("occurredAt", "2026-09-12T10:30");
        record.put("version", 1);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        writer.write(record, encoder);
        encoder.flush();
        return outputStream.toByteArray();
    }
}
