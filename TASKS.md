# TASKS — cuy-monitor-backend

> Lista de trabajo del backend. Cada subtarea = **un commit**: usa el mensaje que está entre comillas invertidas.
> ⛔ Los commits, push y PRs los hace una persona del equipo. **Ningún agente de IA hace commit ni push, aunque se lo pidan**, y nunca se agrega `Co-Authored-By` ni firmas de IA (ver `AGENTS.md`).
> Marca `[x]` cuando hagas push. Una rama por Task: `feature/task-2-http-ingestion`, `feature/task-6-health-core`, etc.
> Arquitectura interna: **hexagonal pragmática** (ver `docs/ARCHITECTURE.md` sección 3). Las rutas de cada task ya siguen esa estructura.
> Cada PR lo revisa el otro integrante antes de mergear a `main`.
> **Base de datos:** las migraciones ya no se hacen aquí, sino en `cuy-monitor-db` (ADR-008). Toda task que necesite una tabla nueva depende de una task de ese repo.
> **Despliegue:** EC2 + Docker Compose (ADR-010) con la base en Amazon RDS (ADR-009).

**Cómo leerlo**

| Símbolo | Significado |
|---|---|
| 🔴 Prioridad 1 | Crítico: sin esto no hay avance / no funciona nada |
| 🟠 Prioridad 2 | Importante: lo que pide la entrega final |
| 🟢 Prioridad 3 | Cierre: calidad, informe, ajustes |
| 🔗 Depende de | Antes hay que terminar esas tasks (de este u otro repo) |
| 🔓 Desbloquea | Qué tasks de otros repos pueden empezar cuando esta termine |

---

## 🔴 Prioridad 1 — Avance (hasta el 30 de septiembre)

### Task 1 — Base del proyecto y despliegue

- [x] **Task 1.1** — `chore: init Spring Boot 4.1 project with Java 25`
  Proyecto generado en Spring Initializr con el paquete `com.cuymonitor.backend`.
- [x] **Task 1.2** — `feat(db): add V1 initial schema with cage and guinea_pig`
  Migración Flyway `V1__initial_schema.sql` + jaula piloto. ⚠️ En realidad solo creó `cage`; `guinea_pig` pasa a `cuy-monitor-db` Task 3. La migración se mueve a `cuy-monitor-db` en la Task 21.
- [x] **Task 1.3** — `build: add multi-stage Dockerfile and dockerignore`
- [x] **Task 1.4** — `chore(infra): add docker compose, Caddyfile and env example`
- [x] **Task 1.5** — *(sin commit)* EC2 t3.small + Elastic IP + DuckDNS + `docker compose up`
  Verificado: `/actuator/health` responde `UP` por HTTPS.

### Task 2 — Estructura hexagonal e ingesta por HTTP

- [x] **Task 2.1** — `refactor(ingestion): remove Kafka classes and dependencies`
  Borrar `KafkaConfigTopics.java`, `ingestion/kafka/`, dependencias con `kafka` en el `pom.xml`, sección `spring.kafka` del `application.yml`.
- [x] **Task 2.2** — `refactor: move to hexagonal package layout`
  Crear `domain/{model,health,notification,port/in,port/out}`, `application/`, `adapter/in/{web,ingestion}`, `adapter/out/{persistence,notification}`, `config/`. Mover `SystemController` a `adapter/in/web/`. Un `package-info.java` por paquete principal explicando qué va ahí.
- [x] **Task 2.3** — `feat(domain): add EventType enum and IngestionEvent envelope`
  `EventType` en `domain/model/`; `IngestionEvent` en `adapter/in/ingestion/dto/`.
- [x] **Task 2.4** — `feat(web): add IngestionController with API key check`
  `adapter/in/web/IngestionController`: `POST /api/v1/ingestion/events` → `202` / `400` / `401`. Por ahora solo registra en el log.
- [x] **Task 2.5** — `chore(infra): remove broker service from compose files`
- [x] **Task 2.6** — *(sin commit)* desplegar con `docker compose up -d --build --remove-orphans` y probar el `POST` desde Postman

🔓 **Desbloquea:** `cuy-monitor-ai-service` Task 3 · `cuy-monitor-arduino` Task 5

### Task 3 — Contratos y documentación

