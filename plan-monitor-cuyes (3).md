# Monitor de Salud de Cuyes — Plan de trabajo, arquitectura y repos

> Documento informal del equipo. Aquí está todo: qué hace cada pieza, en qué repo vive, qué necesita de los otros repos, quién hace qué y en qué orden.
> **Versión 2** — 24 de septiembre de 2026. Cambios respecto a la v1: versiones actualizadas (sección 3) y todo el código, carpetas, clases, tópicos, endpoints y JSON en inglés (sección 4).

---

## 0. Resumen en 30 segundos

- **4 repos:** `cuy-monitor-backend` (Java, el corazón), `cuy-monitor-ai-service` (Python, la IA), `cuy-monitor-dashboard` (React + TS), `cuy-monitor-arduino` (firmware del sensor de peso + el puente serial).
- **Flujo:** el celular A12 graba la jaula → la laptop vieja toma frames/audio y los manda a la IA en AWS → la IA publica eventos en **Kafka** → el backend Java los consume, los pasa por los **6 patrones** y guarda todo en **Postgres** → el dashboard muestra el estado de cada cuy y las alertas en tiempo real.
- **Peso:** Arduino + HX711 → USB a la laptop → la laptop se lo manda al backend → el backend lo publica en Kafka.
- **Nube:** una sola EC2 en AWS con Docker Compose (backend + IA + Kafka + Postgres + Caddy para HTTPS). El dashboard va en AWS Amplify.
- **Stack (sept 2026):** Java 25 LTS + Spring Boot 4.1 · Kafka 4.3 · PostgreSQL 18 · Python 3.14 + FastAPI + YOLO26n (ONNX) · React 19.3 + TypeScript 6.0 + Vite 8 · Node 24 LTS · Ubuntu 26.04 LTS.
- **Idioma:** código, carpetas, clases, variables, tópicos, endpoints, JSON, commits y README → **inglés**. Textos que ve el criador en el dashboard → **español** (con i18n, así cambiar a inglés es un archivo).
- **Reparto:** tú te quedas con el **núcleo de salud** del backend (State, Chain, Composite), el dashboard y el despliegue. Tu compañero con la **entrada de datos** del backend (Factory Method, Adapter, Observer), el Arduino y la IA. El contrato de eventos y el Docker Compose los hacen juntos.

---

## 1. Requisitos (lo que tiene que cumplir)

### Funcionales

1. Identificar a cada cuy por el color que tiene marcado en el lomo y seguirlo por cámara.
2. Calcular el comportamiento de cada cuy (tiempo quieto, visitas al comedero/bebedero, qué tan lejos está del grupo) y detectar cuando cambia de forma **sostenida**.
3. Clasificar el audio de la jaula (normal vs. chillido de angustia) y generar una alerta **de jaula**.
4. Registrar el peso (Arduino) y usarlo como una señal más de salud.
5. Llevar un estado de salud por cuy: `NORMAL → OBSERVED → ALERT → CRITICAL` (y que pueda volver hacia atrás).
6. Dashboard con el estado de cada cuy, el resumen de la jaula, el historial y las alertas.

### No funcionales

| Qué | Meta realista |
|---|---|
| Cuyes por jaula | 5 a 8 |
| Jaulas | 1 (piloto), pero que se pueda escalar sin reescribir |
| Frecuencia de frames | 1–2 por segundo (los cuyes no son tan rápidos) |
| Latencia de una alerta | menos de 1 minuto (la alerta es por comportamiento sostenido de varios minutos) |
| Disponibilidad | "que siga corriendo": reinicio automático si algo se cae |
| Costo | $0 en nube (créditos AWS); solo se compra el kit del Arduino |

### Restricciones

- Backend **obligatoriamente en Java** (ahí viven los patrones).
- Frontend **obligatoriamente en TypeScript** (con React).
- **Kafka con Docker en AWS** (requisito del curso).
- 2 personas, avance desplegado el **30 de septiembre** y entrega final en **noviembre**.
- Hardware: celular A12, laptop Celeron 8 GB, Arduino + HX711 + celda de carga.

---

## 2. Arquitectura general

```
        GUINEA PIG CAGE (farm)                                    AWS — EC2 (Docker Compose)
┌─────────────────────────────────────┐              ┌──────────────────────────────────────────────────┐
│                                     │              │                                                  │
│  Samsung A12 (IP Webcam app)        │              │   Caddy  (HTTPS :443, free certificate)          │
│   video + audio over WiFi           │              │     ├── /api/**, /ws/** ──► backend  (Java :8080)│
│            │                        │              │     └── /ai/**          ──► ai-service (Py :8000)│
│            ▼                        │   HTTPS      │                                                  │
│  Celeron laptop                     │   + API key  │   ai-service ──produce──► ┌────────────────────┐ │
│   ├─ edge_agent (Python) ───────────┼─────────────►│                           │  Kafka 4.3 (KRaft) │ │
│   │   frames 1-2 fps + 1 s audio    │   /ai/...    │                           │  events.camera     │ │
│   │                                 │              │   backend ──produce─────► │  events.audio      │ │
│   └─ serial_bridge (Python) ────────┼─────────────►│   (from /api/ingestion)   │  events.weight     │ │
│          ▲                          │   /api/...   │                           │  alerts.cage       │ │
│          │ USB serial               │              │   backend ◄──consume───── └────────────────────┘ │
│  Arduino Uno + HX711 + load cell    │              │      │                                           │
│                                     │              │      └──► PostgreSQL 18 (history, states, alerts)│
└─────────────────────────────────────┘              └──────────────────────────────────────────────────┘
                                                                  ▲
                                                                  │ REST + WebSocket (HTTPS)
                                                      ┌───────────┴───────────┐
                                                      │ AWS Amplify           │
                                                      │ dashboard React + TS  │
                                                      └───────────────────────┘
```

### Cómo viaja un dato (ejemplo: un cuy se queda quieto mucho rato)

1. **edge_agent** (laptop) saca un frame del stream del A12 y lo manda a `POST /ai/frames`.
2. **ai-service** detecta los cuyes por color (YOLO26n en ONNX), mantiene la identidad con el tracker (algoritmo húngaro), y cada ventana de 60 s calcula las variables de comportamiento de cada cuy y las pasa por el Random Forest.
3. **ai-service** publica en el tópico `events.camera` un evento con el resultado por cuy.
4. **backend** lo consume. El **Factory Method** (`AdapterFactory`) elige el adaptador según el tipo de evento y el **Adapter** lo convierte al `HealthEvent` interno.
5. El evento entra a la **Chain of Responsibility**: validación → asociar el color al cuy registrado → evaluar umbrales → confirmar que la anomalía es sostenida.
6. Si se confirma, el **State** del cuy pasa de `NORMAL` a `OBSERVED` (o de `OBSERVED` a `ALERT`, etc.).
7. El **Composite** (`CageHealth`) recalcula la salud de la jaula (cuyes + audio + peso).
8. El **Observer** (`AlertPublisher`) avisa a sus suscriptores: el que empuja al dashboard por WebSocket, el que guarda la alerta en Postgres y el que la publica en `alerts.cage`.
9. El **dashboard** recibe el mensaje por WebSocket y pinta al cuy en amarillo/rojo.

