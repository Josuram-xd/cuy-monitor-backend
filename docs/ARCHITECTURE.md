# Architecture — cuy-monitor-backend

> Java 25 LTS · Spring Boot 4.1 · Spring Security (JWT resource server) · Amazon RDS for PostgreSQL 18 · Caddy 2.11 · Docker Compose on AWS EC2
> Last reviewed: 2026-10-03 · Internal style: pragmatic hexagonal (ADR-007)

This repo is the core of the system. It receives events over HTTP, runs them through the six design patterns inside a hexagonal core, stores everything in PostgreSQL and exposes a REST API and a WebSocket to the dashboard. It also manages **user accounts** (registration, login with an email OTP, JWT), owns the deployment (`infra/`) and the cross-repo contracts (`docs/contracts/`).

The database **schema** lives in its own repo, `cuy-monitor-db` (ADR-008). This backend only validates it.

---

## 1. System context

```
        GUINEA PIG CAGE (farm)                         AWS — one region, one VPC
┌─────────────────────────────────────┐     ┌──────────────────────────────────────────────────────────┐
│  Samsung A12 (IP Webcam app)        │     │  EC2 (Docker Compose)                                    │
│   video + audio over WiFi           │     │  ┌────────────────────────────────────────────────────┐  │
│            ▼                        │HTTPS│  │ Caddy (HTTPS :443, Let's Encrypt)                  │  │
│  Celeron laptop                     │+API │  │  ├── /api/*, /ws*, /actuator/health* ─► backend    │  │
│   ├─ edge_agent ────────────────────┼─key─┼─►│  ├── /ai/*                           ─► ai-service │  │
│   │   (repo: ai-service)  /ai/...   │     │  │  └── /*  (SPA)                       ─► dashboard  │  │
│   └─ serial_bridge ─────────────────┼────►│  └────────────────────────────────────────────────────┘  │
│          ▲ (repo: arduino)          │     │   ai-service ──POST /api/v1/ingestion/events──► backend   │
│          │ USB serial               │     │                 (internal http://backend:8080)  │        │
│  Arduino Uno + HX711 + load cell    │     │   migrate (one-shot, repo: cuy-monitor-db) ──┐  │ JDBC   │
└─────────────────────────────────────┘     │                                              ▼  ▼ TLS    │
                                            │  RDS for PostgreSQL 18 (private subnet, no public IP)    │
       Farmer's phone / PC (browser)        └──────────────────────────────────────────────────────────┘
        └── HTTPS: dashboard + REST (Bearer JWT) + WSS /ws (JWT on STOMP CONNECT) ──► Caddy
```

| Repo | Role | Talks to the backend through |
|---|---|---|
| `cuy-monitor-db` | PostgreSQL schema (Flyway migrations), local Postgres, dev seeds | Nothing at runtime: its `migrate` container prepares the schema the backend validates |
| `cuy-monitor-ai-service` | Vision + audio ML (Python) | `POST /api/v1/ingestion/events` over the internal Docker network (`X-API-Key`) |
| `cuy-monitor-arduino` | Weight sensor + serial bridge | `POST /api/v1/ingestion/events` over HTTPS through Caddy (`X-API-Key`) |
| `cuy-monitor-dashboard` | Farmer UI (React + TS), behind login | `/api/v1/auth/**` (public), REST `/api/v1/**` + STOMP `/ws` with a JWT, same origin |

All producers send the **same event envelope** to the **same endpoint**. The backend decides what to do by the event `type`. Producers use the API key; people use a JWT.

---

## 2. Responsibilities

**In scope:**
- Receive events on `POST /api/v1/ingestion/events` and normalize them into a single internal `HealthEvent`.
- Decide health: per guinea pig state machine + cage-level aggregation.
- Persist events, state transitions, alerts, weight readings, baselines, users and OTP challenges (through JPA, on the schema owned by `cuy-monitor-db`).
- Notify: WebSocket push, database, application log.
- User accounts: register, verify email with an OTP, login with password + OTP, issue and validate JWTs, account CRUD on `/api/v1/account`.
- Expose the REST API and WebSocket for the dashboard, protected by JWT.
- Own `infra/` (Compose, Caddy, env template) and `docs/contracts/`.

**Out of scope:**
- Object detection, tracking, feature extraction, ML inference (all in `cuy-monitor-ai-service`).
- Creating or changing tables (all in `cuy-monitor-db`).
- Roles, admin users, password recovery, general rate limiting (only the OTP request limit exists).

---

## 3. Internal architecture: pragmatic hexagonal (ports and adapters)

