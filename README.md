# Cuy Monitor — Backend

> Core service of **Cuy Monitor**, a guinea pig (*cuy*) health monitoring system. It receives camera, audio and weight events over HTTPS, runs them through a health-evaluation pipeline built on classic design patterns, stores everything in PostgreSQL (Amazon RDS), pushes live updates to the dashboard and manages the user accounts that protect it.

![Java](https://img.shields.io/badge/Java-25_LTS-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18_(RDS)-4169E1?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![AWS](https://img.shields.io/badge/AWS-EC2_+_RDS-FF9900?logo=amazonaws)
![Status](https://img.shields.io/badge/status-in_development-yellow)

---

## Table of contents

- [Overview](#overview)
- [System architecture](#system-architecture)
- [Design patterns](#design-patterns)
- [Users and authentication](#users-and-authentication)
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

- Receives normalized events (`BEHAVIOR`, `AUDIO`, `WEIGHT`) through a single endpoint: `POST /api/v1/ingestion/events`.
- Evaluates them through a **Chain of Responsibility** and updates each guinea pig's **health state**: `NORMAL → OBSERVED → ALERT → CRITICAL` (and back).
- Aggregates the health of the whole cage with a **Composite** tree.
- Notifies subscribers (dashboard via WebSocket, database, application log) through an **Observer**.
- Manages **user accounts**: registration, login with password + a one-time code sent by email, and JWT-protected access to the API and WebSocket. There is a single kind of user (whoever looks at the dashboard), no roles.
- Exposes a REST API and a WebSocket endpoint for the dashboard, plus the HTTPS ingestion endpoint used by the AI service and the weight sensor.
- Holds the deployment infrastructure (`infra/`) and the cross-repo contracts (`docs/contracts/`).

The **database schema** is not in this repo: it lives in [`cuy-monitor-db`](https://github.com/Josuram-xd/cuy-monitor-db) (Flyway migrations). This backend only validates it.

## System architecture

```
      GUINEA PIG CAGE (farm)                         AWS — one region, one VPC
┌──────────────────────────────┐       ┌──────────────────────────────────────────────────────────┐
│ Phone camera (IP Webcam)     │       │ EC2 (Docker Compose)                                     │
│        │ video + audio       │       │  Caddy (HTTPS :443)                                      │
│        ▼                     │ HTTPS │    ├── /api/**, /ws, /actuator/health ──► backend (:8080) │
│ Edge laptop                  │ + API │    ├── /ai/**                         ──► ai-service      │
│  ├─ edge_agent ──────────────┼──key─►│    └── /  (SPA)                       ──► dashboard       │
│  │   frames 1–2 fps + audio  │       │  ai-service ── POST /api/v1/ingestion/events ──► backend │
│  └─ serial_bridge ───────────┼──────►│  migrate (one-shot, from cuy-monitor-db) ─┐     │        │
│        ▲ USB serial          │       │                                           ▼     ▼ TLS    │
│ Arduino + HX711 load cell    │       │  Amazon RDS for PostgreSQL 18 (private, no public IP)    │
└──────────────────────────────┘       └──────────────────────────────────────────────────────────┘
                                                        ▲
              Farmer's browser ── HTTPS: dashboard + REST (session cookie) + WSS /ws (same cookie)
```

**Key architectural decisions** (full ADRs in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md))

| Decision | Rationale |
|---|---|
| Multi-repo (backend, db, AI service, dashboard, Arduino) | Independent deliverables; contracts live in this repo as the single source of truth |
| AI in a separate Python service | Training/inference tooling is native to Python; the backend only knows about events |
| HTTP ingestion, no message broker (ADR-003) | One pilot cage produces a few events per minute; a single endpoint handles it |
| Pragmatic hexagonal architecture (ADR-007) | Pure-Java core with the patterns, testable without Spring or a database |
| Schema in its own repo (ADR-008) | `cuy-monitor-db` owns the Flyway migrations; a one-shot `migrate` container applies them before the backend starts |
| Amazon RDS instead of Supabase or a Postgres container (ADR-009) | Private network in the same VPC, automated backups, paid with AWS credits |
| EC2 + Docker Compose instead of Lambda + SAM (ADR-010) | STOMP WebSocket needs long-lived connections, load is constant 24/7, ML models load once |
| Own accounts: password + email OTP → JWT (ADR-011) | One user type; dashboard served from the same domain, so no CORS |
| Only Caddy is public | External clients reach the system through HTTPS; the database is never exposed |

### How an event flows through the backend

1. The AI service sends a `BEHAVIOR` event to `POST /api/v1/ingestion/events` (one per guinea pig, every 60 s window).
2. `IngestionController` checks the `X-API-Key` header; the **Factory Method** picks the right adapter for the event `type`.
3. The **Adapter** converts the raw payload into the internal `HealthEvent` record.
4. The **Chain of Responsibility** validates, identifies, evaluates thresholds and confirms the anomaly is sustained.
5. The guinea pig's **State** transitions (e.g. `NORMAL → OBSERVED`).
6. The **Composite** (`CageHealth`) recomputes the cage-level health.
7. The **Observer** (`AlertPublisher`) notifies the WebSocket, database and log observers.
8. The logged-in dashboard receives the update in real time.

## Design patterns

The health evaluation logic is intentionally built around six GoF patterns, written by hand inside a hexagonal core:

| Pattern | Package | Role in the system |
|---|---|---|
| **Factory Method** | `adapter.in.ingestion.factory` | `AdapterFactory` (abstract creator) and one concrete factory per source (camera, audio, weight) |
| **Adapter** | `adapter.in.ingestion.adapter` | Converts heterogeneous source payloads into a unified `HealthEvent` |
| **Chain of Responsibility** | `domain.health.chain` | `ValidationHandler → IdentificationHandler → BehaviorThresholdHandler → SustainedAnomalyHandler` |
| **State** | `domain.health.state` | Per-guinea-pig lifecycle: `NormalState`, `ObservedState`, `AlertState`, `CriticalState` |
| **Composite** | `domain.health.composite` | `CageHealth` aggregates `GuineaPigHealth`, `CageAudioHealth` and `CageWeightHealth` |
| **Observer** | `domain.notification` + `adapter.out.notification` | `AlertPublisher` fans out to `WebSocketAlertObserver`, `DatabaseAlertObserver`, `LogAlertObserver` through the `AlertObserver` port |

```
AdapterFactory ─► EventSourceAdapter ─► HealthEvent ─► EventHandler chain ─► HealthState ─► CageHealth
                                                                                               │
AlertPublisher ◄──────────────────────── (status change / alert) ◄─────────────────────────────┘
   └─► WebSocketAlertObserver ─────────────────────────────────────────────────────► Dashboard
```

> The Observer is implemented explicitly (port + list of observers) rather than with Spring's `ApplicationEventPublisher`, so the pattern stays visible in the code.

## Users and authentication

| Step | Endpoint | Result |
|---|---|---|
| Register | `POST /api/v1/auth/register` | Account `PENDING_VERIFICATION`, 6-digit code emailed |
| Log in | `POST /api/v1/auth/login` | Password checked, new code emailed |
| Verify code | `POST /api/v1/auth/otp/verify` | Account `ACTIVE` (if it was pending) + session cookies (`access_token` 15 min, `refresh_token` 7 days), both `HttpOnly` |
| Use the app | any `/api/v1/**` and the `/ws` handshake: the browser sends the cookie by itself | |
| Stay in | `POST /api/v1/auth/refresh` rotates both cookies | |
| Log out | `POST /api/v1/auth/logout` revokes the tokens on the server and expires the cookies | |
| My account | `GET/PUT /api/v1/account/profile`, `PUT /api/v1/account/password`, `DELETE /api/v1/account` | Profile, password, soft-delete |

- Passwords and codes are stored as **BCrypt** hashes. Codes are single-use, expire in 5 min, max 5 attempts.
- JWT signed with HS256 (`APP_JWT_SECRET`, ≥ 32 bytes) with a `jti`; revoked ones are rejected. The refresh token rotates on every use and is stored hashed. No roles.
- Ingestion (`/api/v1/ingestion/**`) keeps using `X-API-Key`: producers are not users.
- Not in this version: password recovery, email change, rate limiting.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 25 LTS (Eclipse Temurin) |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, WebSocket, Security, OAuth2 Resource Server, Mail, Actuator) |
| Ingestion | HTTP (`POST /api/v1/ingestion/events` + `X-API-Key`) |
| Auth | BCrypt + JWT (HS256, Nimbus) + email OTP (SMTP: Amazon SES or Gmail) |
| Database | PostgreSQL 18 on Amazon RDS; schema managed by Flyway in `cuy-monitor-db` |
| Build | Maven (wrapper included) |
| Testing | JUnit 5, Spring Boot test starters, Spring Security test, Testcontainers, ArchUnit |
| Runtime | Docker, Docker Compose, Caddy 2.11 (automatic HTTPS) |
| Cloud | AWS EC2 (Ubuntu 26.04 LTS) + Amazon RDS |

> **Spring Boot 4 notes:** the web starter is `spring-boot-starter-webmvc`, the JWT resource server is `spring-boot-starter-security-oauth2-resource-server`, and Jackson 3 lives under the `tools.jackson` package.

## Project structure

```
cuy-monitor-backend/
├── pom.xml
├── Dockerfile
├── docs/
│   ├── PRD.md                  # Product requirements (whole system)
│   ├── ARCHITECTURE.md         # Hexagon, patterns, auth, deployment, ADRs
│   └── contracts/              # Events, REST API, auth API (source of truth) — planned
├── infra/
│   ├── docker-compose.yml      # Production stack (EC2): caddy, migrate, backend, dashboard, ai-service
│   ├── Caddyfile
│   └── .env.example
├── dev/
│   └── fake-producer/          # Sends sample events to /api/v1/ingestion/events — planned
└── src/
    ├── main/java/com/cuymonitor/backend/
    │   ├── domain/             # Pure Java: model (+ user, auth), health (chain, state, composite),
    │   │                       # notification (Observer subject), port/in, port/out, exception
    │   ├── application/        # Use case services
    │   ├── adapter/
    │   │   ├── in/web/         # REST controllers + DTOs
    │   │   ├── in/ingestion/   # Factory Method + Adapter
    │   │   └── out/            # persistence (JPA), notification, security (BCrypt, JWT), mail (OTP)
    │   └── config/             # DomainConfig, SecurityConfig, WebSocketConfig, ...
    ├── main/resources/
    │   └── application.yml
    └── test/java/com/cuymonitor/backend/
```

The backend has no migrations: Flyway is off at runtime and the schema comes from `cuy-monitor-db`.

## Event contracts

### Ingestion endpoint

Every event from outside enters through one endpoint. The `AdapterFactory` chooses the adapter from `type`.

```http
POST /api/v1/ingestion/events
X-API-Key: <API_KEY>
Content-Type: application/json
```

| Producer | Event types |
|---|---|
| ai-service | `BEHAVIOR`, `AUDIO` |
| arduino (serial bridge) | `WEIGHT` |

Responses: `202 Accepted` · `400` invalid envelope (don't retry) · `401` wrong key (don't retry) · `5xx` retry with backoff. `eventId` is generated by the producer and is used to drop duplicates when a producer retries.

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
| `UserStatus` | `PENDING_VERIFICATION, ACTIVE, DISABLED` |

## REST API

| Method & path | Auth | Consumer | Description |
|---|---|---|---|
| `GET /actuator/health` | public | all / Caddy | Liveness and database health |
| `POST /api/v1/ingestion/events` | `X-API-Key` | ai-service, serial bridge | Ingest any event |
| `POST /api/v1/auth/register` | public | dashboard | Create an account (sends a code) |
| `POST /api/v1/auth/login` | public | dashboard | Check password (sends a code) |
| `POST /api/v1/auth/otp/verify` | public | dashboard | Exchange the code for the session cookies |
| `POST /api/v1/auth/refresh` | refresh cookie | dashboard | Rotate the session cookies |
| `POST /api/v1/auth/logout` | cookies | dashboard | End the session on the server |
| `GET` / `PUT /api/v1/account/profile` | JWT | dashboard | View / update my profile |
| `PUT /api/v1/account/password` | JWT | dashboard | Change my password |
| `DELETE /api/v1/account` | JWT | dashboard | Deactivate my account |
| `GET /api/v1/cages/{id}/health` | JWT | dashboard | Cage health summary (from the Composite) |
| `GET /api/v1/cages/{id}/guinea-pigs` | JWT | dashboard | Guinea pigs with their current state |
| `POST /api/v1/cages/{id}/guinea-pigs` | JWT | dashboard | Register a guinea pig (name + mark color) |
| `GET /api/v1/guinea-pigs/{id}/history?from=&to=` | JWT | dashboard | Behavior and state history |
| `GET /api/v1/alerts?status=OPEN` | JWT | dashboard | List alerts |
| `PATCH /api/v1/alerts/{id}` | JWT | dashboard | Mark an alert as `REVIEWED` |
| `GET /api/v1/cages/{id}/weight?from=&to=` | JWT | dashboard | Weight history |
| `WS /ws` → `/topic/cages/{id}` | JWT on STOMP `CONNECT` | dashboard | Live updates |

Only `/actuator/health`, `/api/v1/system/status` (temporary) and `/api/v1/ingestion/events` exist today; the rest is in progress (see [`TASKS.md`](TASKS.md)).

## Data model

Owned by [`cuy-monitor-db`](https://github.com/Josuram-xd/cuy-monitor-db) — see its `docs/ARCHITECTURE.md` for the full data dictionary.

```
cage              (id, code 'cage-1', name, location, created_at)
app_user          (id uuid, username, full_name, email, password_hash, status, created_at, updated_at)
otp_challenge     (id uuid, user_id → app_user, code_hash, expires_at, attempts, used_at, revoked_at, ...)
guinea_pig        (id, cage_id → cage, name, mark_color, current_status, active, created_at)
event             (id uuid, type, cage_id, guinea_pig_id NULL, source, occurred_at, payload JSONB)
state_transition  (id, guinea_pig_id, from_status, to_status, reason, occurred_at)
alert             (id, cage_id, guinea_pig_id NULL, level, type, message, status, created_at, reviewed_at)
weight_reading    (id, cage_id, grams, stable, measured_at)
baseline_profile  (guinea_pig_id, avg_still_seconds, avg_feeder_visits, avg_group_distance, updated_at)
```

`guinea_pig_id` is `NULL` for audio and weight, since they refer to the whole cage. The backend runs with `ddl-auto: validate`: if a table doesn't match its JPA entity, it won't start.

## Getting started

### Prerequisites

- **JDK 25** ([Eclipse Temurin](https://adoptium.net))
- **Docker** and Docker Compose
- Git

Maven does not need to be installed — use the included wrapper (`./mvnw` or `mvnw.cmd` on Windows).

### 1. Clone the repos side by side

```bash
mkdir cuy && cd cuy
git clone https://github.com/Josuram-xd/cuy-monitor-backend.git
git clone https://github.com/Josuram-xd/cuy-monitor-db.git
cd cuy-monitor-backend
```

Compose and the integration tests rely on `../cuy-monitor-db` (and `../cuy-monitor-dashboard`, `../cuy-monitor-ai-service` for the full stack).

### 2. Start the local database

```bash
docker compose -f ../cuy-monitor-db/docker-compose.yml up -d   # Postgres 18 on localhost:5432 + migrations + dev seeds
```

> `infra/docker-compose.dev.yml` starts an **empty** Postgres (no tables); use it only if you apply the migrations of `cuy-monitor-db` yourself.

### 3. Run the backend

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev     # Windows: mvnw.cmd ...
```

The service starts on `http://localhost:8080`. With the `dev` profile, OTP codes are written to the log instead of being emailed.

### 4. Verify

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Configuration

The application reads its configuration from environment variables (with local defaults in `application.yml`).

| Variable | Description | Local default |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | PostgreSQL location (RDS endpoint in prod) | `localhost` / `5432` / `cuymonitor` |
| `DB_USER` / `DB_PASSWORD` | Database credentials | `cuymonitor` / `cuymonitor` |
| `APP_API_KEY` | Key required in the `X-API-Key` header for the ingestion endpoint | `dev-key` |
| `APP_JWT_SECRET` | HS256 secret, ≥ 32 bytes | dev-only value |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` | SMTP for the OTP emails (SES or Gmail) | not needed with profile `dev` |
| `SPRING_PROFILES_ACTIVE` | `dev` locally, `prod` on the EC2 | — |

For production, secrets live in `infra/.env` (created from `infra/.env.example`), which is **never committed**:

```dotenv
DOMAIN=your-domain.example.org
DB_HOST=<rds-endpoint>.rds.amazonaws.com
DB_PORT=5432
DB_NAME=cuymonitor
DB_USER=cuymonitor
DB_PASSWORD=<random value>
API_KEY=<random value>
APP_JWT_SECRET=<random value, openssl rand -base64 48>
MAIL_HOST=email-smtp.<region>.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=<smtp user>
MAIL_PASSWORD=<smtp password>
MAIL_FROM=no-reply@your-domain.example.org
```

Generate random values with `openssl rand -hex 24`.

## Deployment

Everything runs on AWS: one EC2 instance with Docker Compose and an Amazon RDS for PostgreSQL instance in the same VPC.

| Service | Image | Public |
|---|---|---|
| caddy | `caddy:2.11` | 80, 443 |
| migrate | built from `../../cuy-monitor-db` (Flyway); runs once and exits | no |
| backend | built from this repo (`eclipse-temurin:25-jre`); starts after `migrate` succeeds | no |
| dashboard | built from `../../cuy-monitor-dashboard` (static files served by Caddy) | no |
| ai-service | built from `../../cuy-monitor-ai-service` (`--profile ai`) | no |
| database | **Amazon RDS** PostgreSQL 18, `db.t4g.micro`, private, only reachable from the EC2 security group | no |

Only Caddy exposes ports (80/443). All services use `restart: unless-stopped`, and image versions are always pinned.

```bash
# on the EC2, with the repos cloned side by side in ~/cuy
cd ~/cuy/cuy-monitor-backend/infra
cp .env.example .env            # fill in secrets
docker compose up -d --build --remove-orphans
docker compose logs migrate     # migrations applied on RDS
docker compose ps
```

Update to a new version:

```bash
git -C ~/cuy/cuy-monitor-db pull && git pull
cd infra
docker compose up -d --build
```

> Until Task 22 is done, production still uses the `postgres:18` container defined in `infra/docker-compose.yml`.

## Testing

```bash
./mvnw test
```

| Scope | Location |
|---|---|
| State transitions, chain handlers, composite | `src/test/.../domain/health` (plain JUnit) |
| User and OTP rules | `src/test/.../domain/model` (plain JUnit) |
| Use case services | `src/test/.../application` (in-memory fakes) |
| Factory + adapters | `src/test/.../adapter/in/ingestion` |
| Controllers and protected routes | `src/test/.../adapter/in/web` (`@WebMvcTest` + Spring Security test) |
| Hexagonal dependency rules | `src/test/.../architecture` (ArchUnit) |
| Persistence + end to end | `src/test/.../integration` (Testcontainers + `../cuy-monitor-db/migrations` — Docker required) |

## Contributing

- `main` is protected; every change goes through a Pull Request reviewed by the other team member.
- **Commits, pushes and PRs are made by people, never by AI agents** — not even when asked. No `Co-Authored-By` or AI signatures in commits or PRs. See [`AGENTS.md`](AGENTS.md).
- Branch names in English: `feature/task-18-auth-jwt-otp`, `fix/adapter-missing-color`.
- Commits follow [Conventional Commits](https://www.conventionalcommits.org): `feat(state): add OBSERVED to ALERT transition`, `docs(contracts): add WEIGHT payload`.
- Code, identifiers, endpoints, JSON fields and commits are written in **English**; team docs may be in Spanish.
- Schema changes go first to `cuy-monitor-db`; contract changes (`docs/contracts/`) are agreed with the team and mirrored in the other repositories.

## Related repositories

| Repository | Description |
|---|---|
| `cuy-monitor-backend` | **This repo** — Java core, patterns, auth, API, infrastructure and contracts |
| `cuy-monitor-db` | PostgreSQL schema: Flyway migrations, local database, `migrate` image for RDS |
| `cuy-monitor-ai-service` | Python + FastAPI — detection (YOLO26n / ONNX), tracking, behavior and audio classification |
| `cuy-monitor-dashboard` | React + TypeScript — login, cage overview, guinea pig history, alerts and weight charts |
| `cuy-monitor-arduino` | Arduino + HX711 weight sensor firmware and the serial bridge |

## Roadmap

- [x] Spring Boot 4.1 + Java 25 project skeleton
- [x] Initial schema migrations (`cage`, `app_user`, `otp_challenge`)
- [x] Direct HTTP ingestion endpoint (ADR-003)
- [x] Deployment on AWS (EC2 + Docker Compose + Caddy HTTPS)
- [x] Hexagonal package layout
- [x] Auth domain, ports and JPA persistence
- [ ] Auth service, security config, JWT, email OTP and endpoints (Task 18)
- [ ] Account CRUD and WebSocket protected with JWT (Tasks 19–20)
- [ ] Schema moved to `cuy-monitor-db` and database on Amazon RDS (Tasks 21–22)
- [ ] Dashboard served from the same Compose stack (Task 23)
- [ ] Ingestion layer: Factory Method + Adapters
- [ ] Health core: Chain of Responsibility, State, Composite
- [ ] Notifications: Observer with WebSocket, database and log observers
- [ ] REST API and live WebSocket updates for the dashboard
- [ ] Per-guinea-pig baseline profiles and sustained-anomaly windows
- [ ] Integration and architecture tests (Testcontainers, ArchUnit)
- [ ] UML diagrams per pattern

---

<sub>Academic project — Software Design Patterns course, 2026.</sub>
