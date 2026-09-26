# Architecture — cuy-monitor-backend

> Java 25 LTS · Spring Boot 4.1 · PostgreSQL 18 · Caddy 2.11 · Docker Compose on AWS EC2
> Last reviewed: 2026-09-26 (v2: Kafka removed, ingestion over HTTP — see ADR-007)

This repo is the core of the system. It receives events over HTTP, runs them through the six design patterns, stores everything in PostgreSQL and exposes a REST API and a WebSocket to the dashboard. It also owns the deployment (`infra/`) and the cross-repo contracts (`docs/contracts/`).

---

## 1. System context

```
        GUINEA PIG CAGE (farm)                                AWS — EC2 (Docker Compose)
┌─────────────────────────────────────┐          ┌───────────────────────────────────────────────┐
│  Samsung A12 (IP Webcam app)        │          │  Caddy (HTTPS :443, Let's Encrypt)            │
│   video + audio over WiFi           │          │    ├── /api/*, /ws*, /actuator/health* ─► backend
│            ▼                        │  HTTPS   │    └── /ai/*                           ─► ai-service
│  Celeron laptop                     │ +API key │                                               │
│   ├─ edge_agent ────────────────────┼─────────►│  ai-service ──POST /api/ingestion/events──┐   │
│   │   (repo: ai-service)  /ai/...   │          │             (internal: http://backend:8080) │ │
│   └─ serial_bridge ─────────────────┼─────────►│                                           ▼   │
│          ▲ (repo: arduino)          │ /api/ingestion/events          backend (Java :8080)    │
│          │ USB serial               │          │                         └──► PostgreSQL 18    │
│  Arduino Uno + HX711 + load cell    │          └───────────────────────────────────────────────┘
└─────────────────────────────────────┘                      ▲ REST + WebSocket (HTTPS)
                                                 ┌───────────┴───────────┐
                                                 │ AWS Amplify           │
                                                 │ cuy-monitor-dashboard │
                                                 └───────────────────────┘
```

| Repo | Role | Talks to the backend through |
|---|---|---|
| `cuy-monitor-ai-service` | Vision + audio ML (Python) | `POST /api/ingestion/events` over the internal Docker network |
| `cuy-monitor-arduino` | Weight sensor + serial bridge | `POST /api/ingestion/events` over HTTPS (through Caddy) |
| `cuy-monitor-dashboard` | Farmer UI (React + TS) | REST `/api/**` + STOMP over `/ws` |

All producers send the **same event envelope** to the **same endpoint**. The backend decides what to do by the event `type`.

---

## 2. Responsibilities

**In scope:**
- Receive events on `POST /api/ingestion/events` and normalize them into a single internal `HealthEvent`.
- Decide health: per guinea pig state machine + cage-level aggregation.
- Persist events, state transitions, alerts, weight readings and baselines.
- Notify: WebSocket push, database, application log.
- Expose the REST API and WebSocket for the dashboard.
- Own `infra/` (Compose, Caddy, env template) and `docs/contracts/`.

**Out of scope:** object detection, tracking, feature extraction, ML inference (all in `cuy-monitor-ai-service`).

---

## 3. Event pipeline and design patterns

```
POST /api/ingestion/events ──► IngestionController (API key + validation)
                                  │  IngestionEvent (envelope)
                                  ▼
                           AdapterFactory ──────────── FACTORY METHOD (picks the adapter by event type)
                                  │
                                  ▼
                         EventSourceAdapter ────────── ADAPTER (external payload → HealthEvent)
                                  │  HealthEvent  ◄─── boundary between the two owners
                                  ▼
   ValidationHandler → IdentificationHandler → BehaviorThresholdHandler → SustainedAnomalyHandler
                                  │                                 CHAIN OF RESPONSIBILITY
                                  ▼
                      GuineaPigHealthContext ───────── STATE (NORMAL ⇄ OBSERVED ⇄ ALERT ⇄ CRITICAL)
                                  │
                                  ▼
                             CageHealth ────────────── COMPOSITE (guinea pigs + audio + weight)
                                  │
                                  ▼
                           AlertPublisher ──────────── OBSERVER
                             ├── WebSocketAlertObserver  → /topic/cages/{id}
                             ├── DatabaseAlertObserver   → alert table
                             └── LogAlertObserver        → application log (audit)
```

