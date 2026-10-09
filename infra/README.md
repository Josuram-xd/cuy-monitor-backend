# Despliegue en AWS (demo)

Todo corre en **una EC2** con Docker Compose detrás de **Caddy (HTTPS automático)**; la base es **Amazon RDS**; la IA usa **Amazon Bedrock** con el rol de la instancia.

```
Internet ── 443 ──► Caddy ──┬─ /api/*, /ws, /actuator/health ─► backend :8080 ──► RDS (TLS)
                            ├─ /ai/*                          ─► ai-service :8000 ──► Amazon Bedrock
                            └─ todo lo demás                  ─► dashboard (SPA estática)
```

| Pieza | Dónde / cuál |
|---|---|
| Dominio y HTTPS | `https://cuymonitor.duckdns.org`. Caddy pide y renueva el certificado solo (Let's Encrypt, puerto 80 abierto). |
| Servidor | EC2 `cuy-monitor-server` (Debian 13, `t3.small`) con la IP elástica del dominio. |
| Base de datos | RDS PostgreSQL 18 `cuy-monitor-db`. Esquema por el contenedor `migrate` (repo `cuy-monitor-db`). |
| IA | Contenedor `ai-service`, perfil `ai` de Compose, modelo de Bedrock (`BEDROCK_MODEL_ID`). |
| Cuenta AWS | `us-east-1`. Usuario IAM `cuy-admin` para administrar; **no** se usa el root. |

## Desplegar

Los 4 repos van uno junto al otro en la carpeta del usuario `admin` de la EC2.

```bash
ssh -i cuy-key-pair.pem admin@<IP o dominio>
~/cuy-monitor-backend/infra/deploy.sh                   # todo desde main
BACKEND_REF=feature/x AI_REF=feature/y ~/cuy-monitor-backend/infra/deploy.sh   # probar ramas
```

`deploy.sh` crea el swap de 2 GB si falta, actualiza los 4 repos (`git reset` exacto a `origin/<rama>`: el servidor no se edita), toma la contraseña vigente de la base, instala la tarea nocturna y hace `docker compose up -d --build --remove-orphans`. Termina esperando `https://<dominio>/actuator/health` y `/ai/health`.

### `infra/.env` (en el servidor, nunca en git; ver `.env.example`)

`DOMAIN`, datos de la base (`DB_*`, `DB_SECRET_ID`), `API_KEY`, `APP_JWT_SECRET`, y para los correos del código de verificación `MAIL_*` (cuenta de Gmail con **contraseña de aplicación**). Sin `MAIL_*` válidos nadie puede registrarse ni entrar: el código no llega.

La contraseña del usuario maestro de RDS **rota sola cada 7 días**; `refresh-db-password.sh` (cron a las 04:17) la copia de Secrets Manager al `.env` y reinicia el backend si cambió.

## Base de datos pública (solo para la demo)

`aws/setup.sh` deja la RDS con acceso público, pero el grupo de seguridad solo abre el 5432 a **una IP de administración** (y a la EC2). La conexión exige TLS:

```bash
PGPASSWORD="$(aws secretsmanager get-secret-value --secret-id 'rds!db-…' --query SecretString --output text | jq -r .password)" \
  psql "host=<endpoint> dbname=cuymonitor user=cuymonitor sslmode=require"
```

- Cambió tu IP: `aws ec2 authorize-security-group-ingress --group-id <sg de cuy-rds> --protocol tcp --port 5432 --cidr <tu-ip>/32`.
- **Cerrarla al terminar la demo:** `AWS_PROFILE=cuy infra/aws/close-db.sh`.

## IA con Bedrock

El contenedor no tiene claves de AWS: usa el **rol de la instancia** (`cuy-ec2-role`: invocar modelos de Bedrock, SSM y leer solo el secreto de la base). Para que el contenedor alcance las credenciales de la instancia, `setup.sh` sube el límite de saltos del servicio de metadatos a 2. Modelo por defecto: Amazon Nova 2 Lite (no pide formularios). Los modelos de Claude funcionan cuando se llena el formulario de uso de Anthropic en la consola de Bedrock; entonces basta cambiar `BEDROCK_MODEL_ID`.

## Operación

```bash
cd ~/cuy-monitor-backend/infra
docker compose ps                         # estado
docker compose logs -f backend            # logs (también: migrate, ai-service, caddy)
docker compose restart ai-service
aws ec2 stop-instances --instance-ids <id>   # apagar la EC2 cuando no se use (la IP elástica se conserva)
```

Costo aproximado en marcha: EC2 `t3.small` + RDS `db.t3.micro` + Bedrock por uso (≈ una llamada por minuto con cámara conectada). Apaga la EC2 y la RDS (`aws rds stop-db-instance`, hasta 7 días) cuando no se necesiten.
