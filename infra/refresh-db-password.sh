#!/usr/bin/env bash
# Copies the current RDS master password from Secrets Manager into infra/.env (DB_PASSWORD).
# RDS rotates it every 7 days; without this the backend would lose the database after a rotation.
#   refresh-db-password.sh               # update .env and restart the backend if it changed
#   refresh-db-password.sh --no-restart  # only update .env (deploy.sh starts the stack itself)
set -euo pipefail

infra_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
env_file="$infra_dir/.env"
secret_id="$(grep '^DB_SECRET_ID=' "$env_file" | cut -d= -f2-)"
region="$(grep '^AWS_REGION=' "$env_file" | cut -d= -f2- || true)"
[ -n "$secret_id" ] || { echo "DB_SECRET_ID is not set in $env_file"; exit 1; }

# the instance role may read this one secret (infra/aws/setup.sh)
password="$(aws secretsmanager get-secret-value --region "${region:-us-east-1}" --secret-id "$secret_id" \
  --query SecretString --output text | python3 -c 'import sys, json; print(json.load(sys.stdin)["password"])')"
case "$password" in *\'*) echo "The password contains a quote; cannot store it in .env"; exit 1;; esac

current="$(grep '^DB_PASSWORD=' "$env_file" | cut -d= -f2- | sed "s/^'//; s/'\$//")"
if [ "$password" = "$current" ]; then
  echo "$(date -u +%FT%TZ) DB password unchanged"
  exit 0
fi

# single quotes: Docker Compose would otherwise expand $ and friends in the value
tmp="$(mktemp)"
grep -v '^DB_PASSWORD=' "$env_file" > "$tmp"
printf "DB_PASSWORD='%s'\n" "$password" >> "$tmp"
chmod 600 "$tmp" && mv "$tmp" "$env_file"
echo "$(date -u +%FT%TZ) DB password updated"

if [ "${1:-}" != "--no-restart" ]; then
  cd "$infra_dir" && docker compose up -d --no-deps backend
fi
