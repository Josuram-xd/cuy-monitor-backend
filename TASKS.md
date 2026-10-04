# TASKS — cuy-monitor-backend

> Lista de trabajo del backend. Cada subtarea = **un commit**: usa el mensaje que está entre comillas invertidas.
> Marca `[x]` cuando hagas push. Una rama por Task: `feature/task-2-http-ingestion`, `feature/task-6-health-core`, etc.
> Arquitectura interna: **hexagonal pragmática** (ver `docs/ARCHITECTURE.md` sección 3). Las rutas de cada task ya siguen esa estructura.

**Cómo leerlo**

| Símbolo | Significado |
|---|---|
| 🔴 Prioridad 1 | Crítico: sin esto no hay avance / no funciona nada |
| 🟠 Prioridad 2 | Importante: lo que pide la entrega final |
| 🟢 Prioridad 3 | Cierre: calidad, informe, ajustes |
| 🔗 Depende de | Antes hay que terminar esas tasks (de este u otro repo) |
| 🔓 Desbloquea | Qué tasks de otros repos pueden empezar cuando esta termine |
| 👤 | Dueño (el otro revisa el PR) |

---

## 🔴 Prioridad 1 — Avance (hasta el 30 de septiembre)

### Task 1 — Base del proyecto y despliegue 👤 Josuram

- [x] **Task 1.1** — `chore: init Spring Boot 4.1 project with Java 25`
  Proyecto generado en Spring Initializr con el paquete `com.cuymonitor.backend`.
- [x] **Task 1.2** — `feat(db): add V1 initial schema with cage and guinea_pig`
  Migración Flyway `V1__initial_schema.sql` + jaula de ejemplo `cage-1`.
- [x] **Task 1.3** — `build: add multi-stage Dockerfile and dockerignore`
- [x] **Task 1.4** — `chore(infra): add docker compose, Caddyfile and env example`
- [x] **Task 1.5** — *(sin commit)* EC2 t3.small + Elastic IP + DuckDNS + `docker compose up`
  Verificado: `/actuator/health` responde `UP` por HTTPS.

### Task 2 — Estructura hexagonal e ingesta por HTTP 👤 Juan

- [x] **Task 2.1** — `refactor(ingestion): remove Kafka classes and dependencies`
  Borrar `KafkaConfigTopics.java`, `ingestion/kafka/`, dependencias con `kafka` en el `pom.xml`, sección `spring.kafka` del `application.yml`.
- [x] **Task 2.2** — `refactor: move to hexagonal package layout`
  Crear `domain/{model,health,notification,port/in,port/out}`, `application/`, `adapter/in/{web,ingestion}`, `adapter/out/{persistence,notification}`, `config/`. Mover `SystemController` a `adapter/in/web/`. Un `package-info.java` por paquete principal explicando qué va ahí.
- [x] **Task 2.3** — `feat(domain): add EventType enum and IngestionEvent envelope`
  `EventType` en `domain/model/`; `IngestionEvent` en `adapter/in/ingestion/dto/`.
- [x] **Task 2.4** — `feat(web): add IngestionController with API key check`
  `adapter/in/web/IngestionController`: `POST /api/ingestion/events` → `202` / `400` / `401`. Por ahora solo registra en el log.
- [x] **Task 2.5** — `chore(infra): remove broker service from compose files`
- [x] **Task 2.6** — *(sin commit)* desplegar con `docker compose up -d --build --remove-orphans` y probar el `POST` desde Postman

🔓 **Desbloquea:** `cuy-monitor-ai-service` Task 3 · `cuy-monitor-arduino` Task 5

### Task 3 — Contratos y documentación 👤 Los dos

- [x] **Task 3.1** — `docs: add PRD, ARCHITECTURE, AGENTS and TASKS`
- [ ] **Task 3.2** — `docs(contracts): add events envelope and payloads`
  `docs/contracts/events.md`: sobre común, `BEHAVIOR`, `AUDIO`, `WEIGHT`, enums compartidos.