- [x] **Task 3.1** — `docs: add PRD, ARCHITECTURE, AGENTS and TASKS`
- [x] **Task 3.2** — `docs(contracts): add events envelope and payloads`
  `docs/contracts/events.md`: sobre común, `BEHAVIOR`, `AUDIO`, `WEIGHT`, enums compartidos.
- [x] **Task 3.3** — `docs(contracts): add REST API and WebSocket messages`
  `docs/contracts/rest-api.md`: endpoints del dashboard, ingesta y formato de los mensajes STOMP.
- [x] **Task 3.4** — `docs(contracts): add example JSON per event type`
  `docs/contracts/examples/behavior.json`, `audio.json`, `weight.json`.

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 2 · `cuy-monitor-ai-service` Task 2 · `cuy-monitor-arduino` Task 5 · `cuy-monitor-db` Task 3

### Task 4 — Dominio, puertos de salida y persistencia

🔗 **Depende de:** Task 2.2 de este repo (estructura hexagonal) · `cuy-monitor-db` Task 3 (tablas de salud, para las Task 4.6–4.8)

- [x] **Task 4.1** — `feat(domain): add MarkColor, HealthStatus and AlertStatus enums`
- [x] **Task 4.2** — `feat(domain): add Cage, GuineaPig and HealthEvent models`
  Clases/records de Java puro en `domain/model/`, sin anotaciones. `HealthEvent` es la frontera entre las dos mitades *(revisar en equipo: es la frontera entre la entrada y el núcleo)*.
- [x] **Task 4.3** — `feat(domain): add repository output ports`
  Interfaces en `domain/port/out/`: `CageRepository`, `GuineaPigRepository`, `EventRepository`, `StateTransitionRepository`, `AlertRepository`, `WeightReadingRepository`, `BaselineProfileRepository`.
- [ ] **Task 4.4** — ~~`feat(db): add V2 migration…`~~ **Se movió a `cuy-monitor-db` Task 3** (`V4__create_health_tables.sql`). Aquí no se hace commit.
- [x] **Task 4.5** — `feat(domain): add Alert, WeightReading, BaselineProfile and StateTransition models`
- [ ] **Task 4.6** — `feat(persistence): add JPA entities, Spring Data repositories and mappers`
  `adapter/out/persistence/{entity,repository,mapper}/`.
- [ ] **Task 4.7** — `feat(persistence): add persistence adapters implementing the repository ports`
- [ ] **Task 4.8** — `test(persistence): cover adapters and mappers with Testcontainers`

### Task 5 — Factory Method + Adapter (entrada)

🔗 **Depende de:** seguir con las Task 2.3–2.4 y 4.2 de este repo

- [ ] **Task 5.1** — `feat(ingestion): add EventSourceAdapter target interface`
  `adapter/in/ingestion/adapter/`.
- [ ] **Task 5.2** — `feat(ingestion): add camera, audio and weight adapters`
  `CameraBehaviorAdapter`, `AudioClassificationAdapter`, `WeightReadingAdapter` → `HealthEvent`.
- [ ] **Task 5.3** — `feat(ingestion): add AdapterFactory and concrete factories`
  `adapter/in/ingestion/factory/`.
- [x] **Task 5.4** — `feat(domain): add ProcessEventUseCase input port` *(contrato entre la entrada y el núcleo: revisarlo en equipo)*
- [ ] **Task 5.5** — `feat(web): wire IngestionController to AdapterFactory and ProcessEventUseCase`
- [ ] **Task 5.6** — `test(ingestion): cover factory selection and each adapter`

### Task 6 — Núcleo de salud

🔗 **Depende de:** seguir con las Task 4.2–4.3 de este repo

- [x] **Task 6.1** — `feat(chain): add EventHandler and the four handlers`
  `domain/health/chain/`: `ValidationHandler`, `IdentificationHandler`, `BehaviorThresholdHandler`, `SustainedAnomalyHandler` (lógica simple por ahora; reciben los puertos que necesitan por constructor).
- [x] **Task 6.2** — `feat(chain): add HandlerChainBuilder`
- [x] **Task 6.3** — `feat(state): add HealthState and the four states`
- [x] **Task 6.4** — `feat(state): add GuineaPigHealthContext`
- [x] **Task 6.5** — `feat(composite): add HealthComponent, leaves and CageHealth`
- [x] **Task 6.6** — `feat(application): add EventProcessingService implementing ProcessEventUseCase`
  Orquesta cadena → estado → composite → `AlertPublisher`.
  🔗 Depende de: Task 5.4
