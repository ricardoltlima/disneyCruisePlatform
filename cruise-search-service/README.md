# Cruise Search Service

Cruise Search Service owns the cruise catalog for the Disney Cruise Platform.

It provides the REST API used to create cruises, retrieve a cruise by id, and list cruises. It also populates MongoDB with sample cruises every time the service starts, which keeps the POC easy to run locally.

## Responsibilities

- Store cruise catalog data in MongoDB.
- Validate incoming cruise creation requests.
- Protect cruise endpoints with JWT-based authorization.
- Return cruise availability, dates, prices, cabin counts, and status.
- Seed sample cruises on startup.

## Main Technologies

- Java 17
- Spring Boot
- Spring WebFlux
- Spring Security OAuth2 Resource Server
- Reactive MongoDB
- MapStruct
- Lombok
- Logback

## Local Runtime

Default port:

```text
8081
```

MongoDB defaults:

```properties
spring.mongodb.uri=mongodb://localhost:27017/cruise_search
spring.mongodb.database=cruise_search
```

JWT secret defaults to a local development value in `application.properties`.

## Endpoints

```text
POST /api/v1/cruises
GET  /api/v1/cruises/{id}
GET  /api/v1/cruises
```

## Run

```powershell
.\mvnw.cmd spring-boot:run
```