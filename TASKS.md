# TASKS — cuy-monitor-backend

> Lista de trabajo del backend. Cada subtarea = **un commit**: usa el mensaje que está entre comillas invertidas.
> Marca `[x]` cuando hagas push. Una rama por Task: `feature/task-2-http-ingestion`, `feature/task-6-health-core`, etc.

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
  Migración Flyway `V1__initial_schema.sql` + jaula de ejemplo `cage-1`.
- [x] **Task 1.3** — `build: add multi-stage Dockerfile and dockerignore`
- [x] **Task 1.4** — `chore(infra): add docker compose, Caddyfile and env example`
- [x] **Task 1.5** — *(sin commit)* EC2 t3.small + Elastic IP + DuckDNS + `docker compose up`
  Verificado: `/actuator/health` responde `UP` por HTTPS.

### Task 2 — Ingesta por HTTP 

- [x] **Task 2.1** — `refactor(ingestion): remove Kafka classes and dependencies`
  Borrar `KafkaConfigTopics.java`, `ingestion/kafka/`, dependencias con `kafka` en el `pom.xml`, sección `spring.kafka` del `application.yml`.
- [x] **Task 2.2** — `feat(domain): add EventType enum and IngestionEvent envelope`
- [x] **Task 2.3** — `feat(api): add IngestionController with API key check`
  `POST /api/ingestion/events` → `202` / `400` / `401`. Por ahora solo registra en el log.
- [x] **Task 2.4** — `chore(infra): remove broker service from compose files`
- [ ] **Task 2.5** — *(sin commit)* desplegar con `docker compose up -d --build --remove-orphans` y probar el `POST` desde Postman

🔓 **Desbloquea:** `cuy-monitor-ai-service` Task 3 · `cuy-monitor-arduino` Task 5

### Task 3 — Contratos y documentación 

- [x] **Task 3.1** — `docs: add PRD, ARCHITECTURE and AGENTS`
- [ ] **Task 3.2** — `docs(contracts): add events envelope and payloads`
  `docs/contracts/events.md`: sobre común, `BEHAVIOR`, `AUDIO`, `WEIGHT`, enums compartidos.
- [ ] **Task 3.3** — `docs(contracts): add REST API and WebSocket messages`
  `docs/contracts/rest-api.md`: endpoints del dashboard, ingesta y formato de los mensajes STOMP.
- [ ] **Task 3.4** — `docs(contracts): add example JSON per event type`
  `docs/contracts/examples/behavior.json`, `audio.json`, `weight.json`.

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 2 · `cuy-monitor-ai-service` Task 2 · `cuy-monitor-arduino` Task 5

### Task 4 — Modelo de dominio 

- [ ] **Task 4.1** — `feat(domain): add MarkColor, HealthStatus and AlertStatus enums`
- [ ] **Task 4.2** — `feat(domain): add Cage and GuineaPig entities with repositories`
- [ ] **Task 4.3** — `feat(db): add V2 migration for event, state_transition, alert, weight_reading and baseline_profile`
- [ ] **Task 4.4** — `feat(domain): add entities and repositories for V2 tables`
- [ ] **Task 4.5** — `feat(domain): add HealthEvent record` *(👤 los dos: es la frontera entre las dos mitades)*

### Task 5 — Factory Method + Adapter 

🔗 **Depende de:** seguir con las Task 2.2–2.3 y 4.5 de este repo

- [ ] **Task 5.1** — `feat(adapter): add EventSourceAdapter target interface`
- [ ] **Task 5.2** — `feat(adapter): add camera, audio and weight adapters`
  `CameraBehaviorAdapter`, `AudioClassificationAdapter`, `WeightReadingAdapter` → `HealthEvent`.
- [ ] **Task 5.3** — `feat(factory): add AdapterFactory and concrete factories`
- [ ] **Task 5.4** — `feat(ingestion): wire IngestionController to AdapterFactory`
- [ ] **Task 5.5** — `feat(ingestion): store raw events and ignore duplicate eventId`
- [ ] **Task 5.6** — `test(ingestion): cover factory selection and each adapter`

### Task 6 — Núcleo de salud (esqueleto) 

🔗 **Depende de:** seguir con la Task 4 de este repo

- [ ] **Task 6.1** — `feat(chain): add EventHandler and the four handlers`
  `ValidationHandler`, `IdentificationHandler`, `BehaviorThresholdHandler`, `SustainedAnomalyHandler` (lógica simple por ahora).
- [ ] **Task 6.2** — `feat(chain): add HandlerChainBuilder`
- [ ] **Task 6.3** — `feat(state): add HealthState and the four states`
- [ ] **Task 6.4** — `feat(state): add GuineaPigHealthContext`
- [ ] **Task 6.5** — `feat(composite): add HealthComponent, leaves and CageHealth`
- [ ] **Task 6.6** — `feat(ingestion): connect adapters output to the handler chain`
  🔗 Depende de: Task 5.4