- [x] **Task 6.7** — `feat(config): add DomainConfig wiring the chain and domain beans`
- [x] **Task 6.8** — `test(health): cover each handler, each transition and cage aggregation with plain JUnit`
  Con fakes en memoria de los puertos; sin Spring ni base de datos.

### Task 7 — Observer + WebSocket

- [x] **Task 7.1** — `feat(observer): add AlertObserver output port and AlertPublisher`
  `AlertObserver` en `domain/port/out/`, `AlertPublisher` en `domain/notification/`.
- [x] **Task 7.2** — `feat(observer): add DatabaseAlertObserver and LogAlertObserver`
  En `adapter/out/notification/`; `DatabaseAlertObserver` guarda usando el puerto `AlertRepository`.
- [x] **Task 7.3** — `feat(websocket): add STOMP config on /ws`
  Se hizo en la rama de la Task 20 (`feature/task-20-websocket-jwt`) porque el interceptor la necesita.
- [x] **Task 7.4** — `feat(observer): add WebSocketAlertObserver publishing to /topic/cages/{id}`
- [x] **Task 7.5** — `feat(state): publish alerts on transitions to ALERT and CRITICAL` *(el State llama a `AlertPublisher`)*

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 6

### Task 8 — API para el dashboard

Cada endpoint = puerto de entrada en `domain/port/in` + servicio en `application/` + controller en `adapter/in/web/`. Todos exigen JWT cuando la Task 18.14 esté mergeada (mientras tanto, se prueban sin token).

- [ ] **Task 8.1** — `feat(config): allow localhost:5173 in CORS for the dev profile`
  En producción el dashboard se sirve desde el mismo dominio (Caddy), así que no hace falta CORS.
- [ ] **Task 8.2** — `feat(web): add cage health and guinea pigs list endpoints`
  `GetCageHealthUseCase`, `ListGuineaPigsUseCase` → `GET /api/v1/cages/{id}/health`, `GET /api/v1/cages/{id}/guinea-pigs`.
- [ ] **Task 8.3** — `feat(web): add endpoint to register a guinea pig`
  `RegisterGuineaPigUseCase` → `POST /api/v1/cages/{id}/guinea-pigs`.
- [ ] **Task 8.4** — `feat(web): add alerts list endpoint`
  `ListAlertsUseCase` → `GET /api/v1/alerts?status=`.
- [ ] **Task 8.5** — *(sin commit)* desplegar y probar cada endpoint en Postman

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 5

### Task 9 — Datos falsos para el avance

- [ ] **Task 9.1** — `feat(dev): add fake producer that posts random events`
  `dev/fake-producer/` manda eventos `BEHAVIOR`, `AUDIO` y `WEIGHT` a `/api/v1/ingestion/events`.
- [ ] **Task 9.2** — *(sin commit)* dejarlo corriendo contra el servidor y ver cambios de estado en el dashboard

---

## 🟠 Prioridad 2 — Entrega final (octubre)

### Task 10 — Reglas reales de State

- [ ] **Task 10.1** — `feat(state): add escalation rules between states`
- [ ] **Task 10.2** — `feat(state): add recovery back to NORMAL`
- [ ] **Task 10.3** — `feat(state): persist every transition through StateTransitionRepository`
- [ ] **Task 10.4** — `test(state): cover escalation and recovery paths`

### Task 11 — Perfil normal y umbrales

- [ ] **Task 11.1** — `feat(domain): compute BaselineProfile per guinea pig`
- [ ] **Task 11.2** — `feat(chain): compare windows against the baseline in BehaviorThresholdHandler`
- [ ] **Task 11.3** — `feat(chain): require N consecutive anomalous windows in SustainedAnomalyHandler`
- [ ] **Task 11.4** — `feat(config): make thresholds and N configurable in application.yml`
  Los valores entran al dominio desde `DomainConfig` (el dominio no lee `application.yml` directamente).

### Task 12 — Audio y peso en el Composite

🔗 **Depende de:** `cuy-monitor-ai-service` Task 11 (audio real) y `cuy-monitor-arduino` Task 5 (peso real) para probar con datos reales

