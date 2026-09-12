package com.disney.app.reservationservice.messaging;

import com.disney.app.reservationservice.config.AppKafkaProperties;
import com.disney.app.reservationservice.event.ReservationCreatedEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ReservationCreatedEventAvroSerializer {

    private final Schema schema;

    public ReservationCreatedEventAvroSerializer(AppKafkaProperties properties) throws IOException {
        this.schema = new Schema.Parser()
                .parse(new ClassPathResource(properties.getSchemas().getReservationCreated()).getInputStream());
    }

    public byte[] serialize(ReservationCreatedEvent event) throws IOException {
        GenericRecord record = new GenericData.Record(schema);
        record.put("eventId", event.eventId());
        record.put("reservationId", event.reservationId());
        record.put("cruiseId", event.cruiseId());
        record.put("guestId", event.guestId());
        record.put("amount", event.amount().toPlainString());
        record.put("occurredAt", event.occurredAt().toString());
        record.put("version", event.version());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        writer.write(record, encoder);
        encoder.flush();
        return outputStream.toByteArray();
    }

    Schema schema() {
        return schema;
    }
}