- [ ] **Task 3.3** — `docs(contracts): add REST API and WebSocket messages`
  `docs/contracts/rest-api.md`: endpoints del dashboard, ingesta y formato de los mensajes STOMP.
- [ ] **Task 3.4** — `docs(contracts): add example JSON per event type`
  `docs/contracts/examples/behavior.json`, `audio.json`, `weight.json`.

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 2 · `cuy-monitor-ai-service` Task 2 · `cuy-monitor-arduino` Task 5

### Task 4 — Dominio, puertos de salida y persistencia 👤 Josuram

🔗 **Depende de:** Task 2.2 de este repo (estructura hexagonal)

- [ ] **Task 4.1** — `feat(domain): add MarkColor, HealthStatus and AlertStatus enums`
- [ ] **Task 4.2** — `feat(domain): add Cage, GuineaPig and HealthEvent models`
  Clases/records de Java puro en `domain/model/`, sin anotaciones. `HealthEvent` es la frontera entre las dos mitades *(👤 revisar entre los dos)*.
- [ ] **Task 4.3** — `feat(domain): add repository output ports`
  Interfaces en `domain/port/out/`: `CageRepository`, `GuineaPigRepository`, `EventRepository`, `StateTransitionRepository`, `AlertRepository`, `WeightReadingRepository`, `BaselineProfileRepository`.
- [ ] **Task 4.4** — `feat(db): add V2 migration for event, state_transition, alert, weight_reading and baseline_profile`
- [ ] **Task 4.5** — `feat(domain): add Alert, WeightReading, BaselineProfile and StateTransition models`
- [ ] **Task 4.6** — `feat(persistence): add JPA entities, Spring Data repositories and mappers`
  `adapter/out/persistence/{entity,repository,mapper}/`.
- [ ] **Task 4.7** — `feat(persistence): add persistence adapters implementing the repository ports`
- [ ] **Task 4.8** — `test(persistence): cover adapters and mappers with Testcontainers`

### Task 5 — Factory Method + Adapter (entrada) 👤 Compañero

🔗 **Depende de:** seguir con las Task 2.3–2.4 y 4.2 de este repo

- [ ] **Task 5.1** — `feat(ingestion): add EventSourceAdapter target interface`
  `adapter/in/ingestion/adapter/`.
- [ ] **Task 5.2** — `feat(ingestion): add camera, audio and weight adapters`
  `CameraBehaviorAdapter`, `AudioClassificationAdapter`, `WeightReadingAdapter` → `HealthEvent`.
- [ ] **Task 5.3** — `feat(ingestion): add AdapterFactory and concrete factories`
  `adapter/in/ingestion/factory/`.
- [ ] **Task 5.4** — `feat(domain): add ProcessEventUseCase input port` *(👤 los dos: contrato entre las dos mitades)*
- [ ] **Task 5.5** — `feat(web): wire IngestionController to AdapterFactory and ProcessEventUseCase`
- [ ] **Task 5.6** — `test(ingestion): cover factory selection and each adapter`

### Task 6 — Núcleo de salud 👤 Josuram

🔗 **Depende de:** seguir con las Task 4.2–4.3 de este repo

- [ ] **Task 6.1** — `feat(chain): add EventHandler and the four handlers`
  `domain/health/chain/`: `ValidationHandler`, `IdentificationHandler`, `BehaviorThresholdHandler`, `SustainedAnomalyHandler` (lógica simple por ahora; reciben los puertos que necesitan por constructor).
- [ ] **Task 6.2** — `feat(chain): add HandlerChainBuilder`
- [ ] **Task 6.3** — `feat(state): add HealthState and the four states`
- [ ] **Task 6.4** — `feat(state): add GuineaPigHealthContext`
- [ ] **Task 6.5** — `feat(composite): add HealthComponent, leaves and CageHealth`
- [ ] **Task 6.6** — `feat(application): add EventProcessingService implementing ProcessEventUseCase`
  Orquesta cadena → estado → composite → `AlertPublisher`.
  🔗 Depende de: Task 5.4
