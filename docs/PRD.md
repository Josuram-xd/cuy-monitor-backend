# PRD — Monitor de Salud de Cuyes

> **Este es el PRD general del producto.** Los otros tres repos tienen un PRD corto con solo su parte y apuntan aquí.
> Dueños: Josuram + compañero · Curso: Patrones de Diseño · Última revisión: 26 de septiembre de 2026

---

## 1. Problema

En los criaderos pequeños de cuyes, las enfermedades se notan tarde. El cuy es un animal presa: esconde el dolor y el malestar hasta que el cuadro ya está avanzado. El criador revisa las jaulas un par de veces al día y a ojo, así que un cuy que dejó de comer o que se aísla del grupo puede pasar desapercibido uno o dos días. Para ese momento, muchas veces ya es tarde o ya contagió a otros.

## 2. Qué queremos lograr

Un sistema que **observe la jaula todo el día** y avise al criador cuando un cuy cambia su comportamiento de forma sostenida, para que lo revise antes.

El sistema **no diagnostica** enfermedades. Da una **alerta temprana**: "este cuy se está comportando distinto a lo normal, revíselo".

### Objetivos

| # | Objetivo | Cómo se mide |
|---|---|---|
| O1 | Detectar cambios de comportamiento sostenidos por cuy | Alerta generada en menos de 1 minuto después de confirmarse la anomalía |
| O2 | Evitar falsas alarmas | Máximo ~2 alertas falsas por jaula al día en la prueba real (a ajustar en la Fase 2) |
| O3 | Que el criador entienda el estado de un vistazo | En la vista principal se ve el estado de cada cuy sin tener que entrar a detalles |
| O4 | Funcionar con hardware reciclado y $0 en nube | Celular A12, laptop Celeron, Arduino; créditos de AWS |
| O5 | Aplicar 6 patrones de diseño en el backend Java | Factory Method, Adapter, State, Chain of Responsibility, Observer, Composite, cada uno con su código y diagrama |

### Lo que NO hace (fuera de alcance)

- No diagnostica enfermedades ni recomienda tratamientos.
- No reemplaza al veterinario.
- No identifica cuyes por la cara o el cuerpo: los identifica por una **marca de color en el lomo**.
- No asocia el peso a un cuy específico en esta versión (la lectura de peso es de la jaula).
- No maneja varias granjas ni usuarios con login (una sola jaula piloto).
- No manda notificaciones al celular (queda como mejora futura).

## 3. Usuarios

| Usuario | Quién es | Qué necesita |
|---|---|---|
| **Criador** | Persona que cuida los cuyes (en la prueba, la tía de uno de los integrantes) | Ver rápido si algún cuy está mal, recibir alertas, marcar que ya revisó |
| **Equipo técnico** | Los dos estudiantes | Instalar, calibrar, ver logs, ajustar umbrales |
| **Docente** | Evaluador del curso | Ver los patrones aplicados y el sistema desplegado funcionando |

## 4. Historias de usuario

| ID | Como… | Quiero… | Para… |
|---|---|---|---|
| HU-01 | criador | ver todos los cuyes de la jaula con su estado (Normal, En observación, Alerta, Crítico) | saber de un vistazo si alguno necesita atención |
| HU-02 | criador | recibir una alerta en pantalla apenas un cuy empeora | no tener que estar revisando todo el tiempo |
| HU-03 | criador | registrar un cuy con su nombre y el color de su marca | que el sistema lo reconozca |
| HU-04 | criador | ver el historial de un cuy (movimiento, visitas al comedero, estados) | entender desde cuándo está raro |
| HU-05 | criador | marcar una alerta como revisada | llevar control de lo que ya atendí |
| HU-06 | criador | ver el historial de peso de la jaula | notar si bajó el peso general |
| HU-07 | criador | recibir una alerta de jaula cuando hay chillidos de angustia | revisar si hay pelea, un depredador o dolor |
| HU-08 | equipo técnico | saber si cada servicio está vivo | detectar caídas rápido |

## 5. Requisitos funcionales

| ID | Requisito | Prioridad | Entrega |
|---|---|---|---|
| RF-01 | Detectar a cada cuy en la imagen y reconocerlo por su marca de color (`MarkColor`) | Alta | Final |
| RF-02 | Seguir a cada cuy entre frames sin confundir identidades | Alta | Final |
| RF-03 | Calcular por cuy, en ventanas de 60 s: tiempo quieto, visitas a comedero y bebedero, distancia al grupo | Alta | Final |
| RF-04 | Clasificar cada ventana como normal o anómala comparando con el perfil normal del cuy (`BaselineProfile`) | Alta | Final |
| RF-05 | Clasificar el audio de la jaula como normal o angustia y generar alerta de jaula | Media | Final |
| RF-06 | Recibir lecturas de peso del Arduino y guardarlas | Media | Final |
| RF-07 | Llevar el estado de salud por cuy: `NORMAL → OBSERVED → ALERT → CRITICAL`, y que pueda bajar de nuevo | Alta | Avance (esqueleto) / Final (reglas reales) |
| RF-08 | Confirmar que una anomalía es **sostenida** (N ventanas seguidas) antes de cambiar de estado | Alta | Final |
| RF-09 | Calcular el estado general de la jaula a partir de cuyes + audio + peso | Alta | Avance (esqueleto) / Final |
| RF-10 | Avisar en tiempo real al dashboard, guardar la alerta y dejarla en el log | Alta | Avance |
| RF-11 | Dashboard: vista de jaula, detalle de cuy, alertas, registro de cuy, peso | Alta | Avance (con datos falsos) / Final |
| RF-12 | Marcar alertas como revisadas | Media | Final |
| RF-13 | Toda la interfaz del criador en español | Alta | Avance |

