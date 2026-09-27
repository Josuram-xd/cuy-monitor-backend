# AGENTS.md — cuy-monitor-backend

Instrucciones para cualquier agente de IA (Claude Code, Copilot, Cursor, Codex…) que trabaje en este repo. Léelas completas antes de tocar código.

## Qué es este repo

Backend en **Java 25 + Spring Boot 4.1** del Monitor de Salud de Cuyes. Recibe eventos por HTTP (`POST /api/ingestion/events`), aplica 6 patrones de diseño dentro de un núcleo hexagonal, guarda en PostgreSQL y expone REST + WebSocket al dashboard. También contiene el despliegue (`infra/`) y los contratos entre repos (`docs/contracts/`).

Es un proyecto universitario de **Patrones de Diseño**: que cada patrón se vea claro y explicable importa más que ahorrar líneas.

Lee antes de trabajar:
- `docs/PRD.md` — qué hace el producto y qué no.
- `docs/ARCHITECTURE.md` — cómo está armado, paquetes, patrones, contratos.
- `docs/contracts/` — formato de eventos, endpoint de ingesta y API. **Fuente de verdad para los 4 repos.**

## Comandos

```bash
./mvnw spring-boot:run                    # correr local (necesita Postgres)
./mvnw test                               # tests
./mvnw -B package -DskipTests             # compilar el jar
docker compose -f infra/docker-compose.dev.yml up -d   # Postgres local
```

En Windows usar `mvnw.cmd`. No uses un `mvn` global: siempre el Maven Wrapper.

## Idioma y nombres

- Todo el código en **inglés**: paquetes, clases, métodos, variables, tablas, columnas, endpoints, JSON, enums, comentarios, commits, ramas.
- Tablas y columnas en `snake_case`. JSON en `camelCase`. Endpoints en `kebab-case` y plural. Enums en `UPPER_SNAKE_CASE`.
- Usa el glosario: cuy = `GuineaPig`, jaula = `Cage`, marca de color = `MarkColor`, evento de salud = `HealthEvent`, perfil normal = `BaselineProfile`, comedero/bebedero = `feeder`/`waterer`.
- La documentación del equipo (PRD, este archivo) puede estar en español.

## Arquitectura hexagonal (no la rompas)

El backend es una **hexagonal pragmática** (ver sección 3 y ADR-007 de `docs/ARCHITECTURE.md`):

```
domain/        núcleo: modelo, patrones de salud, puertos        ← Java puro
application/   servicios que implementan los puertos de entrada
adapter/in/    web (controllers) e ingestion (Factory Method + Adapter)
adapter/out/   persistence (JPA) y notification (observers)
config/        arma los beans
```

1. **`domain/` es Java puro.** Nada de `org.springframework`, `jakarta.persistence`, `jakarta.validation` ni Jackson dentro de `domain/`. Ni anotaciones de Spring ni de JPA.
2. Las dependencias siempre apuntan **hacia adentro**: `adapter → application → domain`. `domain` nunca importa `application`, `adapter` ni `config`.
3. **Los controllers solo llaman puertos de entrada** (`domain/port/in/*UseCase`). Nunca repositorios, ni JPA, ni `EntityManager`.
4. El núcleo habla con la base de datos y con el exterior **solo por puertos de salida** (`domain/port/out`). Las implementaciones van en `adapter/out/`.
5. Las entidades JPA (`*JpaEntity`) viven en `adapter/out/persistence/entity/` y se convierten al modelo del dominio con un mapper. **No pongas `@Entity` en `domain/model`.**
6. Los objetos del dominio que necesitan ser beans (cadena, `AlertPublisher`, observers) se crean en `config/DomainConfig`, no con `@Component` en el dominio.
7. Un puerto nuevo = una interfaz en `domain/port/in` u `out` + su implementación en `application/` o `adapter/out/`. No crees puertos "por si acaso".
8. El test `architecture/HexagonalArchitectureTest` (ArchUnit) tiene que pasar siempre. Si falla, el código está mal ubicado: muévelo, no cambies la regla.

## Reglas de los patrones (no las rompas)

1. Cada patrón vive en su paquete: `adapter/in/ingestion/factory` (Factory Method), `adapter/in/ingestion/adapter` (Adapter), `domain/health/chain`, `domain/health/state`, `domain/health/composite`, y el Observer repartido en `domain/notification` (sujeto), `domain/port/out/AlertObserver` (puerto) y `adapter/out/notification` (observers concretos). No mezcles lógica de un patrón en otro paquete.
2. **Observer se implementa a mano** (puerto `AlertObserver` + lista en `AlertPublisher`). No lo reemplaces por `ApplicationEventPublisher` ni `@EventListener` de Spring.
3. **State**: cada estado es una clase que decide su propia transición. No conviertas el State en un `switch` gigante sobre `HealthStatus`.
4. **Chain**: cada handler hace una sola cosa y llama al siguiente. El orden se arma solo en `HandlerChainBuilder`.
5. **`HealthEvent`, `ProcessEventUseCase` y `AlertObserver` son el contrato entre los dos integrantes.** No cambies su forma sin que el usuario lo pida explícitamente.
6. DTOs, payloads y `HealthEvent` son `record`. Lombok solo en entidades JPA.
7. No agregues lógica de IA (detección, tracking, modelos) aquí: eso es del `cuy-monitor-ai-service`.