- [ ] **Task 12.1** — `feat(composite): evaluate cage audio health from DISTRESS events`
- [ ] **Task 12.2** — `feat(composite): evaluate cage weight trend from stable readings`
- [ ] **Task 12.3** — `feat(application): store WEIGHT events through WeightReadingRepository`

### Task 13 — API de historial y alertas

- [ ] **Task 13.1** — `feat(web): add guinea pig history endpoint by date range`
  `GetGuineaPigHistoryUseCase` → `GET /api/v1/guinea-pigs/{id}/history`.
- [ ] **Task 13.2** — `feat(web): add cage weight history endpoint by date range`
  `GetWeightHistoryUseCase` → `GET /api/v1/cages/{id}/weight`.
- [ ] **Task 13.3** — `feat(web): add endpoint to mark an alert as reviewed`
  `ReviewAlertUseCase` → `PATCH /api/v1/alerts/{id}`.

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 8, 9 y 10

### Task 14 — Pruebas de arquitectura e integración

- [ ] **Task 14.1** — `test(architecture): enforce hexagonal dependency rules with ArchUnit`
  Agrega `com.tngtech.archunit:archunit-junit5` (scope test) y las reglas de la sección 3.4 de `ARCHITECTURE.md`. *Conviene hacerla apenas termine la Task 2.2.*
- [ ] **Task 14.2** — `test(integration): add Testcontainers Postgres setup`
- [ ] **Task 14.3** — `test(integration): cover event → state change → alert end to end`
- [ ] **Task 14.4** — `test(web): cover controllers responses with WebMvcTest and fake use cases`

---

## 🟢 Prioridad 3 — Cierre (noviembre)

### Task 15 — Ajuste con datos reales

🔗 **Depende de:** `cuy-monitor-ai-service` Task 13 y `cuy-monitor-arduino` Task 7 (sistema montado en la jaula)

- [ ] **Task 15.1** — `chore(config): tune thresholds after the farm test`
- [ ] **Task 15.2** — `docs: record false positives and negatives from the farm test`

### Task 16 — Informe y sustentación

- [ ] **Task 16.1** — `docs(diagrams): add hexagon diagram and UML class diagram per pattern`
- [ ] **Task 16.2** — `docs(adr): add ADR files 001 to 007`
- [ ] **Task 16.3** — `docs: update README with setup, deploy and screenshots`

### Task 17 — Limpieza e infraestructura

- [ ] **Task 17.1** — `refactor(web): remove SystemController smoke endpoint`
- [ ] **Task 17.2** — *(sin commit)* medir RAM con el ai-service y decidir si subir la EC2 a c7i-flex.large
- [ ] **Task 17.3** — `ci: add GitHub Actions build and test on pull requests` *(opcional)*
  Hace checkout también de `cuy-monitor-db` al lado, para que los tests de integración encuentren las migraciones.

---

## 🔐 Autenticación y cuentas de usuario

Un solo tipo de usuario (básico, sin roles ni admin). Cualquiera puede registrarse desde la pantalla de login ("¿No tienes cuenta?"). Cada usuario solo ve y modifica **su propia cuenta** (`/api/v1/users/me`); no hay listado de usuarios.

Se divide en tres tareas, cada una con su rama y su PR: la Task 19 (CRUD) y la Task 20 (WebSocket con JWT) necesitan el modelo `User`, la seguridad y el JWT de la Task 18, así que sus ramas salen de `main` cuando la 18 ya esté mergeada.

Las tablas `app_user` y `otp_challenge` (migración `V2`) pasan a `cuy-monitor-db` en la Task 21; la Task 18 ya no crea migraciones nuevas.

**Seguridad (aplica a las dos tareas)**

- Contraseña guardada con **BCrypt** (hash de un solo sentido con sal, no cifrado reversible). El código OTP también se guarda **hasheado**.
- Política de contraseña: 8 a 72 caracteres (72 bytes es el límite de BCrypt).
- OTP de un solo uso, expira en 5 min, máximo 5 intentos; al pedir un código nuevo se invalidan los anteriores del mismo usuario.
- JWT firmado **HS256** con secreto de ≥ 32 bytes desde `APP_JWT_SECRET`, expira en 30 min. Claims `sub` (id del usuario), `iat`, `exp`, `iss`.
- Login con respuesta genérica (`401 invalid credentials`) para no revelar si el usuario existe; se hashea igual aunque no exista. Las cuentas `DISABLED` también responden genérico.
- **Logout**: la API es sin estado, así que cerrar sesión es borrar el token en el dashboard; el token vence solo a los 30 min. No hay lista de tokens revocados.
- API sin sesión (`STATELESS`), CSRF desactivado por ser API con Bearer.
- Ningún secreto en el código ni en `application.yml`: todo por variables de entorno.
- Fuera de alcance por ahora: límite de peticiones (rate limiting), recuperar contraseña olvidada y cambio de correo.