Processing is synchronous inside the HTTP request. The load is tiny (a few events per minute per cage), so no queue is needed. The endpoint answers `202 Accepted` once the event went through the pipeline.

| Pattern | Package | Key types | Owner |
|---|---|---|---|
| Factory Method | `ingestion.factory` | `AdapterFactory` (abstract creator), `CameraAdapterFactory`, `AudioAdapterFactory`, `WeightAdapterFactory` | Teammate |
| Adapter | `ingestion.adapter` | `EventSourceAdapter` (target), `CameraBehaviorAdapter`, `AudioClassificationAdapter`, `WeightReadingAdapter` | Teammate |
| Chain of Responsibility | `health.chain` | `EventHandler` (abstract), 4 handlers, `HandlerChainBuilder` | Josuram |
| State | `health.state` | `HealthState` (interface), `NormalState`, `ObservedState`, `AlertState`, `CriticalState`, `GuineaPigHealthContext` | Josuram |
| Composite | `health.composite` | `HealthComponent`, `GuineaPigHealth` / `CageAudioHealth` / `CageWeightHealth` (leaves), `CageHealth` (composite) | Josuram |
| Observer | `notification` | `AlertObserver` (interface), `AlertPublisher` (subject), 3 observers | Teammate |

### Chain handlers

| Handler | Responsibility |
|---|---|
| `ValidationHandler` | Drop malformed, stale or low-confidence events |
| `IdentificationHandler` | Map `(cageId, MarkColor)` to a registered `GuineaPig` |
| `BehaviorThresholdHandler` | Compare the window against the guinea pig's `BaselineProfile` |
| `SustainedAnomalyHandler` | Only pass anomalies seen in N consecutive windows |

Audio and weight events skip `IdentificationHandler` (they are cage-level, `guineaPigId = null`).

### State transitions

```
NORMAL ──anomaly confirmed──► OBSERVED ──persists──► ALERT ──persists/worsens──► CRITICAL
   ▲                             │                     │                            │
   └──────── recovered ──────────┴──── recovered ──────┴──────── recovered ─────────┘
```

Every transition is stored in `state_transition` and, if it goes up to `ALERT` or `CRITICAL`, it produces an `Alert` through `AlertPublisher`. Exact thresholds and N are tuned in phase 2 with real data.

### Design rules

- Observer is implemented **by hand** (interface + list of observers), not with Spring's `ApplicationEventPublisher`, so the pattern is explicit.
- DTOs, payloads and `HealthEvent` are Java `record`s. Lombok only on JPA entities.
- `AdapterFactory` may use a `switch` with pattern matching on `EventType`.
- `HealthEvent` and `AlertObserver` are the contract between the two owners: change them only with both in agreement.

---

## 4. Package structure

```
src/main/java/com/cuymonitor/backend/
├── BackendApplication.java
├── config/            WebSocketConfig, CorsConfig
├── domain/
│   ├── model/         EventType ✅, Cage, GuineaPig, MarkColor, HealthStatus, HealthEvent, Alert, WeightReading, BaselineProfile
│   └── repository/    Spring Data JPA interfaces
├── ingestion/
│   ├── dto/           IngestionEvent ✅ (envelope) + payload records
│   ├── factory/       FACTORY METHOD
│   └── adapter/       ADAPTER
├── health/
│   ├── chain/         CHAIN OF RESPONSIBILITY
│   ├── state/         STATE
│   └── composite/     COMPOSITE
├── notification/      OBSERVER
└── api/               SystemController ✅, IngestionController ✅ (logs only for now), CageController, GuineaPigController, AlertController, dto/
```

✅ = exists today. Everything else is planned.

---

## 5. Contracts

Source of truth: `docs/contracts/` in this repo. Other repos copy from here.

### Ingestion endpoint

```http
POST /api/ingestion/events
X-API-Key: <API_KEY>
Content-Type: application/json
```

| Response | Meaning |
|---|---|
| `202 Accepted` | `{ "eventId": "...", "status": "ACCEPTED" }` |
| `400 Bad Request` | Envelope invalid (missing field, unknown `type`) — do not retry |
| `401 Unauthorized` | Wrong or missing API key — do not retry |
| `5xx` / network error | Retry with backoff |