- [ ] **Task 6.7** — `test(health): cover each handler, each transition and cage aggregation`

### Task 7 — Observer + WebSocket 

- [ ] **Task 7.1** — `feat(observer): add AlertObserver and AlertPublisher`
- [ ] **Task 7.2** — `feat(observer): add DatabaseAlertObserver and LogAlertObserver`
- [ ] **Task 7.3** — `feat(websocket): add STOMP config on /ws`
- [ ] **Task 7.4** — `feat(observer): add WebSocketAlertObserver publishing to /topic/cages/{id}`
- [ ] **Task 7.5** — `feat(state): publish alerts on transitions to ALERT and CRITICAL` *(👤 Josuram, llama a `AlertPublisher`)*

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 6

### Task 8 — API para el dashboard 

- [ ] **Task 8.1** — `feat(config): add CORS for the Amplify domain`
- [ ] **Task 8.2** — `feat(api): add GET cage health and guinea pigs list`
  `GET /api/cages/{id}/health`, `GET /api/cages/{id}/guinea-pigs`.
- [ ] **Task 8.3** — `feat(api): add POST to register a guinea pig`
- [ ] **Task 8.4** — `feat(api): add GET alerts by status`
- [ ] **Task 8.5** — *(sin commit)* desplegar y probar cada endpoint en Postman

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 5

### Task 9 — Datos falsos para el avance 

- [ ] **Task 9.1** — `feat(dev): add fake producer that posts random events`
  `dev/fake-producer/` manda eventos `BEHAVIOR`, `AUDIO` y `WEIGHT` a `/api/ingestion/events`.
- [ ] **Task 9.2** — *(sin commit)* dejarlo corriendo contra el servidor y ver cambios de estado en el dashboard

---

## 🟠 Prioridad 2 — Entrega final (octubre)

### Task 10 — Reglas reales de State 

- [ ] **Task 10.1** — `feat(state): add escalation rules between states`
- [ ] **Task 10.2** — `feat(state): add recovery back to NORMAL`
- [ ] **Task 10.3** — `feat(state): persist every transition in state_transition`
- [ ] **Task 10.4** — `test(state): cover escalation and recovery paths`

### Task 11 — Perfil normal y umbrales 

- [ ] **Task 11.1** — `feat(baseline): compute BaselineProfile per guinea pig`
- [ ] **Task 11.2** — `feat(chain): compare windows against the baseline in BehaviorThresholdHandler`
- [ ] **Task 11.3** — `feat(chain): require N consecutive anomalous windows in SustainedAnomalyHandler`
- [ ] **Task 11.4** — `feat(config): make thresholds and N configurable in application.yml`

### Task 12 — Audio y peso en el Composite 

🔗 **Depende de:** `cuy-monitor-ai-service` Task 11 (audio real) y `cuy-monitor-arduino` Task 5 (peso real) para probar con datos reales

- [ ] **Task 12.1** — `feat(composite): evaluate cage audio health from DISTRESS events`
- [ ] **Task 12.2** — `feat(composite): evaluate cage weight trend from stable readings`
- [ ] **Task 12.3** — `feat(weight): store WEIGHT events in weight_reading`

### Task 13 — API de historial y alertas 

- [ ] **Task 13.1** — `feat(api): add guinea pig history by date range`
- [ ] **Task 13.2** — `feat(api): add cage weight history by date range`
- [ ] **Task 13.3** — `feat(api): add PATCH to mark an alert as reviewed`

🔓 **Desbloquea:** `cuy-monitor-dashboard` Task 8, 9 y 10

### Task 14 — Pruebas de integración 

- [ ] **Task 14.1** — `test(integration): add Testcontainers Postgres setup`
- [ ] **Task 14.2** — `test(integration): cover event → state change → alert end to end`
- [ ] **Task 14.3** — `test(api): cover IngestionController responses with WebMvcTest`

---

## 🟢 Prioridad 3 — Cierre (noviembre)

### Task 15 — Ajuste con datos reales 

🔗 **Depende de:** `cuy-monitor-ai-service` Task 13 y `cuy-monitor-arduino` Task 7 (sistema montado en la jaula)

- [ ] **Task 15.1** — `chore(config): tune thresholds after the farm test`
- [ ] **Task 15.2** — `docs: record false positives and negatives from the farm test`

### Task 16 — Informe y sustentación 

- [ ] **Task 16.1** — `docs(diagrams): add UML class diagram per pattern`
- [ ] **Task 16.2** — `docs(adr): add ADR files 001 to 006`
- [ ] **Task 16.3** — `docs: update README with setup, deploy and screenshots`

### Task 17 — Limpieza e infraestructura 

- [ ] **Task 17.1** — `refactor(api): remove SystemController smoke endpoint`
- [ ] **Task 17.2** — *(sin commit)* medir RAM con el ai-service y decidir si subir la EC2 a c7i-flex.large
- [ ] **Task 17.3** — `ci: add GitHub Actions build and test on pull requests` *(opcional)*
