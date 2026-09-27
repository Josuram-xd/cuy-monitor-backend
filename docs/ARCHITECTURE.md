# Architecture — cuy-monitor-backend

> Java 25 LTS · Spring Boot 4.1 · PostgreSQL 18 · Caddy 2.11 · Docker Compose on AWS EC2
> Last reviewed: 2026-09-27 · Internal style: pragmatic hexagonal (ADR-007)

This repo is the core of the system. It receives events over HTTP, runs them through the six design patterns inside a hexagonal core, stores everything in PostgreSQL and exposes a REST API and a WebSocket to the dashboard. It also owns the deployment (`infra/`) and the cross-repo contracts (`docs/contracts/`).

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

## 3. Internal architecture: pragmatic hexagonal (ports and adapters)

The backend is organized as a **hexagon**: a core with the business rules and the six design patterns, surrounded by adapters that connect it to the outside world (HTTP, PostgreSQL, WebSocket, logs). The core talks to the outside only through **ports** (Java interfaces). See ADR-007.

```
          DRIVING SIDE (input)                     CORE                               DRIVEN SIDE (output)
   ┌──────────────────────────────┐  ┌──────────────────────────────────────────┐  ┌──────────────────────────────┐
   │ adapter.in.web               │  │ domain.port.in   (use case interfaces)   │  │ adapter.out.persistence      │
   │  IngestionController         │─►│        ▲                                 │  │  JPA entities + Spring Data  │
   │  CageController …            │  │        │ implemented by                  │  │  + mappers ─────► PostgreSQL │
   │                              │  │ application  (use case services)         │  │        ▲                     │
   │ adapter.in.ingestion         │  │        │ orchestrates                    │  │        │ implements          │
   │  FACTORY METHOD + ADAPTER    │  │        ▼                                 │  │ domain.port.out              │
   │  envelope ─► HealthEvent     │  │ domain.health   CHAIN · STATE · COMPOSITE│─►│  repositories (interfaces)   │
   └──────────────────────────────┘  │ domain.notification  OBSERVER subject    │─►│  AlertObserver (interface)   │
                                     │ domain.model   entities, HealthEvent     │  │        ▲ implements          │
                                     └──────────────────────────────────────────┘  │ adapter.out.notification     │
                                                                                   │  WebSocket · Database · Log  │
                                                                                   └──────────────────────────────┘
                    Dependencies always point INTO the core. The core never imports an adapter.
```

### 3.1 Event flow through the hexagon

```
POST /api/ingestion/events
  └► IngestionController                       adapter.in.web        (API key, validation)
       └► AdapterFactory                        adapter.in.ingestion  FACTORY METHOD: picks the adapter by type
            └► EventSourceAdapter               adapter.in.ingestion  ADAPTER: envelope payload → HealthEvent
                 └► ProcessEventUseCase         domain.port.in        ◄── boundary between the two owners
                      └► EventProcessingService application
                           ├► EventHandler chain                  domain.health.chain      CHAIN OF RESPONSIBILITY
                           │    (uses GuineaPigRepository, BaselineProfileRepository, EventRepository — ports out)
                           ├► GuineaPigHealthContext              domain.health.state      STATE
                           │    (saves via StateTransitionRepository — port out)
                           ├► CageHealth                          domain.health.composite  COMPOSITE
                           └► AlertPublisher                      domain.notification      OBSERVER (subject)
                                └► AlertObserver (port out)
                                     ├── WebSocketAlertObserver   adapter.out.notification → /topic/cages/{id}
                                     ├── DatabaseAlertObserver    adapter.out.notification → alert table
                                     └── LogAlertObserver         adapter.out.notification → application log
```

Read endpoints follow the same shape: `CageController` → `GetCageHealthUseCase` → `CageQueryService` → repository ports → persistence adapter.

Processing is synchronous inside the HTTP request. The load is tiny (a few events per minute per cage), so no queue is needed. The endpoint answers `202 Accepted` once the event went through the pipeline.

### 3.2 Where each design pattern lives

