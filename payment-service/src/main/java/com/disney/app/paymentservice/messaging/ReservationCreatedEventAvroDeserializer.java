package com.disney.app.paymentservice.messaging;

import com.disney.app.paymentservice.config.AppKafkaProperties;
import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class ReservationCreatedEventAvroDeserializer {

    private final Schema schema;

    public ReservationCreatedEventAvroDeserializer(AppKafkaProperties properties) throws IOException {
        this.schema = new Schema.Parser()
                .parse(new ClassPathResource(properties.getSchemas().getReservationCreated()).getInputStream());
    }

    public ReservationCreatedEvent deserialize(byte[] payload) throws IOException {
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        GenericRecord record = reader.read(
                null,
                DecoderFactory.get().binaryDecoder(payload, null)
        );

        return new ReservationCreatedEvent(
                record.get("eventId").toString(),
                record.get("reservationId").toString(),
                record.get("cruiseId").toString(),
                record.get("guestId").toString(),
                new BigDecimal(record.get("amount").toString()),
                LocalDateTime.parse(record.get("occurredAt").toString()),
                (Integer) record.get("version")
        );
    }

    Schema schema() {
        return schema;
    }
}
