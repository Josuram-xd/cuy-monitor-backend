# Traspaso de trabajo — 20 de noviembre de 2026

Estado del sistema y trabajo pendiente para que otra persona o agente lo continúe. Los cinco repos viven juntos en
`C:\Users\amand\Documents\cuy\` (backend, ai-service, dashboard, db, arduino).

## Reglas que hay que seguir

- **Ningún commit ni PR puede llevar `Co-Authored-By`, "Generated with Claude Code" ni ninguna mención de IA.** Los commits van solo a nombre del usuario configurado en git (`josuram`). Es la regla principal.
- Nunca trabajar en `main`: rama por cambio, **muchos commits pequeños** (uno por cambio lógico), Conventional Commits en inglés, push y PR hacia `main` con descripción en español (qué cambia, por qué, cómo se probó).
- Prohibido: `merge`, `rebase`, `reset --hard`, `push --force`, push directo a `main`, mergear PRs (los mergea el dueño), `--no-verify`.
- El `AGENTS.md` de ai-service, dashboard y arduino prohíbe que un agente haga commits. El dueño lo autorizó explícitamente en esta sesión con la condición de que no haya coautoría de IA. Los subagentes se niegan igual: ellos dejan los cambios sin commitear y un archivo con la lista de commits, y el agente principal hace los commits.
- Código, identificadores y comentarios en inglés. Los textos que ve el usuario en la app van en español, o en inglés si el usuario elige ese idioma (i18n).
- Nunca subir secretos. Viven solo en archivos `.env` locales que git ignora (ver abajo).

## Hecho en esta sesión

| Qué | Dónde | Estado |
|---|---|---|
| 22 patrones GoF en el sistema (Flyweight, Memento, Decorator, Visitor, Prototype, Iterator, Command, Mediator, etc.) | backend #69, ai-service #24, dashboard #64, arduino #10 | mergeados |
| Comentarios de config en inglés | db #29 | mergeado |
| Calibración de la báscula (factor 426.7 en EEPROM, error −6.6 a +0.2 g) | arduino #11, `docs/calibration.md` | mergeado |
| Soporte de cámaras RTSP (TP-Link Tapo C200) en el edge agent | ai-service #25 | **abierto** |
| Presentación de la exposición (12 diapositivas, estilo de la app) | `C:\Users\amand\Downloads\presentacion-cuyes\index.html` (+ carpeta `img/`) | lista, fuera de los repos |

Probado en vivo contra producción (`https://cuymonitor.duckdns.org`):
- `/actuator/health` y `/ai/health` responden UP. La IA está en modo real con los tres modelos cargados.
- **Báscula:** Arduino UNO en `COM3` con el firmware `weight_sensor` → `serial_bridge` → `POST /api/v1/ingestion/events`, respuesta `202`.
- **Cámara Tapo C200:** `stream2` da H.264 de 1280×720 y audio G.711 de 8 kHz → `edge_agent` → `/ai/v1/frames` y `/ai/v1/audio`, respuesta `202`.

## Configurar la cámara desde la app — programado, falta probar y mergear

PRs (mergear en este orden): db #30 (V20) → backend #71 → ai-service #25 (RTSP) → ai-service #26 (va contra
`feature/rtsp-camera`) → dashboard #65. Ninguno pasó por la revisión de un agente Sonnet; solo corrieron los tests de
cada repo. Falta: `./mvnw test` con Docker (con la rama de db activa), desplegar y probar de punta a punta (paso 5).
`edge_agent/.env` local ya tiene `BACKEND_URL`. Con `VITE_USE_MOCKS=true` el panel no aparece, porque el mock
de salud no simula el equipo conectado.

### Detalle del diseño

El diseño está en `C:\Users\amand\Documents\cuy\CAMERA_SETUP_PLAN.md`, fuera de los repos; copiarlo si hace falta. Sus decisiones finales están al final del archivo:
- La cámara se configura **por equipo** (fila `hardware_station`, migración `V20__add_camera_to_hardware_station.sql`).
- La contraseña se cifra con AES-256-GCM, con una clave derivada de `APP_API_KEY`, y nunca se devuelve al dashboard.
- El edge agent consulta `GET /api/v1/ingestion/station/camera` cada 15 s, cambia de cámara sin reiniciarse y reporta el estado.
- El dashboard tiene el panel `CameraSetup` en la vista de jaula, con un tutorial de la app Tapo y de IP Webcam en es y en.
- Calidad por defecto: `stream2`.