**SOLID**

- **S**: `AuthenticationService` (registro, login, OTP) y `UserAccountService` (CRUD de la cuenta) están separados; `OtpChallenge` decide sus propias reglas (expiró, intentos, usado).
- **O**: cambiar BCrypt por Argon2 o el correo por SMS es crear otro adaptador del puerto, sin tocar los servicios.
- **L**: `EmailOtpSender` y `LogOtpSender` son intercambiables detrás de `OtpSender`; los fakes en memoria de los tests también.
- **I**: un puerto de entrada por caso de uso (`RegisterUserUseCase`, `LoginUseCase`, …); cada controller depende solo de lo que usa.
- **D**: los servicios dependen de puertos (`UserRepository`, `PasswordHasher`, `TokenIssuer`, `OtpSender`), nunca de JPA, Spring Security ni JavaMail.

**Patrones**: no se agrega un patrón GoF nuevo. `OtpSender` funciona como **Strategy** (correo en prod, log en `dev`, elegido por perfil). Un State para `UserStatus` no aporta con 3 estados y reglas simples: basta un enum.

### Task 18 — Registro y login con JWT + OTP por correo

Rama: `feature/task-18-auth-jwt-otp`.

**Flujo**

1. **Registro**: `POST /api/v1/auth/register` `{ username, fullName, email, password }` → `201 { challengeId, expiresAt }`. El usuario queda en `PENDING_VERIFICATION` y se le envía un código de 6 dígitos al correo.
2. **Login**: `POST /api/v1/auth/login` `{ username, password }` → `200 { challengeId, expiresAt }` y se envía un código nuevo al correo. También sirve para reenviar el código a una cuenta que aún no se verificó.
3. **Verificar OTP**: `POST /api/v1/auth/otp/verify` `{ challengeId, code }` → `200 { accessToken, tokenType: "Bearer", expiresAt }`. Si la cuenta estaba `PENDING_VERIFICATION`, pasa a `ACTIVE` (el código prueba que el correo es suyo).
4. El resto de `/api/v1/**` pide `Authorization: Bearer <jwt>`. `/api/v1/ingestion/**` sigue con `X-API-Key`; `/api/v1/auth/**` y `/actuator/health` son públicos.

**Arquitectura**

```
domain/model/user/          User, UserStatus, PasswordPolicy                              (Java puro)
domain/model/auth/          OtpChallenge, LoginChallenge, AuthToken
domain/exception/           InvalidCredentialsException, InvalidOtpException,
                            UserAlreadyExistsException, WeakPasswordException
domain/port/in/             RegisterUserUseCase, LoginUseCase, VerifyOtpUseCase
domain/port/out/            UserRepository, OtpChallengeRepository, PasswordHasher, OtpSender, TokenIssuer
application/                AuthenticationService
adapter/in/web/             AuthController, AuthExceptionHandler, dto/
adapter/out/persistence/    UserJpaEntity, OtpChallengeJpaEntity, repos Spring Data, mappers, adaptadores
adapter/out/security/       BCryptPasswordHasher, JwtTokenIssuer
adapter/out/mail/           EmailOtpSender, LogOtpSender (perfil dev)
config/                     SecurityConfig, JwtConfig, AuthProperties, AuthConfig
```

**Subtareas**

- [x] **Task 18.1** — `docs(tasks): add Task 18 for JWT login with email OTP`
- [x] **Task 18.2** — `build: add spring security, oauth2 resource server and mail starters`
  Dependencias del BOM, sin versión a mano: starter de Security, starter de OAuth2 Resource Server (Nimbus para firmar/validar JWT), starter de Mail y el starter de test de Security.
