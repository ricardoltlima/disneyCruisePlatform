package com.disney.app.reservationservice.messaging;

import com.disney.app.reservationservice.config.AppKafkaProperties;
import com.disney.app.reservationservice.event.ReservationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.util.Map;

@Slf4j
@Component
public class ReservationCreatedEventPublisher {

    private final KafkaSender<String, byte[]> kafkaSender;
    private final ReservationCreatedEventAvroSerializer avroSerializer;
    private final String topic;

    public ReservationCreatedEventPublisher(
            AppKafkaProperties properties,
            ReservationCreatedEventAvroSerializer avroSerializer
    ) {
        this.avroSerializer = avroSerializer;
        this.topic = properties.getTopics().getReservationCreated();
        this.kafkaSender = KafkaSender.create(SenderOptions.create(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class,
                ProducerConfig.ACKS_CONFIG, properties.getProducer().getAcks(),
                ProducerConfig.CLIENT_ID_CONFIG, properties.getProducer().getClientId()
        )));
    }

    public Mono<Void> publish(ReservationCreatedEvent event) {
        return Mono.fromCallable(() -> avroSerializer.serialize(event))
                .flatMap(payload -> kafkaSender.send(Mono.just(SenderRecord.create(
                                topic,
                                null,
                                null,
                                event.reservationId(),
                                payload,
                                event.eventId()
                        )))
                        .single())
                .doOnNext(result -> log.info("Reservation created event {} for reservation {} was published to topic {} at partition {} offset {}",
                        event.eventId(),
                        event.reservationId(),
                        topic,
                        result.recordMetadata().partition(),
                        result.recordMetadata().offset()))
                .then();
    }
}