| Pattern | Package | Key types | Hexagonal role | Owner |
|---|---|---|---|---|
| Factory Method | `adapter.in.ingestion.factory` | `AdapterFactory` (abstract creator), `CameraAdapterFactory`, `AudioAdapterFactory`, `WeightAdapterFactory` | Input adapter | Teammate |
| Adapter | `adapter.in.ingestion.adapter` | `EventSourceAdapter` (target), `CameraBehaviorAdapter`, `AudioClassificationAdapter`, `WeightReadingAdapter` | Input adapter (translates the external format into `HealthEvent`) | Teammate |
| Chain of Responsibility | `domain.health.chain` | `EventHandler` (abstract), 4 handlers, `HandlerChainBuilder` | Core | Josuram |
| State | `domain.health.state` | `HealthState` (interface), `NormalState`, `ObservedState`, `AlertState`, `CriticalState`, `GuineaPigHealthContext` | Core | Josuram |
| Composite | `domain.health.composite` | `HealthComponent`, `GuineaPigHealth` / `CageAudioHealth` / `CageWeightHealth` (leaves), `CageHealth` (composite) | Core | Josuram |
| Observer | `domain.notification` + `domain.port.out` + `adapter.out.notification` | `AlertPublisher` (subject, core), `AlertObserver` (output port), 3 concrete observers (output adapters) | Core + output port + output adapters | Teammate |

The Adapter pattern and the Observer are also the clearest examples of the hexagonal idea: the first converts the outside format into the core's language, the second lets the core notify the outside world through a port without knowing who listens.

### 3.3 Ports

| Port | Kind | Implemented by |
|---|---|---|
| `ProcessEventUseCase` | in | `EventProcessingService` |
| `GetCageHealthUseCase` | in | `CageQueryService` |
| `ListGuineaPigsUseCase`, `RegisterGuineaPigUseCase`, `GetGuineaPigHistoryUseCase` | in | `GuineaPigService` |
| `ListAlertsUseCase`, `ReviewAlertUseCase` | in | `AlertService` |
| `GetWeightHistoryUseCase` | in | `CageQueryService` |
| `CageRepository`, `GuineaPigRepository`, `EventRepository`, `StateTransitionRepository`, `AlertRepository`, `WeightReadingRepository`, `BaselineProfileRepository` | out | `*PersistenceAdapter` classes in `adapter.out.persistence` |
| `AlertObserver` | out | `WebSocketAlertObserver`, `DatabaseAlertObserver`, `LogAlertObserver` |

Port names describe the business need, not the technology (`GuineaPigRepository`, not `GuineaPigJpaRepository`).

### 3.4 Dependency rules

| Package | May depend on | Must NOT depend on |
|---|---|---|
| `domain..` | `java.*` only | Spring, JPA (`jakarta.persistence`), Jackson, `application`, `adapter`, `config` |
| `application..` | `domain..` | `adapter..` |
| `adapter.in..` | `domain.port.in`, `domain.model` | `adapter.out..`, persistence classes |
| `adapter.out..` | `domain.port.out`, `domain.model` | `adapter.in..`, `application..` |
| `config..` | everything (it wires the beans) | — |

These rules are checked by an ArchUnit test (`src/test/java/.../architecture/HexagonalArchitectureTest.java`).

### 3.5 What makes it "pragmatic"

| Decision | Why |
|---|---|
| Spring annotations allowed in `application` (`@Service`, `@Transactional`) and in adapters; **never** in `domain` | Keeps the core pure without writing manual wiring for everything |
| Domain objects without annotations (chain, states, publisher) are created as beans in `config/DomainConfig` | The core stays framework-free and Spring still injects everything |
| JPA entities are separate classes (`GuineaPigJpaEntity`) with a small mapper to the domain model | Only 7 tables; the domain model doesn't depend on Hibernate |
| No command/query objects unless needed: use cases receive domain types or small records | Less boilerplate for a 2-person team |
| `SystemController` uses `JdbcTemplate` directly | Temporary smoke-test endpoint; removed in Task 17.1 |

### 3.6 Chain handlers