- [x] **Task 18.3** — `docs(tasks): split auth into registration/login and account CRUD tasks`
- [x] **Task 18.4** — `feat(domain): add User, OtpChallenge and auth models`
  `User` (crear pendiente, activar, saber si puede iniciar sesión), `UserStatus`, `PasswordPolicy`, `OtpChallenge`, `LoginChallenge`, `AuthToken` y excepciones del dominio.
- [x] **Task 18.5** — `test(domain): cover User and OtpChallenge rules`
  JUnit puro: OTP expira, agota intentos, un solo uso; usuario se activa; política de contraseña.
- [x] **Task 18.6** — `feat(domain): add auth ports`
  `RegisterUserUseCase`, `LoginUseCase`, `VerifyOtpUseCase`, `UserRepository`, `OtpChallengeRepository`, `PasswordHasher`, `OtpSender`, `TokenIssuer`.
- [x] **Task 18.7** — `feat(application): add AuthenticationService for register, login and OTP`
  Genera el código con `SecureRandom`, lo hashea, invalida retos anteriores, envía el correo; al verificar activa la cuenta si hace falta y emite el token. Usa `java.time.Clock` inyectado.
- [x] **Task 18.8** — `test(application): cover AuthenticationService with in-memory fakes`
  Registro correcto, usuario o correo repetido, login correcto, contraseña incorrecta, usuario inexistente, cuenta `DISABLED`, código correcto (y activa la cuenta), incorrecto, expirado, intentos agotados, reutilizado.
- [x] **Task 18.9** — `feat(db): add V2 migration for app_user and otp_challenge`
  Se mueve tal cual a `cuy-monitor-db` (Task 21.1 de este repo · Task 2 de ese repo). Las tablas de salud quedan en `V4` (la `V3` agrega `cage.code`).
- [x] **Task 18.10** — `feat(persistence): add user and otp challenge JPA adapters`
  Entidades, Spring Data, mappers y adaptadores que implementan los puertos.
- [x] **Task 18.11** — `feat(security): add BCrypt password hasher and JWT token issuer`
  `adapter/out/security/`.
- [x] **Task 18.12** — `test(security): cover password hasher and JWT issuer`
  Hash/verify, y que el JWT emitido se valida con el decoder y trae `sub`, `iss` y `exp`.
- [x] **Task 18.13** — `feat(mail): add email and log OTP senders`
  `EmailOtpSender` con `JavaMailSender` (SMTP de Amazon SES o Gmail con contraseña de aplicación); `LogOtpSender` para el perfil `dev` (escribe el código en el log).
- [x] **Task 18.14** — `feat(config): add security filter chain and auth wiring`
  `SecurityConfig`, `JwtConfig`, `AuthProperties` (`app.auth.*`), `AuthConfig` (arma el servicio).
  ⚠️ **Urgente:** desde la Task 18.2 `spring-boot-starter-security` está en el `pom.xml` y todavía no hay `SecurityConfig`, así que Spring Boot protege **todos** los endpoints con su login por defecto (la ingesta y `/api/v1/system/status` responden `401`). No desplegar `main` en la EC2 hasta mergear esta subtarea, o adelantar un `SecurityConfig` mínimo que deje pasar `/api/v1/ingestion/**` (con su `X-API-Key`) y `/actuator/health`.
- [x] **Task 18.15** — `feat(web): add AuthController with register, login and OTP endpoints`
  DTOs `record` con `@Valid`; `AuthExceptionHandler` → `400` / `401` / `409`.
- [x] **Task 18.16** — `test(web): cover auth endpoints and protected routes`
  `@WebMvcTest` con casos de uso falsos + prueba de que una ruta protegida da `401` sin token, `200` con token, y que la ingesta sigue entrando con `X-API-Key`.
- [x] **Task 18.17** — `chore(infra): add JWT and SMTP env vars`
  `application.yml` (solo referencias `${...}`), `infra/.env.example`, `infra/docker-compose.yml`: `APP_JWT_SECRET`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
- [x] **Task 18.18** — `docs(contracts): add auth API contract`
  `docs/contracts/auth-api.md`. ⚠️ Avisar a `cuy-monitor-dashboard` (Task 13): necesita pantallas de registro y login + OTP, mandar el Bearer y borrar el token al cerrar sesión.
- [x] **Task 18.19** — *(sin commit)* `./mvnw test` en verde, prueba manual en Postman y PR a `main`

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 13 (registro y login) · Task 19 · Task 20