The backend is organized as a **hexagon**: a core with the business rules and the six design patterns, surrounded by adapters that connect it to the outside world (HTTP, PostgreSQL, WebSocket, logs, mail). The core talks to the outside only through **ports** (Java interfaces). See ADR-007.

```
          DRIVING SIDE (input)                     CORE                               DRIVEN SIDE (output)
   ┌──────────────────────────────┐  ┌──────────────────────────────────────────┐  ┌──────────────────────────────┐
   │ adapter.in.web               │  │ domain.port.in   (use case interfaces)   │  │ adapter.out.persistence      │
   │  IngestionController         │─►│        ▲                                 │  │  JPA entities + Spring Data  │
   │  CageController …            │  │        │ implemented by                  │  │  + mappers ──────► RDS       │
   │  AuthController              │  │ application  (use case services)         │  │ adapter.out.security         │
   │  UserAccountController       │  │        │ orchestrates                    │  │  BCrypt, JWT issuer          │
   │                              │  │        ▼                                 │  │ adapter.out.mail             │
   │ adapter.in.ingestion         │  │ domain.health   CHAIN · STATE · COMPOSITE│─►│  Email / Log OTP sender      │
   │  FACTORY METHOD + ADAPTER    │  │ domain.notification  OBSERVER subject    │─►│        ▲ implements          │
   │  envelope ─► HealthEvent     │  │ domain.model   entities, HealthEvent,    │  │ domain.port.out              │
   └──────────────────────────────┘  │                User, OtpChallenge        │  │  repositories, AlertObserver,│
                                     └──────────────────────────────────────────┘  │  PasswordHasher, TokenIssuer,│
                                                                                   │  OtpSender                   │
                                                                                   │ adapter.out.notification     │
                                                                                   │  WebSocket · Database · Log  │
                                                                                   └──────────────────────────────┘
                    Dependencies always point INTO the core. The core never imports an adapter.
```

### 3.1 Event flow through the hexagon