| Handler | Responsibility | Ports used |
|---|---|---|
| `ValidationHandler` | Drop malformed, stale or low-confidence events | — |
| `IdentificationHandler` | Map `(cageId, MarkColor)` to a registered `GuineaPig` | `GuineaPigRepository` |
| `BehaviorThresholdHandler` | Compare the window against the guinea pig's `BaselineProfile` | `BaselineProfileRepository` |
| `SustainedAnomalyHandler` | Only pass anomalies seen in N consecutive windows | `EventRepository` |

Audio and weight events skip `IdentificationHandler` (they are cage-level, `guineaPigId = null`).

### 3.7 State transitions

```
NORMAL ──anomaly confirmed──► OBSERVED ──persists──► ALERT ──persists/worsens──► CRITICAL
   ▲                             │                     │                            │
   └──────── recovered ──────────┴──── recovered ──────┴──────── recovered ─────────┘
```

Every transition is stored through `StateTransitionRepository` and, if it goes up to `ALERT` or `CRITICAL`, it produces an `Alert` through `AlertPublisher`. Exact thresholds and N are tuned in phase 2 with real data.

### 3.8 Design rules

- Observer is implemented **by hand** (`AlertObserver` port + list in `AlertPublisher`), not with Spring's `ApplicationEventPublisher`, so the pattern is explicit.
- DTOs, payloads and `HealthEvent` are Java `record`s. Lombok only on JPA entities.
- `AdapterFactory` may use a `switch` with pattern matching on `EventType`.
- Controllers only call input ports. They never touch repositories or JPA.
- `HealthEvent`, `ProcessEventUseCase` and `AlertObserver` are the contract between the two owners: change them only with both in agreement.

---

## 4. Package structure

