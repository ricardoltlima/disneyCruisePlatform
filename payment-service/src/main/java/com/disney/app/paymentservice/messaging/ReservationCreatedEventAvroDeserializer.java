package com.disney.app.paymentservice.messaging;

import com.disney.app.paymentservice.config.AppKafkaProperties;
import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import com.disney.app.paymentservice.mapper.ReservationCreatedEventMapper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ReservationCreatedEventAvroDeserializer {

    private final Schema schema;
    private final ReservationCreatedEventMapper mapper;

    public ReservationCreatedEventAvroDeserializer(AppKafkaProperties properties, ReservationCreatedEventMapper mapper) throws IOException {
        this.schema = new Schema.Parser().parse(new ClassPathResource(properties.reservationCreatedSchema()).getInputStream());
        this.mapper = mapper;
    }

    public ReservationCreatedEvent deserialize(byte[] payload) throws IOException {
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        GenericRecord record = reader.read(null, DecoderFactory.get().binaryDecoder(payload, null)
        );

        return mapper.toReservationCreatedEvent(record);
    }

    Schema schema() {
        return schema;
    }
}