### Task 19 — CRUD de la cuenta del usuario

Rama: `feature/task-19-user-account-crud`, creada desde `main` después de mergear la Task 18.

**Endpoints** (todos con `Authorization: Bearer <jwt>`; el id sale del `sub` del token, nunca de la URL)

- `GET /api/v1/users/me` → datos de la cuenta (nunca el hash).
- `PUT /api/v1/users/me` `{ fullName }` → actualiza el perfil.
- `PUT /api/v1/users/me/password` `{ currentPassword, newPassword }` → cambia la contraseña.
- `DELETE /api/v1/users/me` `{ currentPassword }` → **desactiva** la cuenta (`DISABLED`, soft delete). Ya no puede iniciar sesión.

Estas rutas cargan el usuario en cada petición y rechazan cuentas `DISABLED` aunque el JWT siga vigente.

**Arquitectura** (se suma a lo de la Task 18)

```
domain/model/user/          User: cambiar perfil, cambiar contraseña, desactivar
domain/port/in/             GetCurrentUserUseCase, UpdateProfileUseCase, ChangePasswordUseCase, DeactivateAccountUseCase
application/                UserAccountService
adapter/in/web/             UserAccountController, dto/
```

**Subtareas**

- [x] **Task 19.1** — `feat(domain): add profile, password and deactivation rules to User`
- [x] **Task 19.2** — `test(domain): cover User account rules`
  Cambia perfil y contraseña, se desactiva, no cambia nada si está `DISABLED`.
- [x] **Task 19.3** — `feat(domain): add user account ports`
- [x] **Task 19.4** — `feat(application): add UserAccountService for the account CRUD`
- [x] **Task 19.5** — `test(application): cover UserAccountService with in-memory fakes`
  Ver perfil, actualizar, cambiar contraseña (actual correcta e incorrecta), desactivar (contraseña correcta e incorrecta), operar sobre una cuenta `DISABLED`.
- [x] **Task 19.6** — `feat(web): add UserAccountController for /api/users/me`
- [x] **Task 19.7** — `test(web): cover user account endpoints`
  `401` sin token, `200` con token, `401` con contraseña actual incorrecta, `204` al desactivar.
- [x] **Task 19.8** — `docs(contracts): add user account endpoints to auth API contract`
  ⚠️ Avisar a `cuy-monitor-dashboard` (Task 14): pantalla "mi cuenta".
- [x] **Task 19.9** — *(sin commit)* `./mvnw test` en verde, prueba manual en Postman y PR a `main`

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 14 (pantalla "mi cuenta")

### Task 20 — WebSocket protegido con JWT

Rama: `feature/task-20-websocket-jwt`, desde `main` después de mergear la Task 18 y la Task 7.3.

El navegador no puede mandar headers en el handshake del WebSocket, así que el token va en el frame STOMP `CONNECT` (`Authorization: Bearer <jwt>`) y se valida con un `ChannelInterceptor`.

- [x] **Task 20.1** — `feat(websocket): validate JWT on STOMP CONNECT with a channel interceptor`
  Usa el mismo `JwtDecoder` de la Task 18.14. Sin token o con token vencido → se rechaza el `CONNECT`.
- [x] **Task 20.2** — `feat(websocket): reject subscriptions from unauthenticated sessions`
- [x] **Task 20.3** — `test(websocket): cover CONNECT with valid, missing and expired tokens`
- [x] **Task 20.4** — `docs(contracts): document STOMP CONNECT auth header`
  ⚠️ Avisar a `cuy-monitor-dashboard` (Task 13.6): mandar el token en `connectHeaders`.

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 13.6

---

## 🗄️ Base de datos separada, RDS y despliegue con Docker

Decisiones: ADR-008 (repo `cuy-monitor-db`), ADR-009 (Amazon RDS en vez de Supabase o el contenedor de Postgres), ADR-010 (EC2 + Compose, no Lambda + SAM). Ver `docs/ARCHITECTURE.md` sección 8.

### Task 21 — Sacar las migraciones al repo `cuy-monitor-db`

Rama: `refactor/task-21-split-database`.
🔗 **Depende de:** `cuy-monitor-db` Task 1 y Task 2 (repo creado con `V1` y `V2` copiadas tal cual)

