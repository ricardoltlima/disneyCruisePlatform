package com.disney.app.paymentservice.messaging;

import com.disney.app.paymentservice.config.AppKafkaProperties;
import com.disney.app.paymentservice.event.ReservationCreatedEvent;
import com.disney.app.paymentservice.service.PaymentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class ReservationCreatedEventConsumer {

    private final KafkaReceiver<String, byte[]> kafkaReceiver;
    private final ReservationCreatedEventAvroDeserializer avroDeserializer;
    private final PaymentService paymentService;
    private Disposable subscription;

    public ReservationCreatedEventConsumer(
            AppKafkaProperties properties,
            ReservationCreatedEventAvroDeserializer avroDeserializer,
            PaymentService paymentService
    ) {
        this.avroDeserializer = avroDeserializer;
        this.paymentService = paymentService;

        ReceiverOptions<String, byte[]> receiverOptions = ReceiverOptions.<String, byte[]>create(Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, properties.getConsumer().getGroupId(),
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getConsumer().getAutoOffsetReset(),
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false
                ))
                .subscription(Collections.singleton(properties.getTopics().getReservationCreated()));

        this.kafkaReceiver = KafkaReceiver.create(receiverOptions);
    }

    @PostConstruct
    public void start() {
        subscription = kafkaReceiver.receive()
                .concatMap(record -> toEvent(record.value())
                        .flatMap(paymentService::createPaymentFromReservation)
                        .doOnNext(payment -> log.info("Reservation-created event processed. reservationId={} paymentId={} status={}",
                                payment.reservationId(),
                                payment.id(),
                                payment.status()))
                        .then(Mono.fromRunnable(record.receiverOffset()::acknowledge))
                        .onErrorResume(error -> {
                            log.warn("Could not process reservation-created event. offset={} error={}",
                                    record.receiverOffset().offset(),
                                    error.getClass().getSimpleName());
                            return Mono.empty();
                        }))
                .doOnError(error -> log.error("Reservation-created event consumer stopped unexpectedly. error={}", error.getClass().getSimpleName(), error))
                .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    private Mono<ReservationCreatedEvent> toEvent(byte[] payload) {
        return Mono.fromCallable(() -> avroDeserializer.deserialize(payload));
    }
}
