# Disney Cruise Platform

Reactive Java microservices practice project for a cruise search, reservation, and payment platform.

## Services

- `cruise-search-service`: searches and manages cruise catalog data.
- `reservation-service`: will manage reservations and booking state.
- `payment-service`: will simulate payment authorization and payment results.

## Local Infrastructure

Start MongoDB from the repository root:

```powershell
docker compose up -d
```

## Cruise Search Service

Run the service:

```powershell
cd cruise-search-service
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:8081
```

Create a cruise:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8081/cruises `
  -ContentType "application/json" `
  -Body '{
    "shipName": "Disney Wish",
    "destination": "Bahamas",
    "departurePort": "Port Canaveral",
    "departureDate": "2026-11-20",
    "returnDate": "2026-11-24",
    "availableCabinsByType": {
      "INSIDE": 20,
      "OCEAN_VIEW": 15,
      "VERANDAH": 10,
      "CONCIERGE": 3
    },
    "basePrice": 1299.99,
    "status": "SCHEDULED"
  }'
```

Search cruises:

```powershell
Invoke-RestMethod "http://localhost:8081/cruises?destination=Bahamas&status=SCHEDULED"
```
