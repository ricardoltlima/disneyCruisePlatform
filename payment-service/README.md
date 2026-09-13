# Payment Service

Payment Service owns payment creation for the Disney Cruise Platform.

It is event-driven and does not expose a business REST API. It listens to the Kafka `reservation-created` topic, reads Avro messages, creates a payment for each reservation, and saves the payment in MongoDB.

## Responsibilities

- Consume reservation-created events from Kafka.
- Deserialize Avro event payloads.
- Create completed payment records for reservations.
- Avoid duplicate payments by reservation id.
- Persist payments in MongoDB.

## Main Technologies

- Java 17
- Spring Boot
- Spring WebFlux
- Reactive MongoDB
- Reactor Kafka
- Apache Avro
- MapStruct
- Lombok
- Logback

## Local Runtime

Default port:

```text
8083
```

Important local configuration:

```properties
app.kafka.bootstrap-servers=localhost:9092
app.kafka.consumer-group-id=payment-service
app.kafka.reservation-created-topic=reservation-created
```

## Events Consumed

Topic:

```text
reservation-created
```

Payload format:

```text
Avro binary
```

Schema:

```text
src/main/resources/avro/reservation-created-event.avsc
```

## Run

```powershell
.\mvnw.cmd spring-boot:run
```