```
POST /api/v1/ingestion/events
  └► IngestionController                       adapter.in.web        (API key, validation)
       └► AdapterFactory                        adapter.in.ingestion  FACTORY METHOD: each concrete factory creates its adapter
            └► EventSourceAdapter               adapter.in.ingestion  ADAPTER: envelope payload → HealthEvent
                 └► ProcessEventUseCase         domain.port.in        ◄── boundary between data input and the health core
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

Read endpoints follow the same shape: `CageController` → `GetCageHealthUseCase` → `CageQueryService` → repository ports → persistence adapter. Every read endpoint requires a valid JWT (section 4).

Processing is synchronous inside the HTTP request. The load is tiny (a few events per minute per cage), so no queue is needed. The endpoint answers `202 Accepted` once the event went through the pipeline.

### 3.2 Where each design pattern lives

| Pattern | Package | Key types | Hexagonal role |
|---|---|---|---|
| Factory Method | `adapter.in.ingestion.factory` | `AdapterFactory` (abstract creator), `CameraAdapterFactory`, `AudioAdapterFactory`, `WeightAdapterFactory` | Input adapter |
| Adapter | `adapter.in.ingestion.adapter` | `EventSourceAdapter` (target), `CameraBehaviorAdapter`, `AudioClassificationAdapter`, `WeightReadingAdapter` | Input adapter (translates the external format into `HealthEvent`) |
| Chain of Responsibility | `domain.health.chain` | `EventHandler` (abstract), 4 handlers, `HandlerChainBuilder` | Core |
| State | `domain.health.state` | `HealthState` (interface), `NormalState`, `ObservedState`, `AlertState`, `CriticalState`, `GuineaPigHealthContext` | Core |
| Composite | `domain.health.composite` | `HealthComponent`, `GuineaPigHealth` / `CageAudioHealth` / `CageWeightHealth` (leaves), `CageHealth` (composite) | Core |
| Observer | `domain.notification` + `domain.port.out` + `adapter.out.notification` | `AlertPublisher` (subject, core), `AlertObserver` (output port), 3 concrete observers (output adapters) | Core + output port + output adapters |

The Adapter pattern and the Observer are also the clearest examples of the hexagonal idea: the first converts the outside format into the core's language, the second lets the core notify the outside world through a port without knowing who listens.

The user/auth part adds **no new GoF pattern** for the course. `OtpSender` behaves like a Strategy (email in prod, log in `dev`, chosen by profile) and `UserStatus` is a plain enum (3 states with simple rules don't justify a State).

### 3.3 Ports

| Port | Kind | Implemented by |
|---|---|---|
| `ProcessEventUseCase` | in | `EventProcessingService` |
| `GetCageHealthUseCase` | in | `CageQueryService` |
| `ListGuineaPigsUseCase`, `RegisterGuineaPigUseCase`, `GetGuineaPigHistoryUseCase` | in | `GuineaPigService` |
| `ListAlertsUseCase`, `ReviewAlertUseCase` | in | `AlertService` |
| `GetWeightHistoryUseCase` | in | `CageQueryService` |
| `RegisterUserUseCase`, `LoginUseCase`, `VerifyOtpUseCase` | in | `AuthenticationService` |
| `GetCurrentUserUseCase`, `UpdateProfileUseCase`, `ChangePasswordUseCase`, `DeactivateAccountUseCase` | in | `UserAccountService` |
| `CageRepository`, `GuineaPigRepository`, `EventRepository`, `StateTransitionRepository`, `AlertRepository`, `WeightReadingRepository`, `BaselineProfileRepository` | out | `*PersistenceAdapter` classes in `adapter.out.persistence` |
| `UserRepository`, `OtpChallengeRepository` | out | `UserPersistenceAdapter`, `OtpChallengePersistenceAdapter` |
| `PasswordHasher`, `TokenIssuer` | out | `BCryptPasswordHasher`, `JwtTokenIssuer` (`adapter.out.security`) |
| `OtpSender` | out | `EmailOtpSender`, `LogOtpSender` (`adapter.out.mail`) |
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
| JPA entities are separate classes (`GuineaPigJpaEntity`) with a small mapper to the domain model | Few tables; the domain model doesn't depend on Hibernate |
| Small command records in `domain.port.in` (`LoginCommand`, `RegisterUserCommand`, …) only where a use case needs several fields | Less boilerplate for a 2-person team |
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
- Factory Method is a real Factory Method, not a Simple Factory: `AdapterFactory` is an abstract creator with an abstract `createAdapter()` method, and each concrete factory (`CameraAdapterFactory`, `AudioAdapterFactory`, `WeightAdapterFactory`) overrides it to create its own adapter. The only `switch` on `EventType` allowed is the one that picks **which factory** to use; it never creates adapters directly.
- Patterns are written **by hand** and stay visible in the code. Spring only wires the objects (constructor injection, beans in `config/DomainConfig`); it never implements a pattern. Not allowed: `@EventListener` / `ApplicationEventPublisher` for Observer, `@Order` or injected lists to order the Chain (`HandlerChainBuilder` links each handler with `setNext`), Spring State Machine for State.
- Controllers only call input ports. They never touch repositories or JPA.
- `HealthEvent`, `ProcessEventUseCase` and `AlertObserver` are the contract between the data-input side and the health core: change them only with the whole team in agreement.

---

## 4. Users and authentication

One kind of user (the person who looks at the dashboard). No roles, no admin. Anyone can sign up from the login screen; each user only sees and changes **their own account**. All users see the same pilot cage.

### 4.1 Flows

```
REGISTER   POST /api/v1/auth/register {username, fullName, email, password}
             → user PENDING_VERIFICATION, 6-digit code emailed      → 201 {challengeId, expiresAt}
LOGIN      POST /api/v1/auth/login {username, password}
             → password OK (and not DISABLED), new code emailed     → 200 {challengeId, expiresAt}
VERIFY     POST /api/v1/auth/otp/verify {challengeId, code}
             → code OK; PENDING_VERIFICATION becomes ACTIVE         → 200 {accessToken, tokenType: "Bearer", expiresAt}
