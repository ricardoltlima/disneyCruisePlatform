package com.disney.app.reservationservice.messaging;

import com.disney.app.reservationservice.event.ReservationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
@Component
public class ReservationCreatedEventPublisher {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;
    private final String topic;

    public ReservationCreatedEventPublisher(
            ObjectMapper objectMapper,
            @Value("${app.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${app.kafka.topics.reservation-created}") String topic
    ) {
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.kafkaSender = KafkaSender.create(SenderOptions.create(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"
        )));
    }

    public Mono<Void> publish(ReservationCreatedEvent event) {
        return Mono.fromCallable(() -> toJson(event))
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

    private String toJson(ReservationCreatedEvent event) throws Exception {
        return objectMapper.writeValueAsString(event);
    }
}