La rama `feature/camera-setup` ya existe en los cuatro repos. En ai-service sale de `feature/rtsp-camera`, porque el PR #25 todavía no se ha mergeado. Los subagentes dejan el código sin commitear y una lista de commits en:

```
C:\Users\amand\Documents\cuy\camera-commits-db.txt
C:\Users\amand\Documents\cuy\camera-commits-backend.txt
C:\Users\amand\Documents\cuy\camera-commits-ai-service.txt
C:\Users\amand\Documents\cuy\camera-commits-dashboard.txt
```

Cada línea es: `mensaje del commit<TAB>archivo1 archivo2 ...`, en orden.

Pasos para terminarlo:
1. Revisar en cada repo con `git status` y `git diff` que los cambios estén completos (comparar con el plan).
2. Correr las pruebas:
   - backend: `./mvnw test`, con Docker abierto para los tests de integración, que leen `../cuy-monitor-db/migrations` (la rama de db con la V20 tiene que estar activa).
   - ai-service: `pytest` y `ruff check .`. Hay un E501 en `edge_agent/uploader.py` que ya existía.
   - dashboard: `npm run build`, `npm run lint` y `npm run test`. `AuthProvider.test.tsx` a veces falla de forma intermitente; si pasa, repetir.
3. Hacer los commits desde cada lista, por ejemplo con:
   ```bash
   while IFS=$'\t' read -r msg files; do git add -- $files && git commit -m "$msg"; done < ../camera-commits-<repo>.txt
   ```
   Al terminar, `git status` tiene que quedar limpio. Revisar que ningún commit tenga atribución de IA:
   `git log main..HEAD --format=%B | grep -iE "co-authored|claude|anthropic"` no debe imprimir nada.
4. Push y PR de cada repo. En ai-service, el PR va contra `feature/rtsp-camera` o contra `main` si el #25 ya se mergeó. El orden para mergear es db → backend → ai-service y dashboard.
5. Prueba de punta a punta (sección 5 del plan): desde el dashboard guardar la Tapo (IP `10.154.224.245`, `stream2`) y ver que el estado pase a `CONNECTED`.

## Pendientes (de mayor a menor importancia)

1. **Terminar y subir la configuración de la cámara desde la app** (sección anterior).
2. **Mergear ai-service #25** (RTSP). Antes del merge no hay forma de usar la Tapo en producción.
3. **El firmware hace la tara al arrancar:** abrir el puerto serie reinicia el Arduino, que pone en cero lo que tenga encima. Si el bridge o la laptop se reinician con los cuyes sobre la plataforma, el cero queda mal. Propuesta: guardar el cero en la EEPROM, igual que el factor, y no hacer tara automática.
4. **Estabilidad del firmware (Task 7.4):** marca `"stable": true` aunque el peso suba de forma sostenida (unos 2 g cada 2 s). Hay que endurecer el criterio.
5. **Afinar la calibración** con el peso exacto de la botella, medido en una balanza de cocina. Hoy se supone de 312 g.
6. **Backend sin Docker:** los 7 tests de integración no se corrieron después de los cambios de patrones. Correr `./mvnw test` con Docker.
7. **Tests del firmware:** `test/run_tests.sh` necesita `g++`, que no está instalado en la laptop.
8. **Textos del backend:** unos 13 mensajes para el usuario están escritos en español dentro del código Java (`AlertMessages`, `EmailOtpSender`, etc.) y no cambian al poner la app en inglés.
9. **Cambiar los secretos** que se escribieron en el chat de esta sesión: la API key de ingesta (`APP_API_KEY` en el servidor y en los `.env` locales) y la contraseña de la cuenta de cámara en la app Tapo.

## Dónde está cada cosa en la laptop

| Qué | Ruta | En git |
|---|---|---|
| Config del bridge de peso (puerto, URL, API key, `cage-1`) | `cuy-monitor-arduino/serial_bridge/.env` | no (ignorado) |
| Config del edge agent (URL RTSP de la Tapo, URL de la IA, API key) | `cuy-monitor-ai-service/edge_agent/.env` | no (ignorado) |
| ffmpeg 9.1 (winget `Gyan.FFmpeg`) | en el `PATH` del usuario; abrir una terminal nueva para que se cargue | — |
| arduino-cli | `C:\Program Files\Arduino CLI\arduino-cli.exe` | — |

Para arrancar el sistema de la granja (cada uno en su terminal):

```bash
cd cuy-monitor-arduino/serial_bridge && python bridge.py   # con la plataforma vacía, por la tara al arrancar
cd cuy-monitor-ai-service && python -m edge_agent           # cámara Tapo por RTSP
```
