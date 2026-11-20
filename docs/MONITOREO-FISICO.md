# Prueba de la jaula física

## Preparación del servidor (una vez)

El despliegue incluye la migración V19 y ejecuta el servicio de IA en modo real. El equipo físico utiliza el identificador fijo `farm-station`. La persona que hace la demostración puede usar cualquier cuenta normal: en **Mis jaulas**, abre o crea una jaula propia, registra al cuy con el color de su marca y pulsa **Conectar equipo**. La conexión persiste al cerrar sesión. Para cambiar de cuenta, pulsa **Desconectar equipo** desde la jaula que lo tiene asignado. Solo una jaula puede usar el equipo a la vez.

La versión de demostración desplegada en la EC2 es `equipment-20261120-v1`. El archivo `docker-compose.override.yml` del servidor selecciona esas imágenes y habilita la estación en la IA. Antes del siguiente despliegue con reconstrucción, integra los cambios de backend, dashboard, IA y base de datos en las ramas de despliegue, incluida la migración V19. Conserva el identificador del equipo al configurar el portátil.

## Preparación del equipo (una vez)

1. Calibra la báscula, carga el firmware `weight_sensor` y verifica con un peso conocido que imprime gramos estables. Cierra el monitor serie. El Arduino hace tara al arrancar, así que deja la plataforma sin la carga de prueba al encenderlo.
2. En el Samsung A12 abre IP Webcam, habilita cámara y micrófono, elige 640 × 480 e inicia el servidor. Conecta el teléfono al cargador, permite la ejecución en segundo plano y reserva su dirección en el router. Desde el portátil abre `/shot.jpg` y `/audio.wav` en la dirección del teléfono. Comprueba que la marca del cuy se distinga en la imagen. Si el teléfono se reinicia, comprueba que IP Webcam vuelve a transmitir.
3. En el portátil Windows instala Python 3.14 y coloca los repositorios de Arduino, IA y backend como carpetas hermanas. Anota el puerto del Arduino en el Administrador de dispositivos. Con el equipo conectado a corriente, desactiva la suspensión automática mientras está enchufado.
4. Desde PowerShell como administrador, en la carpeta del backend, ejecuta el instalador con los dos valores de tu equipo. Por ejemplo:

```powershell
.\infra\install-farm-monitor.ps1 -PhoneUrl 'http://192.168.1.50:8080' -SerialPort 'COM3'
```

El instalador pide la contraseña de la cuenta de Windows para crear las tareas de arranque y la clave de ingesta del servidor para los dos agentes. Se introducen en el portátil. Comprueba que las tareas `CuyMonitor-Scale` y `CuyMonitor-CameraAudio` estén en ejecución. Si ya tienes los agentes instalados con NSSM, usa esos servicios y evita iniciar una segunda copia.

La cámara se configura desde el panel de la jaula (**Conectar equipo** → Cámara), no en el `.env`: elige el celular o la Tapo, escribe la IP y pulsa **Guardar y probar**. El agente toma el cambio sin reiniciarse. `-PhoneUrl` es opcional en el instalador: sin él, la cámara se toma del panel.

## Prueba del profesor

1. Abre la web, inicia sesión y entra en la jaula física. Pulsa **Iniciar medición**.
2. Coloca un peso conocido en la plataforma. La pantalla debe mostrar los gramos y una hora posterior al inicio de la medición.
3. Deja que la cámara observe al cuy marcado y mantén el micrófono activo. Espera una o dos ventanas de análisis. Cuando llegan tres señales, aparece **Medición completa**. Si falta alguna, su fila indica **Sin datos nuevos**.
4. Cierra la pestaña, espera un par de minutos y vuelve a abrir la jaula. Las horas de los sensores deben haber avanzado. El portátil, el Arduino, el teléfono y los servicios en el servidor deben permanecer encendidos.
5. La primera ventana anómala aceptada al pasar de normal a observado genera una alerta para ese cuy. Se publica en vivo y se guarda en el historial. Un clip de audio clasificado como angustia con suficiente confianza genera una alerta de jaula. Un animal tranquilo puede completar la medición sin generar alertas. Para demostrar una secuencia anómala artificial, usa una jaula de simulación separada.