### Ojo: cómo se reparte la "cadena" original

En la propuesta la cadena era *detección → tracking → clasificación → persistencia*. Como la detección y el tracking corren en Python (ai-service), en Java la **Chain of Responsibility** queda con los eslabones que vienen después:

| Eslabón (Java) | Qué hace |
|---|---|
| `ValidationHandler` | Descarta eventos mal formados, viejos o con confianza muy baja |
| `IdentificationHandler` | Traduce "color RED en la jaula 1" al cuy registrado (Bolita, Canela…) |
| `BehaviorThresholdHandler` | Decide si el comportamiento está fuera de su perfil normal |
| `SustainedAnomalyHandler` | Solo deja pasar si la anomalía se repite N ventanas seguidas (evita falsas alarmas por un cuy que se rasca) |

En el informe hay que explicarlo así: la detección y el tracking son IA en Python; la decisión de salud es patrón en Java.

---

## 3. Stack y versiones (revisado el 24 de septiembre de 2026)

Revisé cada pieza contra su página oficial: cuál es la última versión, si sigue con soporte y si conviene usarla o no.

### 3.1 Tabla de versiones

| Pieza | Plan anterior | **Versión a usar** | ¿Es la última? | Soporte | Por qué |
|---|---|---|---|---|---|
| **Java** | 17 | **25 LTS** (Eclipse Temurin) | La última es 26, pero no es LTS | 25: soporte premier hasta sept 2030 | ⚠️ Java 17 **termina su soporte premier este mes (sept 2026)**. Empezar un proyecto nuevo en 17 no tiene sentido. La siguiente LTS será 29 (sept 2027) |
| **Spring Boot** | 3.x | **4.1.x** | Sí (4.1, junio 2026) | Hasta ~julio 2027 | ⚠️ 3.5 llegó a fin de vida en junio 2026 y 4.0 termina en dic 2026. Boot 4.1 soporta Java 17 a 26 |
| Spring for Apache Kafka, Flyway, Hibernate, Testcontainers | — | Las que trae Spring Boot 4.1 | — | — | No se ponen versiones a mano: el BOM de Spring Boot las maneja |
| **Apache Kafka** | "3.x/4.x" | **4.3.1** (imagen `apache/kafka:4.3.1`) | Sí (junio 2026) | Activa | Desde 4.0 solo existe KRaft, ya no hay ZooKeeper |
| **PostgreSQL** | 16 | **18** (imagen `postgres:18`) | Sí, la estable (18.6) | Hasta 2030 | 19 todavía está en beta; no usar betas en el proyecto |
| **Python** (ai-service, edge, bridge) | 3.11 | **3.14** | Sí (3.15 sale en octubre) | Bugfix hasta 2030 | 3.11 ya solo recibe parches de seguridad. FastAPI, onnxruntime, scikit-learn y aiokafka ya soportan 3.14 |
| **TensorFlow** (para YAMNet) | TF completo | **No va en el servicio** | 2.21 | — | ⚠️ TensorFlow **no soporta oficialmente Python 3.14** (llega hasta 3.13). Solución: YAMNet se convierte a ONNX una sola vez en Colab y el servicio lo corre con onnxruntime. Además la imagen Docker queda mucho más liviana. Plan B: si la conversión da guerra, el ai-service usa Python 3.13 + TF 2.21 |
| **Detector** | YOLOv8n | **YOLO26n** (Ultralytics) | Sí (enero 2026) | Activo | Hasta 43% más rápido en CPU con ONNX que YOLO11n, y no necesita NMS. Justo lo que sirve para el Celeron o una EC2 sin GPU |
| **ONNX Runtime** | — | **1.26** | Sí | Activo | Tiene wheels para Python 3.14 |
| **FastAPI** | — | **0.141** | Sí | Activo | Soporta 3.10–3.14 |
| **aiokafka** | — | **0.14** | Sí | Activo | Soporta 3.10–3.14 |
| **scikit-learn** | — | **1.9** | Sí (sept 2026) | Activo | Random Forest, SVM, KNN |
| **React** | 18/19 | **19.3** | Sí (9 sept 2026) | Activo | React 20 no existe todavía |
| **TypeScript** | — | **6.0** | La última es 7.0.2, pero… | Activo | ⚠️ TS 7 (el compilador reescrito en Go) salió en agosto 2026 **sin API para herramientas**: typescript-eslint todavía no funciona con él. Microsoft recomienda pasar por 6.0 primero. Migrar a 7.1 cuando salga |
| **Vite** | — | **8.x** | Sí (8.3) | Activo | Build con Rolldown, más rápido |
| **Node.js** (para el front) | — | **24 LTS** | 26 pasa a LTS en octubre 2026 | 24: seguridad hasta abril 2028 | 24 es la LTS estable hoy; cuando 26 sea LTS (fin de octubre) se puede subir sin drama |
| React Router | — | **8.x** | Sí | Activo | — |
| TanStack Query | — | **5.x** | Sí | Activo | Cache y llamadas a la API |
| Recharts | — | **3.x** | Sí | Activo | Gráficas |
| @stomp/stompjs | — | **7.x** | Sí | Activo | WebSocket con STOMP (Spring lo habla nativo) |
| i18next + react-i18next | — | **26.x** | Sí | Activo | Textos en español separados del código |
| **Caddy** | 2 | **2.11** | Sí (junio 2026) | Activo | HTTPS automático |
| **SO de la EC2** | Ubuntu 24.04 | **Ubuntu 26.04 LTS** | Sí (abril 2026) | Hasta 2031 | La LTS más nueva |
| Arduino | IDE 2 | **Arduino IDE 2.x** + librería HX711 (bogde) | — | — | Sin cambios |

### 3.2 Cambios de Spring Boot 4 que hay que tener en cuenta