- [ ] **Task 21.1** — `refactor(db): remove Flyway migrations moved to cuy-monitor-db`
  Borrar `src/main/resources/db/migration/`. Los archivos ya viven en `cuy-monitor-db/migrations/` con el mismo nombre y contenido (mismo checksum).
- [ ] **Task 21.2** — `chore(config): disable Flyway at runtime and keep ddl-auto validate`
  `spring.flyway.enabled: false`. Flyway queda solo como dependencia de test *(propón el cambio del `pom.xml` y espera confirmación)*.
- [ ] **Task 21.3** — `test(integration): apply migrations from ../cuy-monitor-db in Testcontainers`
  Perfil de test con `spring.flyway.enabled: true` y `spring.flyway.locations: filesystem:../cuy-monitor-db/migrations`.
- [ ] **Task 21.4** — `chore(infra): remove local Postgres compose file`
  Borrar `infra/docker-compose.dev.yml`; la base local ahora se levanta con `cuy-monitor-db/docker-compose.yml`. Actualizar comandos del README.
- [ ] **Task 21.5** — `chore(config): add dev profile with log OTP sender and local CORS`

### Task 22 — Pasar a Amazon RDS

Rama: `chore/task-22-rds`.
🔗 **Depende de:** Task 21 · `cuy-monitor-db` Task 4 (imagen `migrate`)

- [x] **Task 22.1** — *(sin commit)* crear la RDS PostgreSQL `db.t4g.micro`, single-AZ, 20 GB gp3, sin acceso público, en la misma VPC y región que la EC2, respaldos de 7 días
  Hecho desde la consola: `cuy-monitor-db`, PostgreSQL 18.6 en `db.t3.micro` (18.6 no se ofrece en `t4g.micro` en us-east-1) y backups de **1 día** (máximo del plan Free de la cuenta). Security group `cuy-rds`: 5432 solo desde el SG de la EC2. Contraseña en Secrets Manager.
  Postgres 18 (o 17 si 18 no está disponible en la región). Security group `sg-rds`: 5432 solo desde `sg-ec2`.
- [x] **Task 22.2** — `chore(infra): replace postgres container with RDS connection settings`
  Quitar el servicio `postgres` y el volumen `pgdata` de `infra/docker-compose.yml`. Backend con `DB_HOST` (endpoint de la RDS), `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
- [x] **Task 22.3** — `chore(config): require TLS on the JDBC connection in prod`
  `?sslmode=require` en la URL del perfil `prod`.
- [ ] **Task 22.4** — `chore(infra): add one-shot migrate service built from cuy-monitor-db`
  ⏸️ Pendiente: `cuy-monitor-db` aún no tiene migraciones ni Dockerfile. Mientras tanto Flyway corre dentro del backend y crea el esquema en la RDS.
  `build: ../../cuy-monitor-db`, mismas variables `DB_*`; el backend con `depends_on: migrate: condition: service_completed_successfully`.
- [x] **Task 22.5** — `chore(infra): update env example for RDS, JWT and SMTP`
- [ ] **Task 22.6** — *(sin commit)* clonar `cuy-monitor-db` al lado del backend en la EC2, `docker compose up -d --build --remove-orphans`, revisar `docker compose logs migrate` y `/actuator/health`
  La RDS empieza vacía (solo había datos de prueba); los usuarios de prueba se registran de nuevo.
- [ ] **Task 22.7** — *(sin commit)* configurar AWS Budgets (50 % y 80 %) y anotar cómo detener la RDS cuando no se use

### Task 23 — Dashboard en el mismo stack de Docker

Rama: `chore/task-23-dashboard-container`.
🔗 **Depende de:** `cuy-monitor-dashboard` Task 4.4 (Dockerfile del dashboard)

- [x] **Task 23.1** — `chore(infra): add dashboard service built from cuy-monitor-dashboard`
  `build: ../../cuy-monitor-dashboard`, sin puertos publicados.
- [x] **Task 23.2** — `chore(infra): route the SPA through Caddy`
  En el `Caddyfile`, el `handle` final pasa de `respond "cuy-monitor API"` a `reverse_proxy dashboard:80`. `/api/*`, `/ws*`, `/actuator/health*` y `/ai/*` quedan igual.
- [ ] **Task 23.3** — *(sin commit)* desplegar y verificar en el celular: login, vista de jaula y WebSocket por `wss://cuymonitor.duckdns.org/ws`

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 4.5
