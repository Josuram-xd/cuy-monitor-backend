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

### Task 2 — Estructura hexagonal e ingesta por HTTP 👤 Josuram

- [x] **Task 2.1** — `refactor(ingestion): remove Kafka classes and dependencies`
  Borrar `KafkaConfigTopics.java`, `ingestion/kafka/`, dependencias con `kafka` en el `pom.xml`, sección `spring.kafka` del `application.yml`.
- [x] **Task 2.2** — `refactor: move to hexagonal package layout`
  Crear `domain/{model,health,notification,port/in,port/out}`, `application/`, `adapter/in/{web,ingestion}`, `adapter/out/{persistence,notification}`, `config/`. Mover `SystemController` a `adapter/in/web/`. Un `package-info.java` por paquete principal explicando qué va ahí.
- [x] **Task 2.3** — `feat(domain): add EventType enum and IngestionEvent envelope`
  `EventType` en `domain/model/`; `IngestionEvent` en `adapter/in/ingestion/dto/`.
- [x] **Task 2.4** — `feat(web): add IngestionController with API key check`
  `adapter/in/web/IngestionController`: `POST /api/ingestion/events` → `202` / `400` / `401`. Por ahora solo registra en el log.
- [x] **Task 2.5** — `chore(infra): remove broker service from compose files`
- [ ] **Task 2.6** — *(sin commit)* desplegar con `docker compose up -d --build --remove-orphans` y probar el `POST` desde Postman

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