- [ ] **Task 6.7** — `feat(config): add DomainConfig wiring the chain and domain beans`
- [ ] **Task 6.8** — `test(health): cover each handler, each transition and cage aggregation with plain JUnit`
  Con fakes en memoria de los puertos; sin Spring ni base de datos.

### Task 7 — Observer + WebSocket 👤 Compañero

- [ ] **Task 7.1** — `feat(observer): add AlertObserver output port and AlertPublisher`
  `AlertObserver` en `domain/port/out/`, `AlertPublisher` en `domain/notification/`.
- [ ] **Task 7.2** — `feat(observer): add DatabaseAlertObserver and LogAlertObserver`
  En `adapter/out/notification/`; `DatabaseAlertObserver` guarda usando el puerto `AlertRepository`.
- [ ] **Task 7.3** — `feat(websocket): add STOMP config on /ws`
- [ ] **Task 7.4** — `feat(observer): add WebSocketAlertObserver publishing to /topic/cages/{id}`
- [ ] **Task 7.5** — `feat(state): publish alerts on transitions to ALERT and CRITICAL` *(👤 Josuram, llama a `AlertPublisher`)*

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 6

### Task 8 — API para el dashboard 👤 Josuram

Cada endpoint = puerto de entrada en `domain/port/in` + servicio en `application/` + controller en `adapter/in/web/`.

- [ ] **Task 8.1** — `feat(config): add CORS for the Amplify domain`
- [ ] **Task 8.2** — `feat(web): add cage health and guinea pigs list endpoints`
  `GetCageHealthUseCase`, `ListGuineaPigsUseCase` → `GET /api/cages/{id}/health`, `GET /api/cages/{id}/guinea-pigs`.
- [ ] **Task 8.3** — `feat(web): add endpoint to register a guinea pig`
  `RegisterGuineaPigUseCase` → `POST /api/cages/{id}/guinea-pigs`.
- [ ] **Task 8.4** — `feat(web): add alerts list endpoint`
  `ListAlertsUseCase` → `GET /api/alerts?status=`.
- [ ] **Task 8.5** — *(sin commit)* desplegar y probar cada endpoint en Postman

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 5

### Task 9 — Datos falsos para el avance 👤 Compañero

- [ ] **Task 9.1** — `feat(dev): add fake producer that posts random events`
  `dev/fake-producer/` manda eventos `BEHAVIOR`, `AUDIO` y `WEIGHT` a `/api/ingestion/events`.
- [ ] **Task 9.2** — *(sin commit)* dejarlo corriendo contra el servidor y ver cambios de estado en el dashboard

---

## 🟠 Prioridad 2 — Entrega final (octubre)

### Task 10 — Reglas reales de State 👤 Josuram

- [ ] **Task 10.1** — `feat(state): add escalation rules between states`
- [ ] **Task 10.2** — `feat(state): add recovery back to NORMAL`
- [ ] **Task 10.3** — `feat(state): persist every transition through StateTransitionRepository`
- [ ] **Task 10.4** — `test(state): cover escalation and recovery paths`

### Task 11 — Perfil normal y umbrales 👤 Josuram

- [ ] **Task 11.1** — `feat(domain): compute BaselineProfile per guinea pig`
- [ ] **Task 11.2** — `feat(chain): compare windows against the baseline in BehaviorThresholdHandler`
- [ ] **Task 11.3** — `feat(chain): require N consecutive anomalous windows in SustainedAnomalyHandler`
- [ ] **Task 11.4** — `feat(config): make thresholds and N configurable in application.yml`
  Los valores entran al dominio desde `DomainConfig` (el dominio no lee `application.yml` directamente).

### Task 12 — Audio y peso en el Composite 👤 Josuram

🔗 **Depende de:** `cuy-monitor-ai-service` Task 11 (audio real) y `cuy-monitor-arduino` Task 5 (peso real) para probar con datos reales