## 6. Requisitos no funcionales

| Qué | Meta |
|---|---|
| Cuyes por jaula | 5 a 8 |
| Jaulas | 1 piloto, pero con `cageId` en todo para escalar sin reescribir |
| Frames analizados | 1–2 por segundo |
| Latencia de una alerta | < 1 minuto desde que se confirma la anomalía |
| Disponibilidad | Reinicio automático de contenedores (`restart: unless-stopped`) |
| Seguridad | Solo HTTPS hacia afuera; Postgres cerrado a internet; API key para ingesta |
| Costo | $0 en nube (créditos AWS); solo se compra el kit de Arduino |
| Idioma | Código en inglés; textos del criador en español |
| Bienestar animal | Nada del montaje puede estresar o lastimar a los cuyes (marcas no tóxicas, plataforma estable) |

## 7. Restricciones

- Backend **obligatoriamente en Java** (ahí viven los 6 patrones).
- Frontend **obligatoriamente en TypeScript** con React.
- Despliegue con **Docker en AWS**.
- Equipo de 2 personas.
- Hardware disponible: Samsung Galaxy A12 (cámara + micrófono), laptop Celeron 8 GB, Arduino Uno + HX711 + celda de carga.

## 8. Entregas

| Hito | Fecha | Qué debe funcionar |
|---|---|---|
| **Avance** | 30 de septiembre de 2026 | Backend desplegado en AWS con Docker, Postgres y HTTPS. Eventos falsos entrando por `POST /api/ingestion/events`. Esqueleto de patrones. Dashboard mostrando datos (puede ser con mocks). **Sin demo en vivo, solo desplegado.** |
| Datos y modelos | Octubre | Detector, tracker, clasificadores entrenados; Arduino y puente funcionando |
| Prueba en jaula real | 27 oct – 9 nov | Sistema corriendo varios días; ajuste de umbrales |
| **Entrega final** | Noviembre | Todo funcionando con datos reales, informe y sustentación |

### Estado al 26 de septiembre

- ✅ EC2 con Elastic IP, dominio `cuymonitor.duckdns.org`, HTTPS con Caddy.
- ✅ Postgres 18 y backend Spring Boot 4.1 corriendo en Docker Compose.
- ✅ Endpoint de ingesta `POST /api/ingestion/events` recibiendo eventos (por ahora solo los registra).
- 🔁 26 sept: se quitó Kafka; la carga de una jaula no lo justifica (ADR-007).
- ✅ Endpoints de prueba: `/actuator/health`, `/api/system/status`.
- ⏳ Patrones, API del dashboard, WebSocket, dashboard, ai-service mock.

## 9. Métricas de éxito (para el informe)

- Precisión del detector por color (mAP del modelo YOLO26n) en frames de prueba.
- Porcentaje de ventanas bien clasificadas (Random Forest) en datos etiquetados.
- Alertas verdaderas vs. falsas durante la prueba en la jaula real.
- Tiempo desde el evento hasta que aparece en el dashboard.
- Uso de RAM/CPU de la EC2 y costo real en créditos.

## 10. Supuestos

- La tía presta cuyes para la prueba; si no, se entrena primero con objetos de colores.
- El criadero tiene WiFi suficiente para subir 1–2 frames por segundo (si no, la IA pasa a la laptop, ver ADR-005).
- Las marcas de color se reaplican cada 1–2 semanas.

## 11. Riesgos principales

| Riesgo | Plan B |
|---|---|
| No se consiguen cuyes a tiempo | Entrenar con pompones de colores y reentrenar después |
| Internet del criadero malo | Correr el ai-service en la laptop |
| La EC2 se queda sin memoria | Heaps limitados + swap; subir a c7i-flex.large cuando entre la IA |
| Pocos audios de angustia | Tratarlo como "sonido anómalo" vs. normal |
| Se acaban los créditos | AWS Budgets + apagar la EC2 cuando no se use |

## 12. Documentos relacionados

- Arquitectura del backend: [`ARCHITECTURE.md`](ARCHITECTURE.md)
- Contratos entre repos: `docs/contracts/` (fuente de verdad)
- Reglas para agentes de IA: [`../AGENTS.md`](../AGENTS.md)
- PRD por componente: en `docs/PRD.md` de `cuy-monitor-ai-service`, `cuy-monitor-dashboard` y `cuy-monitor-arduino`
