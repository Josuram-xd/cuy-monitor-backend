# Cuy Monitor — Backend

> Core service of **Cuy Monitor**, a guinea pig (*cuy*) health monitoring system. It receives camera, audio and weight events over HTTPS, runs them through a health-evaluation pipeline built on classic design patterns, stores everything in PostgreSQL and pushes live updates to the dashboard.

![Java](https://img.shields.io/badge/Java-25_LTS-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![Status](https://img.shields.io/badge/status-in_development-yellow)

---

## Table of contents

- [Overview](#overview)
- [System architecture](#system-architecture)
- [Design patterns](#design-patterns)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Event contracts](#event-contracts)
- [REST API](#rest-api)
- [Data model](#data-model)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Testing](#testing)
- [Contributing](#contributing)
- [Related repositories](#related-repositories)
- [Roadmap](#roadmap)

---

## Overview

Cuy Monitor watches a guinea pig cage and flags animals whose behavior changes in a **sustained** way — a common early sign of illness. Each guinea pig is identified by a color mark on its back and tracked by camera; the cage audio is classified to detect distress calls; and a load cell provides weight readings.

This repository is the **heart of the system**. It:

- Receives normalized events (`BEHAVIOR`, `AUDIO`, `WEIGHT`) through a single endpoint: `POST /api/ingestion/events`.
- Evaluates them through a **Chain of Responsibility** and updates each guinea pig's **health state**: `NORMAL → OBSERVED → ALERT → CRITICAL` (and back).
- Aggregates the health of the whole cage with a **Composite** tree.
- Notifies subscribers (dashboard via WebSocket, database, application log) through an **Observer**.
- Exposes a REST API and a WebSocket endpoint for the dashboard, plus the HTTPS ingestion endpoint used by the AI service and the weight sensor.
- Holds the deployment infrastructure (`infra/`) and the cross-repo contracts (`docs/contracts/`).

## System architecture

```
      GUINEA PIG CAGE (farm)                              AWS — EC2 (Docker Compose)
┌──────────────────────────────┐          ┌─────────────────────────────────────────────────┐
│ Phone camera (IP Webcam)     │          │  Caddy (HTTPS :443)                             │
│        │ video + audio       │          │    ├── /api/**, /ws/** ──► backend  (:8080)     │
│        ▼                     │  HTTPS   │    └── /ai/**          ──► ai-service (:8000)   │
│ Edge laptop                  │ +API key │                                                 │
│  ├─ edge_agent ──────────────┼─────────►│  ai-service ── POST /api/ingestion/events ──┐   │
│  │   frames 1–2 fps + audio  │          │                                             ▼   │
│  └─ serial_bridge ───────────┼─────────►│  POST /api/ingestion/events ──► backend (Java)  │
│        ▲ USB serial          │          │                                    │            │
│ Arduino + HX711 load cell    │          │                                    ▼            │
└──────────────────────────────┘          │                              PostgreSQL 18      │
                                          └─────────────────────────────────────────────────┘
                                                            ▲  REST + WebSocket (HTTPS)
                                                  ┌─────────┴─────────┐
                                                  │ Dashboard (React) │
                                                  └───────────────────┘
```

**Key architectural decisions**

| Decision | Rationale |
|---|---|
| Multi-repo (backend, AI service, dashboard, Arduino) | Independent deliverables; contracts live in this repo as the single source of truth |
| AI in a separate Python service | Training/inference tooling is native to Python; the backend only knows about events |
| HTTP ingestion, no message broker | One pilot cage produces a few events per minute; a single endpoint handles it and saves ~400 MB of RAM (ADR-003) |
| Only Caddy is public | External clients reach the system through HTTPS + API key; Postgres is never exposed |
| Single EC2 + Docker Compose | Low cost and complexity for a pilot cage; a broker can be added behind the same envelope if it grows to many cages |

### How an event flows through the backend

1. The AI service sends a `BEHAVIOR` event to `POST /api/ingestion/events` (one per guinea pig, every 60 s window).
2. `IngestionController` checks the `X-API-Key` header; the **Factory Method** picks the right adapter for the event `type`.
3. The **Adapter** converts the raw payload into the internal `HealthEvent` record.
4. The **Chain of Responsibility** validates, identifies, evaluates thresholds and confirms the anomaly is sustained.
5. The guinea pig's **State** transitions (e.g. `NORMAL → OBSERVED`).
6. The **Composite** (`CageHealth`) recomputes the cage-level health.
7. The **Observer** (`AlertPublisher`) notifies the WebSocket, database and log observers.
8. The dashboard receives the update in real time.

## Design patterns

The health evaluation logic is intentionally built around six GoF patterns:

| Pattern | Package | Role in the system |
|---|---|---|
| **Factory Method** | `ingestion.factory` | `AdapterFactory` creates the right adapter for each event source (camera, audio, weight) |
| **Adapter** | `ingestion.adapter` | Converts heterogeneous source payloads into a unified `HealthEvent` |
| **Chain of Responsibility** | `health.chain` | `ValidationHandler → IdentificationHandler → BehaviorThresholdHandler → SustainedAnomalyHandler` |
| **State** | `health.state` | Per-guinea-pig lifecycle: `NormalState`, `ObservedState`, `AlertState`, `CriticalState` |
| **Composite** | `health.composite` | `CageHealth` aggregates `GuineaPigHealth`, `CageAudioHealth` and `CageWeightHealth` |
| **Observer** | `notification` | `AlertPublisher` fans out to `WebSocketAlertObserver`, `DatabaseAlertObserver`, `LogAlertObserver` |

```
AdapterFactory ─► EventSourceAdapter ─► HealthEvent ─► EventHandler chain ─► HealthState ─► CageHealth
                                                                                               │
AlertPublisher ◄──────────────────────── (status change / alert) ◄─────────────────────────────┘
   └─► WebSocketAlertObserver ─────────────────────────────────────────────────────► Dashboard
```

> The Observer is implemented explicitly (interface + list of observers) rather than with Spring's `ApplicationEventPublisher`, so the pattern stays visible in the code.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 25 LTS (Eclipse Temurin) |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, WebSocket, Actuator) |
| Ingestion | HTTP (`POST /api/ingestion/events` + `X-API-Key`) |
| Database | PostgreSQL 18 + Flyway migrations |
| Build | Maven (wrapper included) |
| Testing | JUnit 5, Spring Boot test starters, Testcontainers |
| Runtime | Docker, Docker Compose, Caddy 2.11 (automatic HTTPS) |
| Cloud | AWS EC2 (Ubuntu 26.04 LTS) |

> **Spring Boot 4 notes:** the web starter is `spring-boot-starter-webmvc`, Flyway needs `spring-boot-starter-flyway` + `flyway-database-postgresql`, and Jackson 3 lives under the `tools.jackson` package.

## Project structure

```
cuy-monitor-backend/
├── pom.xml
├── Dockerfile
├── docs/
│   ├── contracts/              # Event format, ingestion endpoint and REST API (source of truth)
│   ├── adr/                    # Architecture decision records
│   └── diagrams/               # UML class diagram per pattern
├── infra/
│   ├── docker-compose.yml      # Production stack (EC2)
│   ├── docker-compose.dev.yml  # Local stack: Postgres only
│   ├── Caddyfile
│   └── .env.example
├── dev/
│   └── fake-producer/          # Sends sample events to /api/ingestion/events
└── src/
    ├── main/java/com/cuymonitor/backend/
    │   ├── config/             # WebSocket, CORS, API key filter
    │   ├── domain/
    │   │   ├── model/          # Cage, GuineaPig, HealthEvent, Alert, WeightReading, ...
    │   │   └── repository/     # Spring Data JPA repositories
    │   ├── ingestion/
    │   │   ├── dto/            # Event DTOs and payload records
    │   │   ├── factory/        # Factory Method
    │   │   └── adapter/        # Adapter
    │   ├── health/
    │   │   ├── chain/          # Chain of Responsibility
    │   │   ├── state/          # State
    │   │   └── composite/      # Composite
    │   ├── notification/       # Observer
    │   └── api/                # REST controllers and DTOs
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/       # Flyway: V1__initial_schema.sql, V2__..., ...
    └── test/java/com/cuymonitor/backend/
```

## Event contracts

### Ingestion endpoint

Every event from outside enters through one endpoint. The `AdapterFactory` chooses the adapter from `type`.

```http
POST /api/ingestion/events
X-API-Key: <API_KEY>
Content-Type: application/json
```

| Producer | Event types |
|---|---|
| ai-service | `BEHAVIOR`, `AUDIO` |
| arduino (serial bridge) | `WEIGHT` |

`eventId` is generated by the producer and is used to drop duplicates when a producer retries.

### Event envelope

```json
{
  "eventId": "uuid",
  "type": "BEHAVIOR | AUDIO | WEIGHT",
  "cageId": "cage-1",
  "timestamp": "2026-10-05T14:32:00Z",
  "source": "ai-service | arduino",
  "schemaVersion": 1,
  "payload": { }
}
```

### Payloads

```jsonc
// BEHAVIOR — one per guinea pig, every 60 s window
{ "color": "RED", "windowSeconds": 60, "stillSeconds": 48, "feederVisits": 0,
  "watererVisits": 1, "avgGroupDistance": 0.72, "probAnomaly": 0.81, "detectionConfidence": 0.93 }

// AUDIO — one per ~1 s clip (or aggregated every 10 s)
{ "label": "DISTRESS | NORMAL", "probability": 0.88, "durationMs": 960 }

// WEIGHT
{ "grams": 812.4, "stable": true }
```

### Shared enums

| Enum | Values |
|---|---|
| `MarkColor` | `RED, BLUE, GREEN, YELLOW, ORANGE, PURPLE, BLACK, WHITE` |
| `HealthStatus` | `NORMAL, OBSERVED, ALERT, CRITICAL` |
| `EventType` | `BEHAVIOR, AUDIO, WEIGHT` |
| `AlertStatus` | `OPEN, REVIEWED` |

## REST API

| Method & path | Consumer | Description |
|---|---|---|
| `GET /api/cages/{id}/health` | dashboard | Cage health summary (from the Composite) |
| `GET /api/cages/{id}/guinea-pigs` | dashboard | Guinea pigs with their current state |
| `POST /api/cages/{id}/guinea-pigs` | dashboard | Register a guinea pig (name + mark color) |
| `GET /api/guinea-pigs/{id}/history?from=&to=` | dashboard | Behavior and state history |
| `GET /api/alerts?status=OPEN` | dashboard | List alerts |
| `PATCH /api/alerts/{id}` | dashboard | Mark an alert as `REVIEWED` |
| `GET /api/cages/{id}/weight?from=&to=` | dashboard | Weight history |
| `POST /api/ingestion/weight` | serial bridge | Ingest a weight reading (requires `X-API-Key`) |
| `WS /ws` → `/topic/cages/{id}` | dashboard | Live updates (STOMP) |
| `GET /actuator/health` | all / Caddy | Liveness and database health |

## Data model

```
cage              (id, name, location, created_at)
guinea_pig        (id, cage_id → cage, name, mark_color, current_status, active, created_at)
event             (id uuid, type, cage_id, guinea_pig_id NULL, source, occurred_at, payload JSONB)
state_transition  (id, guinea_pig_id, from_status, to_status, reason, occurred_at)
alert             (id, cage_id, guinea_pig_id NULL, level, type, message, status, created_at, reviewed_at)
weight_reading    (id, cage_id, grams, stable, measured_at)
baseline_profile  (guinea_pig_id, avg_still_seconds, avg_feeder_visits, avg_group_distance, updated_at)
```

`guinea_pig_id` is `NULL` for audio events and alerts, since audio refers to the whole cage. The schema is managed exclusively by Flyway — never edit a migration that has already been applied; add a new `V{n}__description.sql` instead.

## Getting started

### Prerequisites

- **JDK 25** ([Eclipse Temurin](https://adoptium.net))
- **Docker** and Docker Compose
- Git

Maven does not need to be installed — use the included wrapper (`./mvnw` or `mvnw.cmd` on Windows).

### 1. Clone

```bash
git clone https://github.com/Josuram-xd/cuy-monitor-backend.git
cd cuy-monitor-backend
```

### 2. Start local infrastructure (PostgreSQL)

```bash
docker compose -f infra/docker-compose.dev.yml up -d
docker compose -f infra/docker-compose.dev.yml ps
```

### 3. Run the backend

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

The service starts on `http://localhost:8080`. On startup, Flyway applies pending migrations.

### 4. Verify

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Configuration

The application reads its configuration from environment variables (with local defaults in `application.yml`).

| Variable | Description | Local default |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | PostgreSQL location | `localhost` / `5432` / `cuymonitor` |
| `DB_USER` / `DB_PASSWORD` | Database credentials | `cuymonitor` / `cuymonitor` |
| `APP_API_KEY` | Key required in the `X-API-Key` header for the ingestion endpoint | `dev-key` |

For production, secrets live in `infra/.env` (created from `infra/.env.example`), which is **never committed**:

```dotenv
DOMAIN=your-domain.example.org
POSTGRES_DB=cuymonitor
POSTGRES_USER=cuymonitor
POSTGRES_PASSWORD=<random value>
API_KEY=<random value>
```

Generate random values with `openssl rand -hex 24`.

## Deployment

The whole stack runs on a single AWS EC2 instance with Docker Compose:

| Service | Image |
|---|---|
| backend | built from this repo (`eclipse-temurin:25-jre`) |
| postgres | `postgres:18` |
| caddy | `caddy:2.11` |
| ai-service | built from `cuy-monitor-ai-service` (`--profile ai`) |

Only Caddy exposes ports (80/443); PostgreSQL (5432) stays inside the Docker network. All services use `restart: unless-stopped`, and image versions are always pinned.

```bash
cd infra
cp .env.example .env            # fill in secrets
docker compose up -d --build
docker compose ps               # postgres should be "healthy"
```

Update to a new version:

```bash
git pull
cd infra
docker compose up -d --build backend
```

## Testing

```bash
./mvnw test
```

| Scope | Location |
|---|---|
| State transitions | `src/test/.../health/state` |
| Individual chain handlers | `src/test/.../health/chain` |
| Composite aggregation | `src/test/.../health/composite` |
| Factory + adapters | `src/test/.../ingestion` |
| Ingestion + Postgres integration | `src/test/.../integration` (Testcontainers — Docker required) |

## Contributing

- `main` is protected; every change goes through a Pull Request reviewed by the other team member.
- Branch names in English: `feature/state-transitions`, `fix/adapter-missing-color`.
- Commits follow [Conventional Commits](https://www.conventionalcommits.org): `feat(state): add OBSERVED to ALERT transition`, `docs(contracts): add WEIGHT payload`.
- Code, identifiers, endpoints, JSON fields, commits and docs are written in **English**.
- Contract changes (`docs/contracts/`) are agreed with the team before pushing, and mirrored in the other repositories.

## Related repositories

| Repository | Description |
|---|---|
| `cuy-monitor-backend` | **This repo** — Java core, patterns, API, infrastructure and contracts |
| `cuy-monitor-ai-service` | Python + FastAPI — detection (YOLO26n / ONNX), tracking, behavior and audio classification |
| `cuy-monitor-dashboard` | React + TypeScript — cage overview, guinea pig history, alerts and weight charts |
| `cuy-monitor-arduino` | Arduino + HX711 weight sensor firmware and the serial bridge |

## Roadmap

- [x] Spring Boot 4.1 + Java 25 project skeleton
- [ ] Initial schema migration
- [x] Direct HTTP ingestion endpoint (ADR-003)
- [ ] Deployment on AWS (EC2 + Docker Compose + Caddy HTTPS)
- [ ] Ingestion layer: Factory Method + Adapters
- [ ] Health core: Chain of Responsibility, State, Composite
- [ ] Notifications: Observer with WebSocket, database and log observers
- [ ] REST API and live WebSocket updates for the dashboard
- [ ] Per-guinea-pig baseline profiles and sustained-anomaly windows
- [ ] Integration tests with Testcontainers
- [ ] UML diagrams per pattern

---

<sub>Academic project — Software Design Patterns course, 2026.</sub>