## Reglas de Spring Boot 4

- Starter web: `spring-boot-starter-webmvc` (no `spring-boot-starter-web`).
- Flyway: `spring-boot-starter-flyway` + `flyway-database-postgresql`.
- Jackson 3: imports de `tools.jackson.*`, **no** `com.fasterxml.jackson.*`. Si copias ejemplos de internet de Boot 3, adáptalos.
- No pongas versiones a mano a dependencias que maneja el BOM de Spring Boot.

## Base de datos

- El esquema lo maneja **Flyway**. `ddl-auto` es `validate`; nunca lo cambies a `update` o `create`.
- **Nunca edites una migración que ya existe** (`V1__...`). Para cualquier cambio crea `V{n+1}__descripcion.sql`.
- `guinea_pig_id` es `NULL` en eventos y alertas de audio y peso (son de jaula).

## Ingesta

- La ingesta es **HTTP directo** (ver ADR-003 en `docs/ARCHITECTURE.md`). No agregues colas ni brokers de mensajes sin que el usuario lo pida.
- Todos los datos de afuera entran por **un solo endpoint**: `POST /api/ingestion/events` con header `X-API-Key` y el sobre común (`eventId`, `type`, `cageId`, `timestamp`, `source`, `schemaVersion`, `payload`).
- El `AdapterFactory` elige el adaptador según `type` (`BEHAVIOR`, `AUDIO`, `WEIGHT`). No crees un endpoint distinto por tipo de evento.
- `eventId` lo genera quien manda el evento y sirve para detectar duplicados (los productores reintentan).

## Contratos

Si un cambio afecta el formato de eventos, un endpoint o un enum compartido (`MarkColor`, `HealthStatus`, `EventType`, `AlertStatus`):
1. Actualiza primero `docs/contracts/`.
2. Avísale al usuario que hay que actualizar los otros repos (`cuy-monitor-ai-service`, `cuy-monitor-dashboard`, `cuy-monitor-arduino`).
3. Sube `schemaVersion` si el cambio rompe compatibilidad.

## Seguridad

- Nunca escribas secretos en el código ni en `application.yml`. Van en variables de entorno.
- Nunca crees, edites ni hagas commit de `infra/.env`. Solo `infra/.env.example` con valores de ejemplo.
- Los endpoints de ingesta llevan header `X-API-Key`.
- No abras puertos nuevos en Compose. Lo único público es Caddy (80/443).

## Tests

- Cada cambio en `domain/health/` o `adapter/in/ingestion/` va con su test unitario.
- Los tests de `domain/` son **JUnit puro**: sin `@SpringBootTest`, sin base de datos. Si necesitas un repositorio, usa un fake en memoria que implemente el puerto.
- Transiciones de estado: un test por transición, incluidas las de regreso a `NORMAL`.
- Handlers: cada uno probado solo, sin armar la cadena completa.
- Integración con Testcontainers en `src/test/java/.../integration/`.
- Antes de decir que terminaste: `./mvnw test` tiene que pasar.

## Git

- Commits con Conventional Commits en inglés: `feat(state): add OBSERVED to ALERT transition`, `fix(adapter): handle missing color`, `docs(contracts): add WEIGHT payload`.
- Ramas: `feature/...`, `fix/...`, `docs/...`.
- `main` se toca solo por Pull Request.
- **Prohibido**: `git push --force` a `main`, `git reset --hard` sobre trabajo sin commitear, reescribir historial compartido.

## Lo que el agente NO debe hacer sin permiso explícito

- Correr `docker compose down -v` (borra el volumen de Postgres con todos los datos).
- Tocar la EC2, AWS, DuckDNS o el `.env` del servidor.
- Cambiar versiones de Java, Spring Boot o Postgres.
- Agregar dependencias nuevas al `pom.xml` (propón y espera confirmación).
- Borrar o renombrar clases de otro dueño (ver sección "Dueños").

## Dueños

| Parte | Dueño |
|---|---|
| `domain/model`, `domain/health/` (State, Chain, Composite), `domain/port/` (menos `AlertObserver`), `application/`, `adapter/out/persistence`, `adapter/in/web` (menos `IngestionController`), migraciones, `config/`, `infra/` | Josuram |
| `adapter/in/ingestion/` (Factory Method, Adapter), `IngestionController`, `domain/notification/` + `adapter/out/notification/` (Observer), WebSocket | Compañero |
| `docs/contracts/`, `HealthEvent`, `ProcessEventUseCase`, `AlertObserver` | Los dos |

## Herramientas que puede usar el agente

- Leer y editar archivos del repo.
- Correr `./mvnw` (compilar y tests) y `docker compose -f infra/docker-compose.dev.yml`.
- `git status`, `git diff`, `git log`, crear ramas y commits locales.
- **No** hacer push ni abrir PRs sin que el usuario lo pida.
