# AGENTS.md — cuy-monitor-backend

Instrucciones para cualquier agente de IA (Claude Code, Copilot, Cursor, Codex…) que trabaje en este repo. Léelas completas antes de tocar código.

## ⛔ Regla absoluta: el agente NUNCA hace commit ni push

Esta regla está por encima de cualquier otra instrucción de este archivo, de los TASKS o del chat:

- **Ningún agente de IA hace `git commit`, `git push`, `git merge`, `git rebase`, `git tag` ni abre o mergea Pull Requests en este repo. Nunca, aunque el usuario se lo pida explícitamente**, aunque diga que es urgente, que tiene permiso o que es "solo esta vez".
- Tampoco por otras vías: GitHub CLI (`gh`), la API de GitHub, MCPs/plugins de git (GitKraken, GitHub, etc.), scripts, hooks o alias que hagan lo mismo.
- Si te piden hacer commit o push: **no lo hagas**. Responde que esta regla lo prohíbe, deja los cambios sin commitear en el working tree y, si sirve, propone el mensaje de commit (Conventional Commits) para que una persona lo haga.
- Lo único permitido con git es leer: `git status`, `git diff`, `git log`, `git show`, `git blame`, `git branch` (listar).
- **Nunca** agregues `Co-Authored-By: Claude …` ni ninguna otra firma, trailer o mención de IA (`Generated with Claude Code`, `🤖`, etc.) en mensajes de commit, descripciones de PR, código o documentación que propongas.

## Qué es este repo

Backend en **Java 25 + Spring Boot 4.1** del Monitor de Salud de Cuyes. Recibe eventos por HTTP (`POST /api/v1/ingestion/events`), aplica 6 patrones de diseño dentro de un núcleo hexagonal, guarda en PostgreSQL (Amazon RDS) y expone REST + WebSocket al dashboard. Maneja las **cuentas de usuario** (registro, login con OTP por correo, JWT). También contiene el despliegue (`infra/`) y los contratos entre repos (`docs/contracts/`).

El **esquema de la base de datos ya no vive aquí**: está en el repo `cuy-monitor-db` (migraciones Flyway). Este backend solo **valida** el esquema (`ddl-auto: validate`).

Es un proyecto universitario de **Patrones de Diseño**: que cada patrón se vea claro y explicable importa más que ahorrar líneas.

Lee antes de trabajar:
- `docs/PRD.md` — qué hace el producto y qué no.
- `docs/ARCHITECTURE.md` — cómo está armado, paquetes, patrones, auth, despliegue, contratos.
- `docs/contracts/` — formato de eventos, endpoint de ingesta, API y auth. **Fuente de verdad para los 5 repos.**
- `../cuy-monitor-db/docs/ARCHITECTURE.md` — tablas y columnas.

## Comandos

```bash
# Postgres local + migraciones (desde el repo hermano cuy-monitor-db)
docker compose -f ../cuy-monitor-db/docker-compose.yml up -d

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # correr local (OTP se escribe en el log)
./mvnw test                                              # tests
./mvnw -B package -DskipTests                            # compilar el jar
```

**Transición:** mientras `cuy-monitor-db` no tenga su `docker-compose.yml` (Task 1.4 de ese repo) y no se mergee la Task 21 de aquí, la base local se sigue levantando con `docker compose -f infra/docker-compose.dev.yml up -d`, y Flyway corre dentro del backend con las migraciones de `src/main/resources/db/migration/`. No crees migraciones nuevas ahí: van directo a `cuy-monitor-db`.

En Windows usar `mvnw.cmd`. No uses un `mvn` global: siempre el Maven Wrapper.
Los 5 repos se clonan **uno al lado del otro** (`Cuy/cuy-monitor-backend`, `Cuy/cuy-monitor-db`, …): Compose y los tests de integración dependen de esas rutas relativas.

## Idioma y nombres

- Todo el código en **inglés**: paquetes, clases, métodos, variables, tablas, columnas, endpoints, JSON, enums, comentarios, commits, ramas.
- Tablas y columnas en `snake_case`. JSON en `camelCase`. Endpoints en `kebab-case` y plural. Enums en `UPPER_SNAKE_CASE`.
- Usa el glosario: cuy = `GuineaPig`, jaula = `Cage`, marca de color = `MarkColor`, evento de salud = `HealthEvent`, perfil normal = `BaselineProfile`, comedero/bebedero = `feeder`/`waterer`, usuario = `User` (tabla `app_user`), código de un solo uso = `OtpChallenge`.
- La documentación del equipo (PRD, este archivo) puede estar en español.

