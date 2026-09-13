# Reservation Service

Reservation Service owns reservation creation and lookup for the Disney Cruise Platform.

It exposes a REST API for reservations. When a reservation is created, it calls Cruise Search Service to check cruise availability, saves the reservation as `PENDING_PAYMENT`, and publishes a `reservation-created` event to Kafka using Avro.

## Responsibilities

- Accept reservation requests.
- Validate guest, cruise, and party-size inputs.
- Check cruise availability through Cruise Search Service.
- Persist reservations in MongoDB.
- Publish reservation-created events to Kafka.
- Use retry and circuit breaker behavior for external service and MongoDB operations.

## Main Technologies

- Java 17
- Spring Boot
- Spring WebFlux
- Reactive MongoDB
- Reactor Kafka
- Apache Avro
- MapStruct
- Lombok
- Resilience4j
- Logback

## Local Runtime

Default port:

```text
8085
```

Important local configuration:

```properties
app.clients.cruise-search.base-url=http://localhost:8081
app.kafka.bootstrap-servers=localhost:9092
app.kafka.reservation-created-topic=reservation-created
```

The Cruise Search bearer token is hardcoded for local POC use.

## Endpoints

```text
POST /api/v1/reservations
GET  /api/v1/reservations/{id}
```

## Events Published

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