# Disney Cruise Platform

Disney Cruise Platform is a proof-of-concept reactive microservices system for searching cruises, creating reservations, and processing payments.

The platform is intentionally small. It focuses on service boundaries, reactive persistence, Kafka-based integration, and simple local development.

## Services

### Cruise Search Service

`cruise-search-service` owns the cruise catalog.

It exposes REST endpoints to create cruises, retrieve one cruise, and list cruises. It stores cruise data in MongoDB and seeds sample Disney cruises every time the service starts so the POC is easy to run locally.

Port: `8081`

### Reservation Service

`reservation-service` owns reservation creation and reservation lookup.

It exposes REST endpoints for reservations. When a reservation is requested, it calls `cruise-search-service` to confirm the cruise is available, saves the reservation with `PENDING_PAYMENT`, and publishes a `reservation-created` event to Kafka using Avro.

Port: `8085`

### Payment Service

`payment-service` owns payment creation.

It does not expose a business REST API. It listens to the Kafka `reservation-created` topic, deserializes Avro messages, creates a payment for the reservation, and stores the payment in MongoDB. Payment creation is idempotent by reservation id.

Port: `8083`

## Local Infrastructure

The root `docker-compose.yml` starts:

- MongoDB
- Redpanda, Kafka-compatible broker
- Grafana

Start infrastructure:

```powershell
docker compose up -d
```

## Running Services

Run each service from its own directory:

```powershell
cd cruise-search-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd reservation-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd payment-service
.\mvnw.cmd spring-boot:run
```

## Technology Stack

- Java 17
- Spring Boot
- Spring WebFlux
- Reactive MongoDB
- Reactor Kafka
- Redpanda/Kafka
- Apache Avro
- MapStruct
- Lombok
- Resilience4j
- Spring Security with JWT in cruise-search-service
- Logback
