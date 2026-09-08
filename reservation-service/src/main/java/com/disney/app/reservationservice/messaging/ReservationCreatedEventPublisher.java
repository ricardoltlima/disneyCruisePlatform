package com.disney.app.reservationservice.messaging;

import com.disney.app.reservationservice.event.ReservationCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class ReservationCreatedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationCreatedEventPublisher.class);

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final String topic;

    public ReservationCreatedEventPublisher(
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${app.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${app.kafka.topics.reservation-created}") String topic
    ) {
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.topic = topic;
        this.kafkaSender = KafkaSender.create(SenderOptions.create(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"
        )));
    }

    public Mono<Void> publish(ReservationCreatedEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);

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
                .doOnNext(result -> log.info("reservation_created_event_published eventId={} reservationId={} topic={} partition={} offset={}",
                        event.eventId(),
                        event.reservationId(),
                        topic,
                        result.recordMetadata().partition(),
                        result.recordMetadata().offset()))
                .doOnNext(result -> {
                    meterRegistry.counter("reservation.event.published", "topic", topic, "outcome", "success").increment();
                    sample.stop(meterRegistry.timer("reservation.event.publish.duration", "topic", topic, "outcome", "success"));
                })
                .doOnError(error -> {
                    meterRegistry.counter("reservation.event.published", "topic", topic, "outcome", error.getClass().getSimpleName()).increment();
                    sample.stop(meterRegistry.timer("reservation.event.publish.duration", "topic", topic, "outcome", "error"));
                })
                .then();
    }

    private String toJson(ReservationCreatedEvent event) throws Exception {
        return objectMapper.writeValueAsString(event);
    }
}