USE        any /api/v1/** with  Authorization: Bearer <jwt>
           STOMP CONNECT on /ws with header  Authorization: Bearer <jwt>
LOGOUT     the dashboard deletes the token (stateless API; the token expires by itself after 30 min)
```

### 4.2 Access rules (`config/SecurityConfig`)

| Path | Access |
|---|---|
| `/api/v1/auth/**`, `/actuator/health` | Public |
| `/api/v1/ingestion/**` | `X-API-Key` filter (no JWT) |
| `/ws` (HTTP handshake) | Open; the JWT is checked on the STOMP `CONNECT` frame by a `ChannelInterceptor` (browsers can't set headers on the WebSocket handshake) |
| everything else under `/api/v1/**` | `Authorization: Bearer <jwt>` (OAuth2 resource server, HS256) |

Session `STATELESS`, CSRF disabled (Bearer API), CORS only for `http://localhost:5173` in the `dev` profile (in prod the dashboard is served from the same domain).

### 4.3 Security rules

- Password hashed with **BCrypt**; policy 8–72 characters. OTP code also stored **hashed**.
- OTP: single use, expires in 5 min, max 5 attempts; requesting a new code revokes the previous ones of that user.
- JWT HS256, secret ≥ 32 bytes from `APP_JWT_SECRET`, 30 min. Claims `sub` (user id), `iat`, `exp`, `iss`. No refresh token, no revocation list.
- Generic `401 invalid credentials` on login (hash is computed even when the user doesn't exist). `DISABLED` accounts get the same answer.
- `/api/v1/account` loads the user on every request and rejects `DISABLED` accounts even if the JWT is still valid.
- Mail: `EmailOtpSender` with `JavaMailSender` over SMTP (Amazon SES SMTP or Gmail with an app password). Profile `dev` uses `LogOtpSender` (writes the code to the log).
- Out of scope: general rate limiting (OTP requests are capped at 5 per 15 min), password recovery, email change.

### 4.4 SOLID in the auth part

- **S**: `AuthenticationService` (register, login, OTP) and `UserAccountService` (account CRUD) are separate; `OtpChallenge` owns its own rules (expired, attempts, used).
- **O**: changing BCrypt for Argon2 or email for SMS is a new adapter of the port, services untouched.
- **L**: `EmailOtpSender` and `LogOtpSender` are interchangeable behind `OtpSender` (and so are the in-memory test fakes).
- **I**: one input port per use case; each controller depends only on what it uses.
- **D**: services depend on ports (`UserRepository`, `PasswordHasher`, `TokenIssuer`, `OtpSender`), never on JPA, Spring Security or JavaMail.

---

## 5. Package structure

```
src/main/java/com/cuymonitor/backend/
├── BackendApplication.java
├── domain/                               ← pure Java: no Spring, no JPA, no Jackson
│   ├── model/                            EventType ✅, MarkColor, HealthStatus, AlertStatus, Cage, GuineaPig,
│   │   │                                 HealthEvent, Alert, WeightReading, BaselineProfile, StateTransition
│   │   ├── user/                         User ✅, UserStatus ✅, PasswordPolicy ✅
│   │   └── auth/                         OtpChallenge ✅, LoginChallenge ✅, AuthToken ✅, OtpVerificationResult ✅
│   ├── exception/                        InvalidCredentials ✅, InvalidOtp ✅, UserAlreadyExists ✅, WeakPassword ✅
│   ├── health/
│   │   ├── chain/                        CHAIN OF RESPONSIBILITY
│   │   ├── state/                        STATE
│   │   └── composite/                    COMPOSITE
│   ├── notification/                     AlertPublisher (OBSERVER subject)
│   └── port/
│       ├── in/                           ProcessEventUseCase, GetCageHealthUseCase, ListGuineaPigsUseCase,
│       │                                 RegisterGuineaPigUseCase, GetGuineaPigHistoryUseCase,
│       │                                 ListAlertsUseCase, ReviewAlertUseCase, GetWeightHistoryUseCase,
│       │                                 RegisterUserUseCase ✅, LoginUseCase ✅, VerifyOtpUseCase ✅ (+ commands ✅),
│       │                                 GetCurrentUserUseCase, UpdateProfileUseCase, ChangePasswordUseCase,
│       │                                 DeactivateAccountUseCase
│       └── out/                          CageRepository, GuineaPigRepository, EventRepository,
│                                         StateTransitionRepository, AlertRepository,
│                                         WeightReadingRepository, BaselineProfileRepository, AlertObserver,
│                                         UserRepository ✅, OtpChallengeRepository ✅, PasswordHasher ✅,
│                                         OtpSender ✅, TokenIssuer ✅
├── application/                          EventProcessingService, CageQueryService, GuineaPigService, AlertService,
│                                         AuthenticationService, UserAccountService
├── adapter/
│   ├── in/
│   │   ├── web/                          IngestionController ✅, SystemController ✅ (temporary),
│   │   │   │                             CageController, GuineaPigController, AlertController,
│   │   │   │                             AuthController, UserAccountController, AuthExceptionHandler
│   │   │   └── dto/                      request/response records for the REST API
│   │   └── ingestion/
│   │       ├── dto/                      IngestionEvent ✅ (envelope) + payload records
│   │       ├── factory/                  FACTORY METHOD
│   │       └── adapter/                  ADAPTER
│   └── out/
│       ├── persistence/
│       │   ├── entity/                   *JpaEntity classes (UserJpaEntity ✅, OtpChallengeJpaEntity ✅)
│       │   ├── repository/               Spring Data interfaces (User ✅, OtpChallenge ✅)
│       │   ├── mapper/                   JPA entity ↔ domain model (User ✅, OtpChallenge ✅)
│       │   └── *PersistenceAdapter.java  implement domain.port.out repositories (User ✅, OtpChallenge ✅)
│       ├── security/                     BCryptPasswordHasher, JwtTokenIssuer
│       ├── mail/                         EmailOtpSender, LogOtpSender (profile dev)
│       └── notification/                 WebSocketAlertObserver, DatabaseAlertObserver, LogAlertObserver
└── config/                               DomainConfig, WebSocketConfig (+ JWT ChannelInterceptor), CorsConfig (dev),
                                          SecurityConfig, JwtConfig, AuthProperties, AuthConfig
```

✅ = exists today. Everything else is planned.

`src/main/resources/db/migration/` (V1, V2) is **moved to `cuy-monitor-db`** in Task 21 and then deleted here.

---

## 6. Contracts

Source of truth: `docs/contracts/` in this repo. Other repos copy from here.

### Ingestion endpoint

```http
POST /api/v1/ingestion/events
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

`cageId` is the cage's public **code** (`"cage-1"`), not its numeric primary key (see `cuy-monitor-db` Task 3).

### Shared enums

- `MarkColor`: `RED, BLUE, GREEN, YELLOW, ORANGE, PURPLE, BLACK, WHITE`
- `HealthStatus`: `NORMAL, OBSERVED, ALERT, CRITICAL`
- `EventType`: `BEHAVIOR, AUDIO, WEIGHT`
- `AlertStatus`: `OPEN, REVIEWED`
- `UserStatus`: `PENDING_VERIFICATION, ACTIVE, DISABLED`

### Auth API (`docs/contracts/auth-api.md`)

| Method and path | Body | Response |
|---|---|---|
| `POST /api/v1/auth/register` | `{ username, fullName, email, password }` | `201 { challengeId, expiresAt }` · `400` invalid / weak password · `409` username or email taken |
| `POST /api/v1/auth/login` | `{ username, password }` | `200 { challengeId, expiresAt }` · `401` invalid credentials |
| `POST /api/v1/auth/otp/verify` | `{ challengeId, code }` | `200 { accessToken, tokenType: "Bearer", expiresAt }` · `401` invalid / expired / used code |
| `GET /api/v1/account/profile` | — | `200 { id, username, fullName, email, status, createdAt }` |
| `PUT /api/v1/account/profile` | `{ fullName }` | `200` user |
| `PUT /api/v1/account/password` | `{ currentPassword, newPassword }` | `204` · `401` wrong current password |
| `DELETE /api/v1/account` | `{ currentPassword }` | `204` (account `DISABLED`) · `401` wrong current password |

### REST API

| Method and path | Auth | Client | Status |
|---|---|---|---|
| `GET /actuator/health` | public | Caddy, everyone | ✅ |
| `GET /api/v1/system/status` | JWT (once security is on) | smoke test (counts cages) | ✅ (removed in Task 17.1) |
| `POST /api/v1/ingestion/events` | `X-API-Key` | ai-service, serial_bridge | ✅ (receives and logs; pipeline pending) |
| `/api/v1/auth/**` | public | dashboard | planned (Task 18) |
| `/api/v1/account/**` | JWT | dashboard | planned (Task 19) |
| `GET /api/v1/cages/{id}/health` | JWT | dashboard | planned |
| `GET /api/v1/cages/{id}/guinea-pigs` | JWT | dashboard | planned |
| `POST /api/v1/cages/{id}/guinea-pigs` | JWT | dashboard | planned |
| `GET /api/v1/guinea-pigs/{id}/history?from=&to=` | JWT | dashboard | planned |
| `GET /api/v1/alerts?status=OPEN` | JWT | dashboard | planned |
| `PATCH /api/v1/alerts/{id}` | JWT | dashboard | planned |
| `GET /api/v1/cages/{id}/weight?from=&to=` | JWT | dashboard | planned |
| `WS /ws` → STOMP `/topic/cages/{id}` | JWT on `CONNECT` | dashboard | planned |

Conventions: paths in `kebab-case` and plural, JSON in `camelCase`, timestamps in ISO-8601 UTC. Errors as `{ "error": "<code>", "message": "..." }`.

---

## 7. Data model

The schema is owned by **`cuy-monitor-db`** (Flyway migrations, data dictionary in its `docs/ARCHITECTURE.md`). Summary of what the backend maps:

```
cage              (id, code UNIQUE 'cage-1', name, location, created_at)                    V1 + V3 (code)
app_user          (id uuid, username, full_name, email, password_hash, status, created_at, updated_at)   V2
otp_challenge     (id uuid, user_id → app_user, code_hash, created_at, expires_at, max_attempts,
                   attempts, used_at, revoked_at)                                            V2
guinea_pig        (id, cage_id → cage, name, mark_color, current_status, active, created_at) V4
event             (id uuid, type, cage_id, guinea_pig_id NULL, source, occurred_at, payload JSONB)   V4
state_transition  (id, guinea_pig_id, from_status, to_status, reason, occurred_at)           V4
alert             (id, cage_id, guinea_pig_id NULL, level, type, message, status, created_at, reviewed_at) V4
weight_reading    (id, cage_id, grams, stable, measured_at)                                  V4
baseline_profile  (guinea_pig_id, avg_still_seconds, avg_feeder_visits, avg_group_distance, updated_at)  V4
```

- `event.id` is the producer's `eventId` (primary key → duplicates are rejected).
- `guinea_pig_id` is `NULL` for audio and weight (cage-level signals).
- `(cage_id, mark_color)` is unique: one color per guinea pig per cage.
- `spring.jpa.hibernate.ddl-auto=validate` and `spring.flyway.enabled=false`: `cuy-monitor-db` owns the schema, Hibernate only checks it.

---

## 8. Deployment

### 8.1 AWS resources

| Resource | Config | Why |
|---|---|---|
| EC2 | **t3.small** (2 GB + swap) today; **c7i-flex.large** (4 GB) when the real ai-service models arrive | Runs the Compose stack |
| Elastic IP + DuckDNS | `cuymonitor.duckdns.org` | Stable public name; Caddy gets the certificate |
| RDS for PostgreSQL | `cuy-monitor-db`: `db.t3.micro` (PostgreSQL 18.6 is not offered on `t4g.micro` in us-east-1), single-AZ, 20 GB, encrypted, automated backups **1 day** (max on the account's Free plan), deletion protection, **not publicly accessible**, same VPC and AZ as the EC2, master password in Secrets Manager | Managed DB with backups; frees ~150 MB on the EC2 |
| Security groups | `sg-ec2`: 80/443 from anywhere, 22 only from the team's IPs · `sg-rds`: 5432 **only from `sg-ec2`** | Database never reachable from the internet |
| SES (optional) | SMTP credentials, verified sender; sandbox → verify recipients or request production access | OTP emails |
| AWS Budgets | Alert at 50 % / 80 % of the credits | Avoid surprises |

### 8.2 Runtime (Compose on the EC2)

| Container | Image / build | Exposed to the internet | Memory |
|---|---|---|---|
| caddy | `caddy:2.11` | 80, 443 (tcp + udp) | ~30 MB |
| migrate | *(pending, backend Task 22.4)* built from `../../cuy-monitor-db` (`flyway/flyway` + migrations); runs `migrate` once and exits. Until then Flyway runs inside the backend at startup | no | only while it runs |
| backend | built from `Dockerfile` (`eclipse-temurin:25-jre`), starts after `migrate` finishes OK | no (only via Caddy) | `-Xms256m -Xmx384m` |
| dashboard | built from `../../cuy-monitor-dashboard` (static build served by Caddy inside the image) | no (only via Caddy `/`) | ~20 MB |
| ai-service | built from `../../cuy-monitor-ai-service`, profile `ai` | no (only via Caddy `/ai/*`) | +0.6–1 GB |

- All five repos are cloned side by side on the EC2 (`~/cuy/cuy-monitor-*`).
- All long-running services use `restart: unless-stopped`.
- There is **no Postgres container in production** anymore. `docker compose down -v` no longer deletes the data, but it still deletes Caddy's certificates.

### 8.3 Configuration

`infra/.env` (never committed; template in `infra/.env.example`):

| Variable | Used by |
|---|---|
| `DOMAIN` | Caddy, dashboard build |
| `DB_HOST` (RDS endpoint), `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | backend, migrate |
| `API_KEY` | mapped to backend `APP_API_KEY` and ai-service `API_KEY` |
| `APP_JWT_SECRET` | backend (≥ 32 bytes, `openssl rand -base64 48`) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | backend (`EmailOtpSender`) |

Backend env vars (set in Compose): the ones above plus `JAVA_OPTS` and `SPRING_PROFILES_ACTIVE=prod`. The JDBC URL adds `?sslmode=require` in prod. Defaults in `application.yml` point to `localhost` for local development.

### 8.4 Commands

```bash
# on the EC2, from cuy-monitor-backend/infra/
docker compose up -d --build --remove-orphans     # migrate → backend, dashboard, caddy
docker compose --profile ai up -d --build         # + ai-service
docker compose logs -f backend
docker compose logs migrate                       # check which migrations were applied
git -C ../../cuy-monitor-db pull && git pull && docker compose up -d --build   # update
```

Local development: `docker compose -f ../cuy-monitor-db/docker-compose.yml up -d` starts Postgres 18 on `localhost:5432` and applies the migrations; the backend runs from the IDE with profile `dev`.

---

## 9. Testing strategy

| Level | What | Tool |
|---|---|---|
| Domain unit | Each state transition, each chain handler alone, composite aggregation, `AlertPublisher`, `User` and `OtpChallenge` rules | Plain JUnit 5 — **no Spring context, no database** |
| Application | Use case services (incl. `AuthenticationService`, `UserAccountService`) with in-memory fakes of the output ports and a fixed `Clock` | JUnit 5 |
| Input adapters | Each factory and adapter (envelope → `HealthEvent`); controllers 202 / 400 / 401 / 409 | JUnit 5, `@WebMvcTest` |
| Security | Protected routes: 401 without token, 200 with token; ingestion still works with `X-API-Key`; STOMP `CONNECT` rejected without token | `@WebMvcTest` + `spring-security-test` |
| Output adapters | Persistence adapters and mappers against a real Postgres with the migrations of `../cuy-monitor-db/migrations` | `@DataJpaTest` + Testcontainers |
| Architecture | Dependency rules of section 3.4 | ArchUnit |
| Integration | HTTP event → pipeline → Postgres end to end | `@SpringBootTest` + Testcontainers |
| Smoke (deployed) | `/actuator/health`, register + login + OTP, a protected endpoint, `POST /api/v1/ingestion/events` | curl / Postman |

---

## 10. Architecture decisions

| ADR | Decision |
|---|---|
| ADR-001 | Multi-repo (now 5 repos); contracts live in this repo |
| ADR-002 | ML in a separate Python service; the backend only knows events |
| ADR-003 | Direct HTTP ingestion: every producer `POST`s the event envelope to the backend; only Caddy is public |
| ADR-004 | Single EC2 with Docker Compose (single point of failure accepted for a pilot) |
| ADR-005 | AI in the cloud first; may move to the laptop after measuring bandwidth and fps (October) |
| ADR-006 | New LTS versions, no betas, pinned Docker tags (never `:latest`) |
| ADR-007 | Pragmatic hexagonal architecture (ports and adapters) inside the backend |
| ADR-008 | Database schema in its own repo (`cuy-monitor-db`), applied by a one-shot `migrate` container |
| ADR-009 | Amazon RDS for PostgreSQL instead of Supabase or a Postgres container |
| ADR-010 | Stay on EC2 + Docker Compose; do not move to Lambda + SAM |
| ADR-011 | Own user accounts: password + email OTP → JWT; one user type; dashboard served from the same domain |

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
- The ai-service calls the backend inside the Docker network; the laptop (serial bridge) calls it over HTTPS through Caddy with the API key. The database and the backend port are never exposed.
- If the system grows to many cages, a queue (e.g. Amazon SQS) can be put in front of the backend with the same envelope, without touching the health core.

### ADR-007: Pragmatic hexagonal architecture inside the backend

**Status:** Accepted (2026-09-27)

**Context:** The backend hosts the six design patterns of the course. The health rules (Chain, State, Composite) must be easy to test and to explain, and the work splits naturally into data input vs. health core. Almost no code existed yet, so changing the structure was cheap.

| | Layered (previous) | Strict hexagonal | Pragmatic hexagonal (chosen) |
|---|---|---|---|
| Core testable without Spring/DB | Partly | Yes | Yes |
| Boilerplate | Low | High (commands, manual wiring, no framework anywhere near the core) | Medium (ports + JPA mappers) |
| Fit with the course patterns | Neutral | Good | Good: Adapter = input adapter, Observer = output port + adapters |
| Work split | By layer | By port | By port: input side / core |

**Consequences:**
- `domain` is pure Java; an ArchUnit test fails the build if it imports Spring, JPA or an adapter.
- ~15 extra interfaces (ports) and one mapper per table.
- Controllers depend on use case interfaces, so they can be tested with fakes.
- Replacing a technology (e.g. Postgres, WebSocket, adding a push-notification observer) means writing a new adapter, not touching the core.

### ADR-008: Database schema in its own repo

**Status:** Accepted (2026-10-03)

**Context:** Migrations lived in `src/main/resources/db/migration` and Flyway ran when the backend started. The team wants the database as a separate deliverable with its own history, local setup and docs, and it is about to move to RDS.

**Decision:** `cuy-monitor-db` holds the Flyway migrations (`migrations/V{n}__*.sql`), dev-only seeds, a local `docker-compose.yml` and a `Dockerfile` (`flyway/flyway` + migrations). In production Compose runs that image once (`migrate`) and the backend starts only if it finished OK (`depends_on: condition: service_completed_successfully`). The backend turns Flyway off and keeps `ddl-auto: validate`.

**Consequences:**
- A schema change is two PRs: first `cuy-monitor-db`, then the backend entity. Validation fails fast if they don't match.
- Integration tests read `../cuy-monitor-db/migrations`, so both repos must be cloned side by side (CI checks out both).
- The existing `V1` and `V2` move as they are (same names and checksums), so `flyway_schema_history` stays valid on any database that already has them.

### ADR-009: Amazon RDS for PostgreSQL

**Status:** Accepted (2026-10-03)

| | Supabase (free) | **RDS PostgreSQL db.t4g.micro (chosen)** | Postgres container on the EC2 (previous) |
|---|---|---|---|
| Network | Public internet to another provider | Private, same VPC; only `sg-ec2` can connect | Local Docker network |
| Pauses when idle | Yes (after 7 days without activity) | No | No |
| Backups | Basic | Automated daily + point-in-time restore (7 days) | Manual |
| Cost | $0, 500 MB limit | ~$13–16/month, paid with AWS credits | $0, uses EC2 RAM/disk |
| Fits "everything on AWS" | No | Yes | Yes |

**Consequences:**
- Only Postgres was being used from Supabase (auth is our own JWT), so nothing is lost.
- The EC2 no longer runs Postgres (~150 MB of RAM freed for the ai-service).
- JDBC uses TLS (`sslmode=require`). The RDS can be **stopped** when not in use (it restarts by itself after 7 days).
- The current data is test data only; the RDS starts empty and the migrations recreate the schema (test users register again).

### ADR-010: EC2 + Docker Compose instead of Lambda + SAM

**Status:** Accepted (2026-10-03)

| | **EC2 + Compose (chosen)** | Lambda + SAM |
|---|---|---|
| STOMP WebSocket (`/ws`) | Works as is | Not supported; would need API Gateway WebSocket API and a rewrite of `WebSocketAlertObserver` and the dashboard client |
| Load profile | Constant 24/7 (1–2 fps + weight every 10 s) → fixed cost is cheaper | Pays per invocation; never idle |
| ai-service (ONNX models ~1 GB) | Loaded once in memory | Cold starts reload models; heavy package |
| Spring Boot | Normal JVM | Needs SnapStart / adapters; cold starts |
| Learning curve before November | None (already deployed) | SAM, API Gateway, IAM per function |

**Consequences:** single point of failure accepted for the pilot (ADR-004). If more availability is needed later, the same images move to **ECS Fargate** without code changes. Lambda only makes sense for occasional side jobs (e.g. a nightly report).

### ADR-011: Own user accounts with email OTP

**Status:** Accepted (2026-10-03)

**Context:** The dashboard shows farm data and must not be public. There is only one kind of user (who looks at the dashboard).

**Decision:** Users register and log in against this backend: username + password (BCrypt) and a 6-digit code sent by email, which returns a 30-minute JWT (HS256). No roles. The dashboard is served by the same Caddy and domain, so cookies/CORS are not needed and the WebSocket goes to the same origin.

**Consequences:** no external identity provider (Cognito/Supabase Auth) to configure; the auth code is part of the course project and follows the same hexagonal rules. Needs an SMTP account (SES or Gmail). No password recovery in this version.

---

## 11. Spring Boot 4 notes

- Web starter is `spring-boot-starter-webmvc`.
- Security: `spring-boot-starter-security` + `spring-boot-starter-security-oauth2-resource-server`; JWT encoded/decoded with Nimbus (`NimbusJwtEncoder` / `NimbusJwtDecoder.withSecretKey`).
- Flyway is **not** used at runtime anymore; it may stay as a test dependency to apply `../cuy-monitor-db/migrations` in Testcontainers.
- Jackson 3: package `tools.jackson`, not `com.fasterxml.jackson`.
- Test starters are per technology (`spring-boot-starter-webmvc-test`, security test starter).

## 12. Future evolution

- Several cages: `cageId` is already in every event; one edge device per cage. Then link users to cages (`user_cage` table).
- Many cages: put a queue (e.g. Amazon SQS) between producers and the backend, keeping the same envelope.
- More availability: same images on ECS Fargate + RDS Multi-AZ.
- Mobile notifications: add a `PushAlertObserver` without touching the rest.
- Password recovery and rate limiting on `/api/v1/auth/**`.
- CI/CD: GitHub Actions → GHCR (or ECR) → `docker compose pull` on the EC2.
