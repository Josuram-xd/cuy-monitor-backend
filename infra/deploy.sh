#!/usr/bin/env bash
# Deploys the whole stack on the EC2 (Debian): pulls the 4 repos, builds, starts, waits for health.
#
#   ~/cuy-monitor-backend/infra/deploy.sh                       # everything from main
#   BACKEND_REF=feature/x AI_REF=feature/y ./deploy.sh          # try other branches
#
# The 4 repos live side by side in the home folder. infra/.env must exist (see .env.example).
set -euo pipefail

main() {
  local infra_dir base_dir
  infra_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  base_dir="$(cd "$infra_dir/../.." && pwd)"
  local org="${GIT_ORG:-https://github.com/Josuram-xd}"

  [ -f "$infra_dir/.env" ] || { echo "Missing $infra_dir/.env (copy .env.example and fill it in)"; exit 1; }

  ensure_swap

  sync_repo "$org" "$base_dir" cuy-monitor-backend   "${BACKEND_REF:-main}"
  sync_repo "$org" "$base_dir" cuy-monitor-db        "${DB_REF:-main}"
  sync_repo "$org" "$base_dir" cuy-monitor-dashboard "${DASHBOARD_REF:-main}"
  sync_repo "$org" "$base_dir" cuy-monitor-ai-service "${AI_REF:-main}"

  # the sync may have replaced this very script: run the rest from the fresh copy
  if [ -z "${CUY_DEPLOY_REEXEC:-}" ]; then
    CUY_DEPLOY_REEXEC=1 exec "$infra_dir/deploy.sh" "$@"
  fi

  cd "$infra_dir"
  "$infra_dir/refresh-db-password.sh" --no-restart
  install_timer "$infra_dir"

  docker compose up -d --build --remove-orphans
  # Git replaces the Caddyfile (new inode) and a single-file bind mount keeps showing the old one:
  # a restart (a few seconds, the certificate lives in a volume) makes Caddy read the new file
  docker compose restart caddy
  docker compose ps
  wait_for_health "$infra_dir"
  docker image prune -f >/dev/null
}

# 2 GB swap: building the Java image on a 2 GB machine needs it
ensure_swap() {
  # /proc/swaps: swapon is in /usr/sbin, not in a normal user's PATH
  if [ "$(awk 'NR>1' /proc/swaps | wc -l)" -eq 0 ]; then
    echo "Creating a 2 GB swap file"
    sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile >/dev/null
    sudo swapon /swapfile
    grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
  fi
}

sync_repo() {
  local org="$1" base="$2" name="$3" ref="$4"
  if [ ! -d "$base/$name/.git" ]; then
    git clone --quiet "$org/$name.git" "$base/$name"
  fi
  git -C "$base/$name" fetch --quiet origin
  # the server never edits code: it always ends up exactly on origin/<ref>
  git -C "$base/$name" checkout --quiet -B "$ref" "origin/$ref"
  echo "$name -> $ref ($(git -C "$base/$name" rev-parse --short HEAD))"
}

install_timer() {
  # the RDS master password rotates by itself every 7 days: pick the new one up every night
  # (a systemd timer: a minimal Debian has no cron)
  sudo tee /etc/systemd/system/cuy-refresh-db.service >/dev/null <<UNIT
[Unit]
Description=Copy the rotated RDS password into the stack .env

[Service]
Type=oneshot
User=$USER
ExecStart=$1/refresh-db-password.sh
UNIT
  sudo tee /etc/systemd/system/cuy-refresh-db.timer >/dev/null <<UNIT
[Unit]
Description=Daily RDS password refresh

[Timer]
OnCalendar=*-*-* 04:17:00
Persistent=true

[Install]
WantedBy=timers.target
UNIT
  sudo systemctl daemon-reload
  sudo systemctl enable --now cuy-refresh-db.timer >/dev/null
}

wait_for_health() {
  local domain
  domain="$(grep '^DOMAIN=' "$1/.env" | cut -d= -f2-)"
  echo "Waiting for https://$domain/actuator/health ..."
  for _ in $(seq 1 40); do
    if curl -fsS --max-time 5 --resolve "$domain:443:127.0.0.1" "https://$domain/actuator/health" 2>/dev/null | grep -q '"UP"'; then
      echo "Backend is UP behind HTTPS"
      if curl -fsS --max-time 5 --resolve "$domain:443:127.0.0.1" "https://$domain/ai/health" 2>/dev/null | grep -q '"UP"'; then
        echo "AI service is UP"
      else
        echo "AI service is not answering yet (check: docker compose logs ai-service)"
      fi
      return 0
    fi
    sleep 5
  done
  echo "The backend did not become healthy in time. Logs:"
  docker compose logs --tail=60 migrate backend caddy
  return 1
}

main "$@"
exit
