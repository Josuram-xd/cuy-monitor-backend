# ADR-004: Single EC2 with Docker Compose

**Status:** Accepted

**Context:** A pilot with one cage, a small team and AWS credits to last until December. The services need a
long-running host (STOMP WebSocket, models loaded once in memory).

**Decision:** Everything runs on one EC2 (t3.small) with Docker Compose behind Caddy (TLS, one domain:
`/` dashboard, `/api` and `/ws` backend, `/ai` AI service). The database is Amazon RDS (ADR-009).

**Consequences:**
- One machine is a single point of failure, accepted for a pilot. If it goes down, producers buffer and retry
  (ADR-003) and the dashboard shows "Sin datos", never "Normal".
- Deploying is `git pull` + `docker compose up -d --build` (also through SSM Run Command, without SSH).
- The EC2 can be stopped when the project is not being shown, which saves most of the cost.
- Moving to ECS Fargate later needs no code change: the same images (ADR-010).