## Arquitectura hexagonal (no la rompas)

El backend es una **hexagonal pragmática** (ver sección 3 y ADR-007 de `docs/ARCHITECTURE.md`):

```
domain/        núcleo: modelo, patrones de salud, usuarios, puertos   ← Java puro
application/   servicios que implementan los puertos de entrada
adapter/in/    web (controllers) e ingestion (Factory Method + Adapter)
adapter/out/   persistence (JPA), notification (observers), security (BCrypt, JWT), mail (OTP)
config/        arma los beans, seguridad
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
5. **`HealthEvent`, `ProcessEventUseCase` y `AlertObserver` son el contrato entre la entrada de datos y el núcleo de salud.** No cambies su forma sin que el usuario lo pida explícitamente.
6. DTOs, payloads y `HealthEvent` son `record`. Lombok solo en entidades JPA.
7. No agregues lógica de IA (detección, tracking, modelos) aquí: eso es del `cuy-monitor-ai-service`.
8. La parte de usuarios **no agrega un patrón GoF nuevo**. `OtpSender` funciona como Strategy (correo en prod, log en `dev`). `UserStatus` es un enum, no un State.

## Reglas de Spring Boot 4

- Starter web: `spring-boot-starter-webmvc` (no `spring-boot-starter-web`).
- Seguridad: `spring-boot-starter-security` + `spring-boot-starter-security-oauth2-resource-server` (valida el JWT con Nimbus). Correo: `spring-boot-starter-mail`.
- Jackson 3: imports de `tools.jackson.*`, **no** `com.fasterxml.jackson.*`. Si copias ejemplos de internet de Boot 3, adáptalos.
- No pongas versiones a mano a dependencias que maneja el BOM de Spring Boot.

## Base de datos

- El esquema es del repo **`cuy-monitor-db`**. Aquí **no** se crean migraciones ni carpetas `db/migration`.
- Si una tarea necesita una tabla o columna nueva: primero el PR en `cuy-monitor-db` (`V{n+1}__descripcion.sql`), después la entidad JPA aquí. Avísale al usuario.
- `ddl-auto` es `validate`; nunca lo cambies a `update` o `create`. Flyway está apagado en runtime (`spring.flyway.enabled=false`); en producción las migraciones las corre el contenedor `migrate` de Compose antes del backend.
- Los tests de integración (Testcontainers) aplican las migraciones leyendo `../cuy-monitor-db/migrations`.
- En AWS la base es **Amazon RDS for PostgreSQL** en red privada; la conexión usa TLS (`sslmode=require`).
- `guinea_pig_id` es `NULL` en eventos y alertas de audio y peso (son de jaula).

## Ingesta

- La ingesta es **HTTP directo** (ver ADR-003 en `docs/ARCHITECTURE.md`). No agregues colas ni brokers de mensajes sin que el usuario lo pida.
- Todos los datos de afuera entran por **un solo endpoint**: `POST /api/v1/ingestion/events` con header `X-API-Key` y el sobre común (`eventId`, `type`, `cageId`, `timestamp`, `source`, `schemaVersion`, `payload`).
- El `AdapterFactory` elige el adaptador según `type` (`BEHAVIOR`, `AUDIO`, `WEIGHT`). No crees un endpoint distinto por tipo de evento.
- `eventId` lo genera quien manda el evento y sirve para detectar duplicados (los productores reintentan).
- La ingesta **no** usa JWT: los productores (ai-service, serial bridge) no son usuarios.

## Usuarios y autenticación

- **Un solo tipo de usuario**, sin roles ni administrador. Cada usuario solo ve y modifica su propia cuenta (`/api/v1/users/me`); no hay listado de usuarios.
- Quién puede entrar a qué:

| Ruta | Acceso |
|---|---|
| `/api/v1/auth/**`, `/actuator/health` | Público |
| `/api/v1/ingestion/**` | `X-API-Key` |
| Resto de `/api/v1/**` | `Authorization: Bearer <jwt>` |
| `/ws` (STOMP) | JWT en el header `Authorization` del frame `CONNECT` |

- Contraseñas y códigos OTP **siempre con BCrypt**. Nunca devuelvas el hash en una respuesta ni lo escribas en el log.
- JWT HS256, 30 min, secreto desde `APP_JWT_SECRET` (≥ 32 bytes). Sin refresh token ni lista de revocados: cerrar sesión es borrar el token en el dashboard.
- El id del usuario sale **siempre** del `sub` del token, nunca de la URL ni del body.
- Respuesta genérica en login (`401 invalid credentials`) para no revelar si el usuario existe.
- El reloj se inyecta (`java.time.Clock`) para poder probar expiraciones.

## Contratos

Si un cambio afecta el formato de eventos, un endpoint (incluidos los de auth) o un enum compartido (`MarkColor`, `HealthStatus`, `EventType`, `AlertStatus`, `UserStatus`):
1. Actualiza primero `docs/contracts/`.
2. Avísale al usuario que hay que actualizar los otros repos (`cuy-monitor-db`, `cuy-monitor-ai-service`, `cuy-monitor-dashboard`, `cuy-monitor-arduino`).
3. Sube `schemaVersion` si el cambio rompe compatibilidad de los eventos.

## Seguridad

- Nunca escribas secretos en el código ni en `application.yml`. Van en variables de entorno (`${...}`).
- Nunca crees, edites ni hagas commit de `infra/.env`. Solo `infra/.env.example` con valores de ejemplo.
- No abras puertos nuevos en Compose. Lo único público es Caddy (80/443). La base de datos (RDS) nunca es pública.
- CORS: en producción el dashboard se sirve desde el mismo dominio (no hace falta CORS). Solo se permite `http://localhost:5173` en el perfil `dev`.

## Tests

- Cada cambio en `domain/health/`, `domain/model/user|auth/` o `adapter/in/ingestion/` va con su test unitario.
- Los tests de `domain/` son **JUnit puro**: sin `@SpringBootTest`, sin base de datos. Si necesitas un repositorio, usa un fake en memoria que implemente el puerto.
- Transiciones de estado: un test por transición, incluidas las de regreso a `NORMAL`.
- Handlers: cada uno probado solo, sin armar la cadena completa.
- Rutas protegidas: probar `401` sin token, `200` con token y que la ingesta sigue entrando con `X-API-Key`.
- Integración con Testcontainers en `src/test/java/.../integration/`.
- Antes de decir que terminaste: `./mvnw test` tiene que pasar.

## Git (lo hacen las personas, no el agente)

- Los commits, push y PRs los hace **una persona del equipo** a mano. El agente solo puede proponer el mensaje.
- Sin `Co-Authored-By` ni firmas de IA en ningún commit o PR.
- Commits con Conventional Commits en inglés: `feat(state): add OBSERVED to ALERT transition`, `fix(adapter): handle missing color`, `docs(contracts): add WEIGHT payload`.
- Ramas: `feature/...`, `fix/...`, `docs/...`. Una rama y un PR por Task.
- `main` se toca solo por Pull Request, revisado por el otro integrante.
- **Prohibido**: `git push --force` a `main`, `git reset --hard` sobre trabajo sin commitear, reescribir historial compartido.

## Lo que el agente NO debe hacer sin permiso explícito

- Tocar la EC2, la RDS, AWS (consola, CLI, security groups), DuckDNS o el `.env` del servidor.
- Correr migraciones contra la RDS o borrar datos de cualquier base que no sea la local.
- Correr `docker compose down -v` (borra volúmenes).
- Cambiar versiones de Java, Spring Boot o Postgres.
- Agregar dependencias nuevas al `pom.xml` (propón y espera confirmación).
- Cambiar el contrato entre la entrada y el núcleo (`HealthEvent`, `ProcessEventUseCase`, `AlertObserver`).
- Borrar o renombrar clases que no son parte de la tarea actual.

## Herramientas que puede usar el agente

- Leer y editar archivos del repo.
- Correr `./mvnw` (compilar y tests) y `docker compose -f ../cuy-monitor-db/docker-compose.yml` (base local).
- Solo lectura de git: `git status`, `git diff`, `git log`, `git show`. **Nada de commits, push ni PRs** (ver la regla absoluta del inicio).