Al crear el proyecto en [start.spring.io](https://start.spring.io) con **Spring Boot 4.1 + Java 25 + Maven**:

- El starter web ahora se llama **`spring-boot-starter-webmvc`** (antes `spring-boot-starter-web`).
- Flyway necesita **`spring-boot-starter-flyway`** + `flyway-database-postgresql` (antes bastaba `flyway-core`).
- Jackson pasó a la versión 3: el paquete ahora es `tools.jackson` (antes `com.fasterxml.jackson`). Si copian ejemplos viejos de internet, ese import va a fallar.
- Los tests usan starters por tecnología (`spring-boot-starter-webmvc-test`, `spring-boot-starter-kafka-test`, etc.).

**Dependencias a marcar en Initializr:** Spring Web MVC, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Validation, WebSocket, Spring for Apache Kafka, Spring Boot Actuator, Lombok, Testcontainers.

### 3.3 Qué NO cambió (y por qué)

- **Kafka sigue**, en modo KRaft (ahora es el único modo).
- **Postgres sigue**, solo sube a 18.
- **Random Forest, SVM/KNN y YAMNet siguen**: son la opción correcta con pocos datos propios; lo único que cambia es cómo se ejecuta YAMNet (ONNX en vez de TensorFlow).

---

## 4. Convenciones de idioma y nombres

### 4.1 Qué va en inglés y qué en español

| Cosa | Idioma | Ejemplo |
|---|---|---|
| Carpetas y archivos | Inglés | `health/state/AlertState.java`, `edge_agent/agent.py` |
| Clases, métodos, variables | Inglés | `GuineaPig`, `evaluateThreshold()`, `stillSeconds` |
| Tablas y columnas de BD | Inglés, `snake_case` | `guinea_pig`, `mark_color` |
| Tópicos de Kafka | Inglés | `events.camera`, `alerts.cage` |
| Endpoints REST | Inglés, `kebab-case`, plural | `/api/guinea-pigs/{id}/history` |
| Campos JSON | Inglés, `camelCase` | `probAnomaly`, `cageId` |
| Enums | Inglés, `UPPER_SNAKE_CASE` | `NORMAL`, `OBSERVED`, `RED` |
| Comentarios en el código | Inglés | `// Only pass events seen in N consecutive windows` |
| Commits y ramas | Inglés | `feat(state): add OBSERVED to ALERT transition`, `feature/state-transitions` |
| README, contratos, ADRs dentro de los repos | Inglés | `docs/contracts/events.md` |
| **Textos que ve el criador en el dashboard** | **Español** (archivo `es.json`) | "Cuy en observación", "Revisar alerta" |
| Este documento y el informe del curso | Español | — |

**Sobre el front:** los componentes, props, hooks y tipos van en inglés como todo el código. Lo que se muestra en pantalla va en `src/i18n/locales/es.json` y nunca escrito directo en el JSX. Si el docente o ustedes quieren la interfaz en inglés, se agrega `en.json` y se cambia el idioma por defecto, sin tocar componentes.

### 4.2 Glosario (para que los dos usen los mismos nombres)

| Español | En el código |
|---|---|
| Cuy | `GuineaPig` / `guinea_pig` / `guineaPig` |
| Jaula | `Cage` |
| Criadero | `Farm` |
| Marca de color | `MarkColor` |
| Evento de salud | `HealthEvent` |
| Estado de salud | `HealthState` |
| Normal / Observado / Alerta / Crítico | `NORMAL` / `OBSERVED` / `ALERT` / `CRITICAL` |
| Alerta | `Alert` |
| Lectura de peso | `WeightReading` |
| Perfil normal | `BaselineProfile` |
| Comedero / Bebedero | `feeder` / `waterer` |
| Tiempo inmóvil | `stillSeconds` |
| Distancia al grupo | `groupDistance` |
| Ingesta | `ingestion` |

---

## 5. Contratos entre repos (lo que TODOS tienen que respetar)

Esto es lo más importante del documento: si los contratos están claros, cada uno avanza en su repo sin bloquear al otro.

**Fuente de verdad:** `cuy-monitor-backend/docs/contracts/`. Si algo cambia, se cambia ahí primero y se avisa.

### 5.1 Tópicos de Kafka

| Tópico | Lo produce | Lo consume | Key |
|---|---|---|---|
| `events.camera` | ai-service | backend | `cageId` |
| `events.audio` | ai-service | backend | `cageId` |
| `events.weight` | backend (desde `/api/ingestion/weight`) | backend | `cageId` |
| `alerts.cage` | backend | (auditoría / futuros consumidores) | `cageId` |

Kafka **no se expone a internet**: solo se habla con él dentro de la red de Docker. Lo de afuera entra por HTTPS (ver ADR-003).

### 5.2 Formato de evento (sobre común)

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

Payload por tipo:

```jsonc
// BEHAVIOR (one per guinea pig, every 60 s window)
{ "color": "RED", "windowSeconds": 60, "stillSeconds": 48, "feederVisits": 0,
  "watererVisits": 1, "avgGroupDistance": 0.72, "probAnomaly": 0.81, "detectionConfidence": 0.93 }

// AUDIO (one per ~1 s clip, or aggregated every 10 s)
{ "label": "DISTRESS | NORMAL", "probability": 0.88, "durationMs": 960 }

// WEIGHT
{ "grams": 812.4, "stable": true }
```

### 5.3 API REST que expone el backend

| Método y ruta | Quién la usa | Para qué |
|---|---|---|
| `GET /api/cages/{id}/health` | dashboard | Resumen de la jaula (sale del Composite) |
| `GET /api/cages/{id}/guinea-pigs` | dashboard | Lista de cuyes con su estado actual |
| `POST /api/cages/{id}/guinea-pigs` | dashboard | Registrar un cuy (nombre + color de marca) |
| `GET /api/guinea-pigs/{id}/history?from=&to=` | dashboard | Gráficas de comportamiento y estados |
| `GET /api/alerts?status=OPEN` | dashboard | Lista de alertas |
| `PATCH /api/alerts/{id}` | dashboard | Marcar alerta como revisada (`status: REVIEWED`) |
| `GET /api/cages/{id}/weight?from=&to=` | dashboard | Historial de peso |
| `POST /api/ingestion/weight` | serial_bridge (arduino) | Recibir lectura de peso (header `X-API-Key`) |
| `WS /ws` → `/topic/cages/{id}` | dashboard | Actualizaciones en vivo (Observer) |
| `GET /actuator/health` | todos / Caddy | Saber si está vivo |

### 5.4 API que expone ai-service

| Método y ruta | Quién la usa | Para qué |
|---|---|---|
| `POST /ai/frames` | edge_agent | Recibir un frame JPEG (header `X-API-Key`) |
| `POST /ai/audio` | edge_agent | Recibir un clip WAV de ~1 s |
| `GET /ai/health` | todos | Saber si está vivo |

### 5.5 Enums compartidos

- `MarkColor`: `RED, BLUE, GREEN, YELLOW, ORANGE, PURPLE, BLACK, WHITE`
- `HealthStatus`: `NORMAL, OBSERVED, ALERT, CRITICAL`
- `EventType`: `BEHAVIOR, AUDIO, WEIGHT`
- `AlertStatus`: `OPEN, REVIEWED`

El mismo enum en Java (`MarkColor.java`), Python (`colors.py`) y TypeScript (`MarkColor.ts`). Si agregan un valor, se agrega en los tres.

---

## 6. Repositorio por repositorio

### 6.1 `cuy-monitor-backend` (Java 25 + Spring Boot 4.1) — el corazón

**Qué hace:** consume eventos de Kafka, aplica los 6 patrones, guarda en Postgres, expone la API y el WebSocket al dashboard. Además guarda la infraestructura de despliegue (`infra/`) y los contratos (`docs/contracts/`).

**Necesita de otros repos:**
- De `cuy-monitor-ai-service`: que publique en `events.camera` y `events.audio` con el formato de la sección 5.2 (mientras tanto, se usa un productor falso, ver `dev/`).
- De `cuy-monitor-arduino`: que el serial_bridge llame a `POST /api/ingestion/weight`.
- De `cuy-monitor-dashboard`: nada para funcionar, pero hay que habilitar CORS para su dominio de Amplify.

**Estructura:**

```
cuy-monitor-backend/
├── pom.xml
├── Dockerfile                      ← eclipse-temurin:25-jre
├── README.md
├── docs/
│   ├── contracts/
│   │   ├── events.md               ← event format and topics (source of truth)
│   │   ├── rest-api.md
│   │   └── examples/*.json         ← one sample JSON per event type
│   ├── adr/                        ← architecture decisions (section 8)
│   └── diagrams/                   ← UML class diagram per pattern (for the report)
├── infra/
│   ├── docker-compose.yml          ← backend + ai-service + kafka + postgres + caddy
│   ├── docker-compose.dev.yml      ← local version (no caddy, open ports)
│   ├── Caddyfile
│   ├── .env.example
│   └── scripts/
│       ├── create-topics.sh
│       └── setup-ec2.sh            ← installs docker, clones repos, starts everything
├── dev/
│   └── fake-producer/              ← publishes made-up events to Kafka
└── src/
    ├── main/java/com/cuymonitor/backend/
    │   ├── BackendApplication.java
    │   ├── config/                 ← KafkaConfig, WebSocketConfig, CorsConfig, ApiKeyFilter
    │   ├── domain/
    │   │   ├── model/              ← Cage, GuineaPig, MarkColor, HealthStatus, HealthEvent,
    │   │   │                          Alert, WeightReading, BaselineProfile
    │   │   └── repository/         ← Spring Data JPA interfaces
    │   ├── ingestion/              ← DATA INPUT
    │   │   ├── kafka/              ← EventConsumer (@KafkaListener), WeightProducer
    │   │   ├── dto/                ← KafkaEventDto and payload records
    │   │   ├── factory/            ── FACTORY METHOD PATTERN
    │   │   │   ├── AdapterFactory.java              (abstract creator)
    │   │   │   ├── CameraAdapterFactory.java
    │   │   │   ├── AudioAdapterFactory.java
    │   │   │   └── WeightAdapterFactory.java
    │   │   └── adapter/            ── ADAPTER PATTERN
    │   │       ├── EventSourceAdapter.java          (target: converts to HealthEvent)
    │   │       ├── CameraBehaviorAdapter.java
    │   │       ├── AudioClassificationAdapter.java
    │   │       └── WeightReadingAdapter.java
    │   ├── health/                 ← HEALTH CORE
    │   │   ├── chain/              ── CHAIN OF RESPONSIBILITY PATTERN
    │   │   │   ├── EventHandler.java                (abstract handler)
    │   │   │   ├── ValidationHandler.java
    │   │   │   ├── IdentificationHandler.java
    │   │   │   ├── BehaviorThresholdHandler.java
    │   │   │   ├── SustainedAnomalyHandler.java
    │   │   │   └── HandlerChainBuilder.java         (wires the chain in order)
    │   │   ├── state/              ── STATE PATTERN
    │   │   │   ├── HealthState.java                 (interface)
    │   │   │   ├── NormalState.java
    │   │   │   ├── ObservedState.java
    │   │   │   ├── AlertState.java
    │   │   │   ├── CriticalState.java
    │   │   │   └── GuineaPigHealthContext.java
    │   │   └── composite/          ── COMPOSITE PATTERN
    │   │       ├── HealthComponent.java             (component)
    │   │       ├── GuineaPigHealth.java             (leaf)
    │   │       ├── CageAudioHealth.java             (leaf)
    │   │       ├── CageWeightHealth.java            (leaf)
    │   │       └── CageHealth.java                  (composite)
    │   ├── notification/           ── OBSERVER PATTERN
    │   │   ├── AlertObserver.java                   (interface)
    │   │   ├── AlertPublisher.java                  (subject)
    │   │   ├── WebSocketAlertObserver.java          (pushes to the dashboard)
    │   │   ├── DatabaseAlertObserver.java           (stores in Postgres)
    │   │   └── KafkaAlertObserver.java              (publishes to alerts.cage)
    │   └── api/
    │       ├── CageController.java
    │       ├── GuineaPigController.java
    │       ├── AlertController.java
    │       ├── IngestionController.java             (POST /api/ingestion/weight)
    │       └── dto/
    ├── main/resources/
    │   ├── application.yml
    │   ├── application-dev.yml
    │   ├── application-prod.yml
    │   └── db/migration/           ← V1__initial_schema.sql, V2__..., (Flyway)
    └── test/java/com/cuymonitor/backend/
        ├── health/state/           ← state transitions
        ├── health/chain/           ← each handler on its own
        ├── health/composite/
        ├── ingestion/              ← factory + adapters
        └── integration/            ← Kafka + Postgres with Testcontainers
```

**Nota sobre Observer:** hagan la implementación "a mano" (interfaz + lista de observers), no con `ApplicationEventPublisher` de Spring, para que el patrón se vea explícito en la sustentación. En el informe pueden mencionar que Spring tiene su propio mecanismo que hace lo mismo.

**Nota sobre Java 25:** los DTOs y payloads de eventos quedan muy limpios como `record`, y el `switch` con pattern matching sirve para el `AdapterFactory`. Lombok queda solo para las entidades JPA.

**Modelo de datos (Postgres 18):**

```
cage (id, name, location, created_at)
guinea_pig (id, cage_id → cage, name, mark_color, current_status, active, created_at)
event (id uuid, type, cage_id, guinea_pig_id NULL, source, occurred_at, payload JSONB)
state_transition (id, guinea_pig_id, from_status, to_status, reason, occurred_at)
alert (id, cage_id, guinea_pig_id NULL, level, type, message, status OPEN|REVIEWED, created_at, reviewed_at)
weight_reading (id, cage_id, grams, stable, measured_at)
baseline_profile (guinea_pig_id, avg_still_seconds, avg_feeder_visits, avg_group_distance, updated_at)
```

`guinea_pig_id` es NULL en eventos y alertas de audio, porque el audio es de la jaula completa, no de un cuy.

---

### 6.2 `cuy-monitor-ai-service` (Python 3.14 + FastAPI) — la IA

**Qué hace:** recibe frames y audio, detecta y sigue a cada cuy por color, calcula comportamiento, clasifica audio y publica los resultados en Kafka. También guarda el **edge_agent** que corre en la laptop.

**Necesita de otros repos:**
- De `cuy-monitor-backend`: el formato de eventos (`docs/contracts/events.md`) y la lista de tópicos.
- De `cuy-monitor-backend/infra`: el `docker-compose.yml` lo levanta junto con Kafka.
- De la jaula real: videos y audios para entrenar (sección 9).

**Estructura:**

```
cuy-monitor-ai-service/
├── pyproject.toml              ← managed with uv (or pip)
├── Dockerfile                  ← python:3.14-slim, NO TensorFlow at runtime
├── README.md
├── app/
│   ├── main.py                 ← FastAPI: /ai/frames, /ai/audio, /ai/health
│   ├── config.py               ← env vars (KAFKA_BOOTSTRAP, API_KEY, THRESHOLDS)
│   ├── contracts/
│   │   ├── events.py           ← Pydantic models, faithful copy of the backend contracts
│   │   └── colors.py           ← MarkColor enum
│   ├── vision/
│   │   ├── detector.py         ← YOLO26n on ONNX Runtime → boxes + color
│   │   ├── tracker.py          ← Hungarian assignment (scipy.optimize.linear_sum_assignment)
│   │   └── zones.py            ← feeder and waterer coordinates in the image
│   ├── behavior/
│   │   ├── features.py         ← per window: still seconds, visits, group distance
│   │   └── classifier.py       ← loads the Random Forest (.joblib)
│   ├── audio/
│   │   ├── embeddings.py       ← YAMNet converted to ONNX
│   │   └── classifier.py       ← SVM/KNN over embeddings
│   └── messaging/
│       └── producer.py         ← aiokafka
├── models/                     ← .onnx / .joblib (don't commit heavy files: use GitHub Releases or Git LFS)
├── training/                   ← runs in Google Colab, NOT in the Docker image
│   ├── notebooks/
│   ├── labeling/               ← labeling guide + Label Studio/Roboflow exports
│   ├── train_detector.py       ← Ultralytics YOLO26n → export to ONNX
│   ├── train_behavior.py
│   ├── train_audio.py
│   └── export_yamnet_onnx.py   ← one-time TF → ONNX conversion (in Colab, with TF)
├── edge_agent/                 ← RUNS ON THE LAPTOP, not on AWS
│   ├── agent.py                ← reads the A12 stream, sends 1-2 fps to /ai/frames and audio to /ai/audio
│   ├── config.example.env      ← A12 URL, EC2 URL, API key
│   └── install_service.md      ← how to keep it running (systemd on Linux / NSSM or Task Scheduler on Windows)
├── dev/
│   └── simulator.py            ← replays a recorded video as if it were the live camera
└── tests/
```

**Para el avance (mock):** `main.py` puede responder a `/ai/frames` sin modelo y publicar eventos con valores inventados. Así el backend y el dashboard ya ven datos fluyendo de punta a punta.

---

### 6.3 `cuy-monitor-dashboard` (React 19 + TypeScript 6 + Vite 8) — lo que ve el criador

**Qué hace:** muestra la jaula, cada cuy con su color y estado, gráficas de su historial, las alertas y el peso.

**Necesita de otros repos:**
- De `cuy-monitor-backend`: la API REST y el WebSocket (sección 5.3). La URL va en `VITE_API_URL`.
- De `cuy-monitor-backend/docs/contracts`: los tipos de datos para escribir `src/types/`.

**Estructura:**

```
cuy-monitor-dashboard/
├── package.json                ← "engines": { "node": ">=24" }
├── vite.config.ts
├── tsconfig.json               ← "strict": true
├── eslint.config.js            ← typescript-eslint
├── amplify.yml                 ← AWS Amplify build
├── .env.example                ← VITE_API_URL, VITE_WS_URL
└── src/
    ├── main.tsx
    ├── App.tsx                 ← routes (React Router 8)
    ├── i18n/
    │   ├── index.ts            ← i18next setup, default language "es"
    │   └── locales/
    │       └── es.json         ← ALL user-facing text lives here (in Spanish)
    ├── types/                  ← GuineaPig.ts, HealthStatus.ts, Alert.ts, CageHealth.ts, MarkColor.ts
    ├── api/
    │   ├── client.ts           ← fetch with base URL
    │   ├── cages.ts
    │   ├── guineaPigs.ts
    │   └── alerts.ts
    ├── realtime/
    │   └── useLiveAlerts.ts    ← STOMP client over /ws, subscribes to /topic/cages/{id}
    ├── pages/
    │   ├── CageOverview.tsx    ← main view: one card per guinea pig + cage traffic light
    │   ├── GuineaPigDetail.tsx ← history and charts for one guinea pig
    │   ├── Alerts.tsx
    │   └── RegisterGuineaPig.tsx ← name + mark color
    ├── components/
    │   ├── GuineaPigCard.tsx
    │   ├── StatusBadge.tsx     ← NORMAL green, OBSERVED yellow, ALERT orange, CRITICAL red
    │   ├── BehaviorChart.tsx
    │   ├── WeightChart.tsx
    │   └── AlertList.tsx
    ├── mocks/                  ← fake data to work without the backend
    └── styles/
```

Ejemplo de cómo se ve un texto en un componente:

```tsx
// GuineaPigCard.tsx — code in English, visible text from es.json
<StatusBadge status={guineaPig.status} />
<p>{t('guineaPig.lastSeen', { minutes: guineaPig.lastSeenMinutes })}</p>
```

```json
// es.json
{ "guineaPig": { "lastSeen": "Visto hace {{minutes}} min" },
  "status": { "NORMAL": "Normal", "OBSERVED": "En observación", "ALERT": "Alerta", "CRITICAL": "Crítico" } }
```

---

### 6.4 `cuy-monitor-arduino` (Arduino C++ + puente en Python 3.14) — el peso

**Qué hace:** el Arduino lee la celda de carga con el HX711 y manda el peso por USB serial. En la laptop, el `serial_bridge` lee ese puerto y le manda el dato al backend.

**¿Por qué hay un puente?** El Arduino Uno no tiene WiFi. Se conecta por USB a la laptop, que sí tiene internet. (Si algún día cambian a un ESP32, puede mandar el dato directo por WiFi y el puente sobra.)

**Necesita de otros repos:**
- De `cuy-monitor-backend`: el endpoint `POST /api/ingestion/weight`, el formato del payload `WEIGHT` y la API key.

**Estructura:**

```
cuy-monitor-arduino/
├── README.md                   ← wiring diagram and photos of the setup
├── firmware/
│   └── weight_sensor/
│       └── weight_sensor.ino   ← reads HX711, averages, detects stability, prints JSON over serial
├── calibration/
│   └── calibrate/
│       └── calibrate.ino       ← sketch to get the calibration factor with a known weight
├── serial_bridge/              ← RUNS ON THE LAPTOP
│   ├── bridge.py               ← pyserial → POST /api/ingestion/weight
│   ├── config.example.env      ← COM/tty port, backend URL, API key
│   └── requirements.txt
└── docs/
    ├── wiring.md               ← HX711: DT → pin 3, SCK → pin 2, VCC 5V, GND
    └── mounting.md             ← how to install the platform without stressing the animals
```

(Arduino obliga a que cada `.ino` esté dentro de una carpeta con su mismo nombre; por eso `calibrate/calibrate.ino`.)

Salida por serial del Arduino, una línea cada ~2 segundos:

```json
{"grams": 812.4, "stable": true}
```

---

## 7. Despliegue en AWS

### Qué se crea en la cuenta

| Recurso | Configuración |
|---|---|
| EC2 | **t3.medium** (4 GB RAM) con **Ubuntu 26.04 LTS**; si el plan gratuito no la deja, t3.small con 2 GB de swap |
| Disco | 30 GB gp3 |
| Elastic IP | Para que la IP no cambie cada vez que se reinicia |
| Security Group | Abiertos solo 80 y 443 al mundo; 22 (SSH) solo para sus IPs. **Kafka (9092) y Postgres (5432) cerrados** |
| Amplify | App conectada a `cuy-monitor-dashboard`, rama `main`, Node 24 |
| AWS Budgets | Alerta al correo si el gasto pasa de $20, para no llevarse sorpresas con los créditos |
| Dominio | Subdominio gratis en DuckDNS apuntando a la Elastic IP (Caddy lo necesita para el certificado) |

### Memoria en la EC2 (por qué t3.medium)

| Contenedor | RAM aprox. |
|---|---|
| Kafka 4.3 (con `KAFKA_HEAP_OPTS=-Xmx512m`) | ~700 MB |
| Backend Spring Boot 4.1 en Java 25 (`-Xmx512m`) | ~650 MB |
| ai-service (YOLO26n ONNX + YAMNet ONNX + sklearn, sin TensorFlow) | ~600 MB – 1 GB |
| Postgres 18 | ~150 MB |
| Caddy | ~30 MB |
| **Total** | **~2.2 – 2.7 GB** → en 2 GB no cabe cómodo |

### Imágenes Docker

| Servicio | Imagen base |
|---|---|
| backend | `eclipse-temurin:25-jre` (build con `maven:3-eclipse-temurin-25`) |
| ai-service | `python:3.14-slim` |
| kafka | `apache/kafka:4.3.1` |
| postgres | `postgres:18` |
| caddy | `caddy:2.11` |

Siempre fijar la versión (nunca `:latest`), para que lo que funciona hoy funcione igual el día de la sustentación.

### Cómo se despliega

1. En la EC2: `git clone` de `cuy-monitor-backend` y `cuy-monitor-ai-service` uno al lado del otro.
2. `infra/docker-compose.yml` construye el backend con `build: ..` y la IA con `build: ../../cuy-monitor-ai-service`.
3. `cp .env.example .env`, llenar contraseñas y API key.
4. `docker compose up -d --build`.
5. `infra/scripts/create-topics.sh` crea los 4 tópicos.
6. Todos los servicios con `restart: unless-stopped` para que vuelvan solos si se caen o si se reinicia la máquina.
7. Para actualizar: `git pull` en ambos repos + `docker compose up -d --build`.

Más adelante (no para el avance) se puede automatizar con GitHub Actions: build de imágenes → GitHub Container Registry → la EC2 hace `docker compose pull`.

### En la laptop del criadero

- Python 3.14 instalado.
- `edge_agent` y `serial_bridge` como servicios que arrancan solos al encender.
- Desactivar suspensión e hibernación.
- El A12 con IP fija en el WiFi del criadero (reserva DHCP en el router, o IP estática en el celular) para que el edge_agent siempre lo encuentre.

---

## 8. Decisiones de arquitectura (ADRs)

### ADR-001: Multi-repo (4 repos)

**Estado:** Aceptada · **Decidieron:** los dos

| | Multi-repo (elegido) | Monorepo |
|---|---|---|
| Complejidad | Media (hay que coordinar contratos) | Baja |
| Claridad para evaluar | Alta: cada entrega es independiente | Media |
| Riesgo | Que los contratos se desincronicen | Commits mezclados |

**Consecuencia:** los contratos viven en un solo lugar (`cuy-monitor-backend/docs/contracts/`) y los otros repos los copian. Cualquier cambio al contrato se avisa en el chat del equipo antes de hacer push.

### ADR-002: IA en Python, separada del backend Java

**Estado:** Aceptada

| | Python separado (elegido) | Todo en Java (ONNX Runtime for Java) |
|---|---|---|
| Entrenar modelos | Fácil (Ultralytics y scikit-learn son nativos de Python) | Hay que entrenar en Python igual y exportar |
| Inferir | Directo | Posible, pero más código y menos ejemplos |
| Cumple "backend en Java" | Sí: los patrones están en Java | Sí |
| Complejidad de despliegue | Un contenedor más | Uno menos |

**Consecuencia:** la IA es un "colaborador externo" que habla por Kafka. El backend no sabe nada de YOLO ni de YAMNet, solo de eventos, y eso es justo lo que el Adapter demuestra.

### ADR-003: Kafka solo dentro de Docker; lo de afuera entra por HTTPS

**Estado:** Aceptada

| | Kafka interno + ingesta HTTPS (elegido) | Exponer Kafka a internet |
|---|---|---|
| Seguridad | Alta: Kafka no es alcanzable desde afuera | Hay que configurar SASL + TLS en Kafka (difícil) |
| Desde la laptop | Un `POST` con API key, fácil | Cliente Kafka en la laptop + certificados |
| Si se cae la conexión del criadero | El edge_agent reintenta | Igual |

**Consecuencia:** la laptop nunca habla con Kafka. El ai-service y el backend son los únicos que producen. Así se cumple el requisito de Kafka sin abrir un hueco de seguridad.

### ADR-004: Una sola EC2 con Docker Compose

**Estado:** Aceptada para avance y entrega final

| | Una EC2 + Compose (elegido) | ECS / EKS / MSK (Kafka administrado) |
|---|---|---|
| Costo | Bajo, cabe en los créditos | MSK solo ya se come los créditos |
| Complejidad | Baja | Alta |
| Alta disponibilidad | No (si se cae la máquina, se cae todo) | Sí |

**Consecuencia:** es un punto único de falla, pero para una jaula piloto está bien. En el informe, en "visión a futuro", mencionar que para varias jaulas se pasaría a ECS + MSK o a varias instancias.

### ADR-005: ¿La IA corre en la nube o en la laptop? (para revisar en octubre)

**Estado:** Propuesta: arrancamos en la nube, medimos y decidimos

| | En la nube (arranque) | En la laptop Celeron |
|---|---|---|
| Internet del criadero | Sube ~1 fps × ~40 KB ≈ 3.5 GB/día + audio ≈ 2.7 GB/día | Solo suben eventos (casi nada) |
| CPU | Sobra | YOLO26n es el más rápido en CPU de la familia; a 320 px en un Celeron debería dar 1–3 fps, hay que medirlo |
| Actualizar el modelo | Fácil (en el EC2) | Hay que actualizar la laptop |

**Qué medir en octubre:** cuánto internet hay en el criadero y a cuántos fps corre YOLO26n en la laptop. Como el ai-service está en Docker, moverlo a la laptop es cambiar dónde se levanta el contenedor, no reescribir código.

### ADR-006: Versiones: LTS nuevas, sin betas, sin ".0 sin ecosistema"

**Estado:** Aceptada (24 sept 2026)

- **Java 25 LTS** en vez de 17, porque el soporte premier de 17 termina este mes y 25 cubre hasta 2030.
- **Spring Boot 4.1** porque 3.5 ya está en fin de vida.
- **TypeScript 6.0** en vez de 7.0: 7.0 es la última, pero todavía no tiene API para typescript-eslint. Se migra a 7.1.
- **PostgreSQL 18** y no 19, porque 19 está en beta.
- **Python 3.14** en el servicio, con **YAMNet en ONNX** porque TensorFlow no soporta 3.14. Si la conversión falla, el ai-service baja a 3.13 + TF 2.21 (plan B documentado).
- Todas las imágenes Docker con versión fija.

---

## 9. Datos para entrenar (lo que hay que conseguir en octubre)

| Dataset | Cómo se consigue | Cuánto | Para qué modelo |
|---|---|---|---|
| Frames con cuyes marcados | Grabar la jaula (A12) en distintas horas y luces | 300–600 frames etiquetados (cajas + color) | YOLO26n (detector) |
| Comportamiento normal vs. anómalo | Ventanas de 60 s por cuy, etiquetadas observando + ayuda de la tía | Mínimo unos cientos de ventanas | Random Forest |
| Audio normal | Grabar la jaula en calma | 1–2 horas cortadas en clips de 1 s | SVM/KNN sobre YAMNet |
| Audio de angustia | Grabaciones de referencia de literatura/videos de manejo + lo que se capture | Lo que se pueda (esta es la clase difícil) | SVM/KNN sobre YAMNet |

Herramientas gratis para etiquetar: Label Studio o Roboflow (plan gratis). El detector se entrena en Google Colab (GPU gratis), no en la laptop. La conversión de YAMNet a ONNX también se hace en Colab, una sola vez.

**Importante:** si los cuyes de la tía no se consiguen pronto, se puede empezar el detector con objetos de colores (pompones, bolitas de lana) en una caja, para tener el pipeline funcionando y luego reentrenar con cuyes reales.

---

## 10. Quién hace qué

Los dos trabajan en todo, pero cada pieza tiene **un dueño** que responde por ella. El otro revisa sus Pull Requests. Así en la sustentación cada uno puede explicar a fondo su parte y conoce la del otro.

### Tú (Josuram): núcleo de salud + dashboard + despliegue

| Repo | Responsabilidad |
|---|---|
| `cuy-monitor-backend` | Patrones **State**, **Chain of Responsibility**, **Composite** (`health/`). Modelo de datos, migraciones Flyway, API REST (`api/`), WebSocket |
| `cuy-monitor-dashboard` | Todo el dashboard, incluido `es.json` |
| `cuy-monitor-backend/infra` | Docker Compose, Caddy, EC2, Amplify, DuckDNS, AWS Budgets |

### Tu compañero: entrada de datos + IA + Arduino

| Repo | Responsabilidad |
|---|---|
| `cuy-monitor-backend` | Patrones **Factory Method**, **Adapter** (`ingestion/`), **Observer** (`notification/`). Consumer/producer de Kafka, `IngestionController` |
| `cuy-monitor-ai-service` | Todo: detector, tracker, features, clasificadores, productor Kafka, edge_agent, conversión de YAMNet |
| `cuy-monitor-arduino` | Firmware, calibración, serial_bridge, montaje físico |

### Juntos

- Escribir y mantener los **contratos** (sección 5) y el **glosario** (sección 4.2). Es lo primero que se hace.
- El `docker-compose.yml` (tú lo mantienes, él agrega el servicio de la IA).
- Grabar y **etiquetar datos** en la jaula (es mucho trabajo, hay que repartirlo).
- Pruebas de punta a punta, el informe y la sustentación.

### Cómo se conectan las dos mitades

```
Compañero                                            Tú
AdapterFactory ─► EventSourceAdapter ─► HealthEvent ─► EventHandler chain ─► HealthState ─► CageHealth
                                                                                               │
AlertPublisher ◄───────────────────── (status change / alert) ◄────────────────────────────────┘
   └─► WebSocketAlertObserver ─────────────────────────────────────────────────────► Dashboard
```

La frontera entre los dos es el record **`HealthEvent`**: él entrega `HealthEvent` ya normalizados, tú los procesas, y cuando algo cambia llamas a `AlertPublisher`, que es suyo. Definan `HealthEvent` y la interfaz `AlertObserver` **el primer día**.

### Reglas de trabajo (cortas)

- Rama `main` protegida; todo entra por Pull Request con revisión del otro.
- Ramas en inglés: `feature/state-transitions`, `fix/kafka-consumer-offset`.
- Commits en inglés con Conventional Commits: `feat(state): add OBSERVED to ALERT transition`, `fix(adapter): handle missing color`, `docs(contracts): add WEIGHT payload`.
- Issues de GitHub para las tareas, en un tablero de Projects con columnas *Todo / In progress / Review / Done*.
- Formateadores automáticos para no discutir estilo: Spotless (Java), Ruff (Python), Prettier + ESLint (TS).

---

## 11. Plan paso a paso

### Fase 0: avance desplegado (24 al 30 de septiembre)

La meta del avance es **que todo esté desplegado en la nube y conectado**, aunque sea con datos falsos.

| Día | Tú | Compañero |
|---|---|---|
| 24–25 sep | Instalar JDK 25 (Temurin). Spring Initializr con **Boot 4.1 + Java 25** y las dependencias de la sección 3.2. Estructura de paquetes de la sección 6.1. Migración `V1__initial_schema.sql`. | Escribir `docs/contracts/` (juntos). `HealthEvent`, `AlertObserver`. Consumer Kafka que imprime lo que llega. |
| 26–27 sep | Esqueleto de State (4 estados), Chain (4 handlers) y Composite, con tests básicos. Controllers devolviendo datos de la BD. | `AdapterFactory` + 3 adapters. `AlertPublisher` + `WebSocketAlertObserver`. ai-service con FastAPI mock que publica eventos inventados. |
| 27–28 sep | Dashboard: `npm create vite@latest` (React + TS), i18n con `es.json`, página CageOverview con mocks, luego conectada a la API. | `fake-producer` y `simulator.py`. Dockerfile del ai-service (`python:3.14-slim`). |
| 28–29 sep | EC2 Ubuntu 26.04 + Elastic IP + DuckDNS + Docker Compose con los 5 contenedores + Amplify. | Probar el flujo completo local con `docker-compose.dev.yml` y arreglar lo que falle. |
| 30 sep | **Avance:** URL del dashboard funcionando, eventos falsos entrando por Kafka, estados cambiando, alerta llegando por WebSocket. | ← lo mismo |

**Lo que NO va en el avance:** modelos entrenados, Arduino, cámara real. Todo eso es mock.

### Fase 1: datos y modelos reales (1 al 26 de octubre)

| Semana | Tú | Compañero |
|---|---|---|
| 1–5 oct | Lógica real del State (reglas de transición, incluyendo volver a NORMAL). `BaselineProfile` por cuy. | Comprar kit Arduino. Pedir los cuyes a la tía. Primeras grabaciones (o pompones de colores). Convertir YAMNet a ONNX en Colab. |
| 6–12 oct | `SustainedAnomalyHandler` con ventanas de tiempo. Página GuineaPigDetail con gráficas. | Etiquetar frames y entrenar YOLO26n en Colab → ONNX. Tracker húngaro. Firmware + calibración del HX711. |
| 13–19 oct | Alertas en el dashboard (listar, marcar revisada). Tests de integración con Testcontainers. | Features de comportamiento + Random Forest. serial_bridge funcionando con el backend. |
| 20–26 oct | Gráfica de peso. Medir RAM/CPU de la EC2. Evaluar subir el front a Node 26 LTS. | Clasificador de audio (YAMNet ONNX + SVM). edge_agent real con el A12. Medir fps en la laptop (ADR-005). |

### Fase 2: instalación en la jaula y ajuste (27 de octubre al 9 de noviembre)

- Montar el A12 cenital, la laptop y el Arduino en la jaula piloto.
- Dejarlo corriendo varios días seguidos.
- Anotar falsos positivos y falsos negativos → ajustar umbrales y la N de `SustainedAnomalyHandler`.
- Decidir ADR-005 (IA en nube o en laptop) con los datos medidos.

### Fase 3: cierre (10 de noviembre hasta la entrega)

- Diagramas UML de cada patrón para el informe (`docs/diagrams/`).
- Informe final (en español): problema, arquitectura, patrones con código, resultados con datos reales, limitaciones, visión a futuro.
- Ensayar la sustentación: cada uno explica sus 3 patrones y su repo.

---

## 12. Riesgos y qué hacer

| Riesgo | Qué tan probable | Plan B |
|---|---|---|
| No se consiguen los cuyes a tiempo | Media | Entrenar con pompones/objetos de colores; demostrar el pipeline y dejar el reentrenamiento documentado |
| El internet del criadero es malo | Media | Mover el ai-service a la laptop (ADR-005) |
| El Celeron no da abasto | Media | Bajar a 1 fps y 320 px; o mantener la IA en la nube |
| YAMNet no se deja convertir a ONNX | Media | ai-service en Python 3.13 + TensorFlow 2.21 (imagen más pesada, pero funciona) |
| Ejemplos de internet escritos para Spring Boot 3 | Alta | Revisar la sección 3.2: starter `webmvc`, starter `flyway`, Jackson `tools.jackson` |
| Poca data de chillidos de angustia | Alta | Tratarlo como detección de "sonido anómalo" (lo que no se parece a lo normal) + audios de referencia |
| La EC2 se queda sin memoria | Baja con t3.medium | Limitar heaps de Java/Kafka; agregar swap |
| Se acaban los créditos de AWS | Baja | AWS Budgets avisa; apagar la EC2 cuando no se use antes del montaje real |
| El marcador del lomo se borra | Alta (es normal) | Reaplicar cada 1–2 semanas; el tracker debe tolerar confianza baja sin dar alertas falsas |

---

## 13. Lo que revisaríamos si el proyecto crece

- Varias jaulas → `cageId` ya es la key de los tópicos; una laptop/Raspberry por jaula.
- Pasar de una EC2 a ECS + Amazon MSK si hay muchas jaulas.
- Notificaciones al celular del criador: agregar un observer nuevo, `PushAlertObserver` (el patrón ya lo permite sin tocar lo demás).
- Cambiar Arduino Uno + laptop por un ESP32 que mande el peso por WiFi.
- Raspberry Pi en vez de laptop, para bajar consumo y espacio.
- Migrar a TypeScript 7.1 cuando salga, y a Node 26 LTS.

---

## Fuentes de las versiones

- Java: [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- Spring Boot: [HeroDevs — Spring Boot versions and EOL](https://www.herodevs.com/blog-posts/spring-boot-versions-eol-dates-and-latest-releases-april-2026) · [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- Kafka: [Apache Kafka release announcements](https://kafka.apache.org/blog/releases/)
- PostgreSQL: [18.6 y 19 Beta 3](https://www.postgresql.org/about/news/postgresql-186-1711-1615-1519-1424-and-19-beta-3-released-3365/)
- Python: [Status of Python versions](https://devguide.python.org/versions/)
- TensorFlow y Python 3.14: [generalistprogrammer — TensorFlow Python support](https://generalistprogrammer.com/python-version-support/tensorflow)
- YOLO26: [Ultralytics YOLO26 docs](https://docs.ultralytics.com/models/yolo26)
- ONNX Runtime: [PyPI onnxruntime](https://pypi.org/project/onnxruntime/) · FastAPI: [PyPI](https://pypi.org/project/fastapi/) · aiokafka: [PyPI](https://pypi.org/project/aiokafka/) · scikit-learn: [PyPI](https://pypi.org/project/scikit-learn/)
- React: [React Versions](https://react.dev/versions) · TypeScript 7: [InfoQ](https://www.infoq.com/news/2026/08/typescript-7-released/) · Vite: [Releases](https://vite.dev/releases) · Node.js: [endoflife.date](https://endoflife.date/nodejs)
- Caddy: [Releases](https://github.com/caddyserver/caddy/releases) · Ubuntu 26.04: [Anuncio](https://discourse.ubuntu.com/t/ubuntu-26-04-resolute-raccoon-lts-released/80833)