- [ ] **Task 12.1** — `feat(composite): evaluate cage audio health from DISTRESS events`
- [ ] **Task 12.2** — `feat(composite): evaluate cage weight trend from stable readings`
- [ ] **Task 12.3** — `feat(application): store WEIGHT events through WeightReadingRepository`

### Task 13 — API de historial y alertas 👤 Josuram

- [ ] **Task 13.1** — `feat(web): add guinea pig history endpoint by date range`
  `GetGuineaPigHistoryUseCase` → `GET /api/guinea-pigs/{id}/history`.
- [ ] **Task 13.2** — `feat(web): add cage weight history endpoint by date range`
  `GetWeightHistoryUseCase` → `GET /api/cages/{id}/weight`.
- [ ] **Task 13.3** — `feat(web): add endpoint to mark an alert as reviewed`
  `ReviewAlertUseCase` → `PATCH /api/alerts/{id}`.

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 8, 9 y 10

### Task 14 — Pruebas de arquitectura e integración 👤 Los dos

- [ ] **Task 14.1** — `test(architecture): enforce hexagonal dependency rules with ArchUnit`
  Agrega `com.tngtech.archunit:archunit-junit5` (scope test) y las reglas de la sección 3.4 de `ARCHITECTURE.md`. *Conviene hacerla apenas termine la Task 2.2.*
- [ ] **Task 14.2** — `test(integration): add Testcontainers Postgres setup`
- [ ] **Task 14.3** — `test(integration): cover event → state change → alert end to end`
- [ ] **Task 14.4** — `test(web): cover controllers responses with WebMvcTest and fake use cases`

---

## 🟢 Prioridad 3 — Cierre (noviembre)

### Task 15 — Ajuste con datos reales 👤 Los dos

🔗 **Depende de:** `cuy-monitor-ai-service` Task 13 y `cuy-monitor-arduino` Task 7 (sistema montado en la jaula)

- [ ] **Task 15.1** — `chore(config): tune thresholds after the farm test`
- [ ] **Task 15.2** — `docs: record false positives and negatives from the farm test`

### Task 16 — Informe y sustentación 👤 Los dos

- [ ] **Task 16.1** — `docs(diagrams): add hexagon diagram and UML class diagram per pattern`
- [ ] **Task 16.2** — `docs(adr): add ADR files 001 to 007`
- [ ] **Task 16.3** — `docs: update README with setup, deploy and screenshots`

### Task 17 — Limpieza e infraestructura 👤 Josuram

- [ ] **Task 17.1** — `refactor(web): remove SystemController smoke endpoint`
- [ ] **Task 17.2** — *(sin commit)* medir RAM con el ai-service y decidir si subir la EC2 a c7i-flex.large
- [ ] **Task 17.3** — `ci: add GitHub Actions build and test on pull requests` *(opcional)*

---

## 🔐 Autenticación y cuentas de usuario

Un solo tipo de usuario (básico, sin roles ni admin). Cualquiera puede registrarse desde la pantalla de login ("¿No tienes cuenta?"). Cada usuario solo ve y modifica **su propia cuenta** (`/api/users/me`); no hay listado de usuarios.

Se divide en dos tareas, cada una con su rama y su PR: la Task 19 (CRUD) necesita el modelo `User`, la seguridad y el JWT de la Task 18, así que su rama sale de `main` cuando la 18 ya esté mergeada.

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

### Task 18 — Registro y login con JWT + OTP por correo 👤 Juan

Rama: `feature/task-18-auth-jwt-otp`.

**Flujo**

