# ADR-001: Multi-repo, contracts in the backend

**Status:** Accepted

**Context:** The product has five parts built by different people and with different stacks: the Spring Boot
backend, the React dashboard, the Python AI service, the Arduino firmware and its serial bridge, and the database
schema. Each has its own tooling, CI and release pace.

**Decision:** One repository per part (`cuy-monitor-backend`, `cuy-monitor-dashboard`, `cuy-monitor-ai-service`,
`cuy-monitor-arduino`, `cuy-monitor-db`). The contracts between them (event envelope, REST API, auth API, enums)
live in **this** repo, in `docs/contracts/`, and are the source of truth: when a client and a contract disagree, the
contract wins.

**Consequences:**
- Each repo builds, tests and deploys on its own; a change in the dashboard never rebuilds the backend.
- A change that crosses repos is several PRs in order (contract first, then producers and consumers).
- The AI service has a test that validates its payloads against the JSON examples of `docs/contracts/`.
- Integration tests need repos side by side (`../cuy-monitor-db/migrations`), and CI checks out both.