Producers: `ai-service` (`BEHAVIOR`, `AUDIO`) calls `http://backend:8080` inside Docker; `serial_bridge` (`WEIGHT`) calls `https://cuymonitor.duckdns.org`.

### Event envelope

```json
{
  "eventId": "uuid",
  "type": "BEHAVIOR | AUDIO | WEIGHT",
  "cageId": "cage-1",
  "timestamp": "2026-10-05T14:32:00Z",
  "source": "ai-service | arduino",
  "schemaVersion": 1,
  "payload": {}
}
```

```jsonc
// BEHAVIOR — one per guinea pig per 60 s window
{ "color": "RED", "windowSeconds": 60, "stillSeconds": 48, "feederVisits": 0,
  "watererVisits": 1, "avgGroupDistance": 0.72, "probAnomaly": 0.81, "detectionConfidence": 0.93 }
// AUDIO
{ "label": "DISTRESS | NORMAL", "probability": 0.88, "durationMs": 960 }
// WEIGHT
{ "grams": 812.4, "stable": true }
```

`eventId` is generated by the producer, so a retried event can be detected as a duplicate.

### Shared enums

- `MarkColor`: `RED, BLUE, GREEN, YELLOW, ORANGE, PURPLE, BLACK, WHITE`
- `HealthStatus`: `NORMAL, OBSERVED, ALERT, CRITICAL`
- `EventType`: `BEHAVIOR, AUDIO, WEIGHT`
- `AlertStatus`: `OPEN, REVIEWED`

### REST API

| Method and path | Client | Status |
|---|---|---|
| `GET /actuator/health` | Caddy, everyone | ✅ |
| `GET /api/system/status` | smoke test (counts cages) | ✅ |
| `POST /api/ingestion/events` (`X-API-Key`) | ai-service, serial_bridge | ✅ (receives and logs; pipeline pending) |
| `GET /api/cages/{id}/health` | dashboard | planned |
| `GET /api/cages/{id}/guinea-pigs` | dashboard | planned |
| `POST /api/cages/{id}/guinea-pigs` | dashboard | planned |
| `GET /api/guinea-pigs/{id}/history?from=&to=` | dashboard | planned |
| `GET /api/alerts?status=OPEN` | dashboard | planned |
| `PATCH /api/alerts/{id}` | dashboard | planned |
| `GET /api/cages/{id}/weight?from=&to=` | dashboard | planned |
| `WS /ws` → STOMP `/topic/cages/{id}` | dashboard | planned |

Conventions: paths in `kebab-case` and plural, JSON in `camelCase`, timestamps in ISO-8601 UTC.

---

## 6. Data model (PostgreSQL 18, Flyway)

```
cage              (id, name, location, created_at)                                        ✅ V1
guinea_pig        (id, cage_id → cage, name, mark_color, current_status, active, created_at) ✅ V1
event             (id uuid, type, cage_id, guinea_pig_id NULL, source, occurred_at, payload JSONB)
state_transition  (id, guinea_pig_id, from_status, to_status, reason, occurred_at)
alert             (id, cage_id, guinea_pig_id NULL, level, type, message, status, created_at, reviewed_at)
weight_reading    (id, cage_id, grams, stable, measured_at)
baseline_profile  (guinea_pig_id, avg_still_seconds, avg_feeder_visits, avg_group_distance, updated_at)
```

- `event.id` is the producer's `eventId` (primary key → duplicates are rejected).
- `guinea_pig_id` is `NULL` for audio and weight (cage-level signals).
- `(cage_id, mark_color)` is unique: one color per guinea pig per cage.
- `spring.jpa.hibernate.ddl-auto=validate`: Flyway owns the schema, Hibernate only checks it.
- Migrations are append-only: `V2__...sql`, `V3__...sql`. Never edit an applied migration.

---

## 7. Deployment

### Runtime

| Container | Image | Exposed to the internet | Memory |
|---|---|---|---|
| caddy | `caddy:2.11` | 80, 443 (tcp + udp) | ~30 MB |
| backend | built from `Dockerfile` (`eclipse-temurin:25-jre`) | no (only via Caddy) | `-Xms256m -Xmx384m` |
| postgres | `postgres:18` (volume on `/var/lib/postgresql`) | no | ~150 MB |
| ai-service | built from `../../cuy-monitor-ai-service`, profile `ai` | no (only via Caddy `/ai/*`) | +0.6–1 GB |