1. **Registro**: `POST /api/auth/register` `{ username, fullName, email, password }` → `201 { challengeId, expiresAt }`. El usuario queda en `PENDING_VERIFICATION` y se le envía un código de 6 dígitos al correo.
2. **Login**: `POST /api/auth/login` `{ username, password }` → `200 { challengeId, expiresAt }` y se envía un código nuevo al correo. También sirve para reenviar el código a una cuenta que aún no se verificó.
3. **Verificar OTP**: `POST /api/auth/otp/verify` `{ challengeId, code }` → `200 { accessToken, tokenType: "Bearer", expiresAt }`. Si la cuenta estaba `PENDING_VERIFICATION`, pasa a `ACTIVE` (el código prueba que el correo es suyo).
4. El resto de `/api/**` pide `Authorization: Bearer <jwt>`. `/api/ingestion/**` sigue con `X-API-Key`; `/api/auth/**` y `/actuator/health` son públicos.

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
  ⚠️ Usa `V2`; la Task 4.4 tendrá que pasar a `V3` si se mergea después.
- [x] **Task 18.10** — `feat(persistence): add user and otp challenge JPA adapters`
  Entidades, Spring Data, mappers y adaptadores que implementan los puertos.
- [x] **Task 18.11** — `feat(security): add BCrypt password hasher and JWT token issuer`
  `adapter/out/security/`.
- [x] **Task 18.12** — `test(security): cover password hasher and JWT issuer`
  Hash/verify, y que el JWT emitido se valida con el decoder y trae `sub`, `iss` y `exp`.
- [x] **Task 18.13** — `feat(mail): add email and log OTP senders`
  `EmailOtpSender` con `JavaMailSender`; `LogOtpSender` para el perfil `dev` (escribe el código en el log).
- [x] **Task 18.14** — `feat(config): add security filter chain and auth wiring`
  `SecurityConfig`, `JwtConfig`, `AuthProperties` (`app.auth.*`), `AuthConfig` (arma el servicio).
- [x] **Task 18.15** — `feat(web): add AuthController with register, login and OTP endpoints`
  DTOs `record` con `@Valid`; `AuthExceptionHandler` → `400` / `401` / `409`.
- [x] **Task 18.16** — `test(web): cover auth endpoints and protected routes`
  `@WebMvcTest` con casos de uso falsos + prueba de que una ruta protegida da `401` sin token, `200` con token, y que la ingesta sigue entrando con `X-API-Key`.
- [x] **Task 18.17** — `chore(infra): add JWT and SMTP env vars`
  `application.yml` (solo referencias `${...}`), `infra/.env.example`, `infra/docker-compose.yml`.
- [x] **Task 18.18** — `docs(contracts): add auth API contract`
  `docs/contracts/auth-api.md`. ⚠️ Avisar a `cuy-monitor-dashboard`: necesita pantallas de registro y login + OTP, mandar el Bearer y borrar el token al cerrar sesión.
- [x] **Task 18.19** — *(sin commit)* `./mvnw test` en verde, prueba manual en Postman y PR a `main`

🔓 **Desbloquea:** registro y login en `cuy-monitor-dashboard` · Task 19

### Task 19 — CRUD de la cuenta del usuario 👤 Juan

Rama: `feature/task-19-user-account-crud`, creada desde `main` después de mergear la Task 18.

**Endpoints** (todos con `Authorization: Bearer <jwt>`; el id sale del `sub` del token, nunca de la URL)

- `GET /api/users/me` → datos de la cuenta (nunca el hash).
- `PUT /api/users/me` `{ fullName }` → actualiza el perfil.
- `PUT /api/users/me/password` `{ currentPassword, newPassword }` → cambia la contraseña.
- `DELETE /api/users/me` `{ currentPassword }` → **desactiva** la cuenta (`DISABLED`, soft delete). Ya no puede iniciar sesión.

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
- [ ] **Task 19.7** — `test(web): cover user account endpoints`
  `401` sin token, `200` con token, `401` con contraseña actual incorrecta, `204` al desactivar.
- [ ] **Task 19.8** — `docs(contracts): add user account endpoints to auth API contract`
  ⚠️ Avisar a `cuy-monitor-dashboard`: pantalla "mi cuenta".
- [ ] **Task 19.9** — *(sin commit)* `./mvnw test` en verde, prueba manual en Postman y PR a `main`

🔓 **Desbloquea:** pantalla "mi cuenta" en `cuy-monitor-dashboard`