```
src/main/java/com/cuymonitor/backend/
├── BackendApplication.java
├── domain/                               ← pure Java: no Spring, no JPA, no Jackson
│   ├── model/                            EventType ✅, MarkColor, HealthStatus, AlertStatus, Cage, GuineaPig,
│   │                                     HealthEvent, Alert, WeightReading, BaselineProfile, StateTransition
│   ├── health/
│   │   ├── chain/                        CHAIN OF RESPONSIBILITY
│   │   ├── state/                        STATE
│   │   └── composite/                    COMPOSITE
│   ├── notification/                     AlertPublisher (OBSERVER subject)
│   └── port/
│       ├── in/                           ProcessEventUseCase, GetCageHealthUseCase, ListGuineaPigsUseCase,
│       │                                 RegisterGuineaPigUseCase, GetGuineaPigHistoryUseCase,
│       │                                 ListAlertsUseCase, ReviewAlertUseCase, GetWeightHistoryUseCase
│       └── out/                          CageRepository, GuineaPigRepository, EventRepository,
│                                         StateTransitionRepository, AlertRepository,
│                                         WeightReadingRepository, BaselineProfileRepository, AlertObserver
├── application/                          EventProcessingService, CageQueryService, GuineaPigService, AlertService
├── adapter/
│   ├── in/
│   │   ├── web/                          IngestionController ✅, SystemController ✅ (temporary),
│   │   │   │                             CageController, GuineaPigController, AlertController
│   │   │   └── dto/                      request/response records for the REST API
│   │   └── ingestion/
│   │       ├── dto/                      IngestionEvent ✅ (envelope) + payload records
│   │       ├── factory/                  FACTORY METHOD
│   │       └── adapter/                  ADAPTER
│   └── out/
│       ├── persistence/
│       │   ├── entity/                   *JpaEntity classes
│       │   ├── repository/               Spring Data interfaces
│       │   ├── mapper/                   JPA entity ↔ domain model
│       │   └── *PersistenceAdapter.java  implement domain.port.out repositories
│       └── notification/                 WebSocketAlertObserver, DatabaseAlertObserver, LogAlertObserver
└── config/                               DomainConfig, WebSocketConfig, CorsConfig
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

- Host: EC2 **t3.small** (2 GB + swap). The base stack uses ~0.7 GB, so the ai-service may fit too; measure before deciding to move to **c7i-flex.large** (4 GB).
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
| Domain unit | Each state transition, each chain handler alone, composite aggregation, `AlertPublisher` | Plain JUnit 5 — **no Spring context, no database** |
| Application | Use case services with in-memory fakes of the output ports | JUnit 5 |
| Input adapters | Each factory and adapter (envelope → `HealthEvent`); controllers 202 / 400 / 401 | JUnit 5, `@WebMvcTest` |
| Output adapters | Persistence adapters and mappers against a real Postgres | `@DataJpaTest` + Testcontainers |
| Architecture | Dependency rules of section 3.4 | ArchUnit |
| Integration | HTTP event → pipeline → Postgres end to end | `@SpringBootTest` + Testcontainers |
| Smoke (deployed) | `/actuator/health`, `/api/system/status`, `POST /api/ingestion/events` | curl / Postman |

---

## 9. Architecture decisions

| ADR | Decision |
|---|---|
| ADR-001 | Multi-repo (4 repos); contracts live in this repo |
| ADR-002 | ML in a separate Python service; the backend only knows events |
| ADR-003 | Direct HTTP ingestion: every producer `POST`s the event envelope to the backend; only Caddy is public |
| ADR-004 | Single EC2 with Docker Compose (single point of failure accepted for a pilot) |
| ADR-005 | AI in the cloud first; may move to the laptop after measuring bandwidth and fps (October) |
| ADR-006 | New LTS versions, no betas, pinned Docker tags (never `:latest`) |
| ADR-007 | Pragmatic hexagonal architecture (ports and adapters) inside the backend |

### ADR-003: Direct HTTP ingestion

**Status:** Accepted

**Context:** One pilot cage produces ~8 behavior events per minute, a few audio events and one weight reading every 10 s. The EC2 has 2 GB of RAM.

| | Direct HTTP (chosen) | Message queue in the middle |
|---|---|---|
| Complexity | Low: one endpoint | Extra service, configuration, consumers |
| Memory | 0 extra | Hundreds of MB |
| If the backend is down | The producer retries | The queue buffers events |
| Enough for 1–few cages | Yes | Yes (overkill) |

**Consequences:**
- Producers retry with backoff on network errors / 5xx and keep a small bounded buffer.
- `eventId` is the idempotency key, so retries don't create duplicates.
- The ai-service calls the backend inside the Docker network; the laptop (serial bridge) calls it over HTTPS through Caddy with the API key. Postgres and the backend port are never exposed.
- If the system grows to many cages, a queue (e.g. Amazon SQS) can be put in front of the backend with the same envelope, without touching the health core.

### ADR-007: Pragmatic hexagonal architecture inside the backend

**Status:** Accepted (2026-09-27)

**Context:** The backend hosts the six design patterns of the course. The health rules (Chain, State, Composite) must be easy to test and to explain, and two people work on different halves (data input vs. health core). Almost no code existed yet, so changing the structure was cheap.

| | Layered (previous) | Strict hexagonal | Pragmatic hexagonal (chosen) |
|---|---|---|---|
| Core testable without Spring/DB | Partly | Yes | Yes |
| Boilerplate | Low | High (commands, manual wiring, no framework anywhere near the core) | Medium (ports + JPA mappers) |
| Fit with the course patterns | Neutral | Good | Good: Adapter = input adapter, Observer = output port + adapters |
| Team split | By layer | By port | By port: input side (teammate) / core (Josuram) |

**Consequences:**
- `domain` is pure Java; an ArchUnit test fails the build if it imports Spring, JPA or an adapter.
- ~15 extra interfaces (ports) and one mapper per table.
- Controllers depend on use case interfaces, so they can be tested with fakes.
- Replacing a technology (e.g. Postgres, WebSocket, adding a push-notification observer) means writing a new adapter, not touching the core.

---

## 10. Spring Boot 4 notes

- Web starter is `spring-boot-starter-webmvc`.
- Flyway needs `spring-boot-starter-flyway` + `flyway-database-postgresql`.
- Jackson 3: package `tools.jackson`, not `com.fasterxml.jackson`.
- Test starters are per technology (`spring-boot-starter-webmvc-test`).

## 11. Future evolution

- Several cages: `cageId` is already in every event; one edge device per cage.
- Many cages: put a queue (e.g. Amazon SQS) between producers and the backend, keeping the same envelope.
- Mobile notifications: add a `PushAlertObserver` without touching the rest.
- CI/CD: GitHub Actions → GHCR → `docker compose pull` on the EC2.