- Host: EC2 **t3.small** (2 GB + swap). Without Kafka the base stack uses ~0.7 GB, so the ai-service may fit too; measure before deciding to move to **c7i-flex.large** (4 GB).
- Elastic IP + DuckDNS `cuymonitor.duckdns.org`. Caddy gets the certificate automatically.
- Security group: 80/443 open, 22 only for the team's IPs. Postgres is never published.
- All services use `restart: unless-stopped`.

### Configuration

`infra/.env` (never committed; template in `infra/.env.example`):

| Variable | Used by |
|---|---|
| `DOMAIN` | Caddy |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | postgres, and mapped to backend `DB_NAME`, `DB_USER`, `DB_PASSWORD` |
| `API_KEY` | mapped to backend `APP_API_KEY` and ai-service `API_KEY` |

Backend env vars (set in Compose): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `APP_API_KEY`, `JAVA_OPTS`. Defaults in `application.yml` point to `localhost` for local development.

### Commands

```bash
# on the EC2, from infra/
docker compose up -d --build --remove-orphans     # backend stack
docker compose --profile ai up -d --build         # + ai-service
docker compose logs -f backend
git pull && docker compose up -d --build          # update
```

Local development: `infra/docker-compose.dev.yml` starts only Postgres with its port on localhost; the backend runs from the IDE.

---

## 8. Testing strategy

| Level | What | Tool |
|---|---|---|
| Unit | Each state transition, each chain handler alone, composite aggregation, each adapter and factory | JUnit 5 |
| Web | `IngestionController`: 202 / 400 / 401 | `@WebMvcTest` |
| Integration | HTTP event → pipeline → Postgres end to end | Testcontainers (Postgres) |
| Smoke (deployed) | `/actuator/health`, `/api/system/status`, `POST /api/ingestion/events` | curl / Postman |

---

## 9. Architecture decisions

| ADR | Decision |
|---|---|
| ADR-001 | Multi-repo (4 repos); contracts live in this repo |
| ADR-002 | ML in a separate Python service; the backend only knows events |
| ADR-003 | ~~Kafka only inside Docker~~ → superseded by ADR-007 |
| ADR-004 | Single EC2 with Docker Compose (single point of failure accepted for a pilot) |
| ADR-005 | AI in the cloud first; may move to the laptop after measuring bandwidth and fps (October) |
| ADR-006 | New LTS versions, no betas, pinned Docker tags (never `:latest`) |
| ADR-007 | **No message broker.** All producers `POST` the event envelope to the backend |

### ADR-007: Remove Kafka, ingest over HTTP

**Status:** Accepted (2026-09-26)

**Context:** One pilot cage produces ~8 behavior events per minute, a few audio events and one weight reading every 10 s. Kafka was using ~400 MB of the 2 GB EC2 and added operational complexity (KRaft config, topics, consumer groups) for a load that a single HTTP endpoint handles easily.

| | HTTP ingestion (chosen) | Kafka |
|---|---|---|
| Complexity | Low: one endpoint | Broker, topics, producers, consumers |
| Memory | 0 extra | ~400 MB |
| Decoupling | Producer must retry if the backend is down | Broker buffers events |
| Enough for 1–few cages | Yes | Yes (overkill) |

**Consequences:**
- Producers must retry with backoff on network errors / 5xx and keep a small bounded buffer.
- `eventId` becomes the idempotency key.
- The Factory Method + Adapter design is unchanged: it now starts at `IngestionController` instead of a Kafka listener.
- If the system grows to many cages, a broker can be reintroduced behind the same envelope (Kafka, RabbitMQ or Amazon SQS) without touching the health core.

---

## 10. Spring Boot 4 notes

- Web starter is `spring-boot-starter-webmvc`.
- Flyway needs `spring-boot-starter-flyway` + `flyway-database-postgresql`.
- Jackson 3: package `tools.jackson`, not `com.fasterxml.jackson`.
- Test starters are per technology (`spring-boot-starter-webmvc-test`).

## 11. Future evolution

- Several cages: `cageId` is already in every event; one edge device per cage.
- Many cages: put a broker (SQS or Kafka) between producers and the backend, keeping the same envelope.
- Mobile notifications: add a `PushAlertObserver` without touching the rest.
- CI/CD: GitHub Actions → GHCR → `docker compose pull` on the EC2.
