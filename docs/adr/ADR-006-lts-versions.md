# ADR-006: LTS versions, no betas, pinned tags

**Status:** Accepted

**Context:** The project must still build and run at the final presentation, months after it started, on a
fresh machine.

**Decision:** Java 25 LTS, Node 24 LTS, Python 3.14, PostgreSQL 18, Spring Boot 4.1; no beta or release-candidate
dependencies. Docker images always with an exact tag (`postgres:18`, `flyway/flyway:11.20.3-alpine`,
`caddy:2.11-alpine`), never `:latest`.

**Consequences:**
- A build from the same commit gives the same result months later.
- Upgrades are a deliberate PR, not something that happens on the next pull.
- Some libraries arrived late for the newest versions (TensorFlow has no Python 3.14 build): they are only used
  outside the service, in Colab.
