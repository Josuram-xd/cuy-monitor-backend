#!/usr/bin/env bash
# Task 8.5: runs the whole Postman collection against a backend on this machine, with Newman.
#
#   1. docker compose -f ../cuy-monitor-db/docker-compose.yml up -d
#   2. APP_JWT_SECRET=<32+ chars> APP_COOKIE_SECURE=false ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
#        > /tmp/backend.log 2>&1 &
#   3. docs/postman/run-local.sh /tmp/backend.log
#
# The login code goes to the backend log with the dev profile, so the script reads it from there between the two
# folders. Against production, use Postman itself: run folder 1, copy the code from the email, run folder 2.
set -euo pipefail

log="${1:?usage: run-local.sh <backend log file>}"
here="$(cd "$(dirname "$0")" && pwd)"
environment="$(mktemp)"
trap 'rm -f "$environment"' EXIT
newman=(npx -y newman@6.2.1 run "$here/cuy-monitor.postman_collection.json" --reporters cli)

"${newman[@]}" -e "$here/local.postman_environment.json" --folder "1 Pedir código" --export-environment "$environment"
sleep 1
code="$(grep -oE 'OTP for [^ ]+: [0-9]{6}' "$log" | tail -1 | grep -oE '[0-9]{6}$' || true)"
if [[ -z "$code" ]]; then
  echo "No login code in $log: is the backend running with the dev profile?" >&2
  exit 1
fi
"${newman[@]}" -e "$environment" --env-var "otpCode=$code" --folder "2 Sesión y API"
