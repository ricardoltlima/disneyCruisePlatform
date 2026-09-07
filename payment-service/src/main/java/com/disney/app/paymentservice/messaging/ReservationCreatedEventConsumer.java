package com.disney.app.paymentservice.messaging;

import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import com.disney.app.paymentservice.service.PaymentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

@Component
public class ReservationCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservationCreatedEventConsumer.class);

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private Disposable subscription;

    public ReservationCreatedEventConsumer(
            ObjectMapper objectMapper,
            PaymentService paymentService,
            @Value("${app.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${app.kafka.consumer.group-id}") String groupId,
            @Value("${app.kafka.topics.reservation-created}") String reservationCreatedTopic
    ) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;

        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, groupId,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false
                ))
                .subscription(Collections.singleton(reservationCreatedTopic));

        this.kafkaReceiver = KafkaReceiver.create(receiverOptions);
    }

    @PostConstruct
    public void start() {
        subscription = kafkaReceiver.receive()
                .concatMap(record -> toEvent(record.value())
                        .flatMap(paymentService::createPaymentFromReservation)
                        .doOnNext(payment -> log.info("reservation_created_event_processed eventId={} reservationId={} paymentId={} status={}",
                                record.receiverOffset().topicPartition(),
                                payment.reservationId(),
                                payment.id(),
                                payment.status()))
                        .then(Mono.fromRunnable(record.receiverOffset()::acknowledge)))
                .doOnError(error -> log.error("reservation_created_consumer_failed error={}", error.getClass().getSimpleName(), error))
                .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    private Mono<ReservationCreatedEvent> toEvent(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, ReservationCreatedEvent.class));
    }
}
