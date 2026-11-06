# ADR-007: Pragmatic hexagonal architecture inside the backend

**Status:** Accepted (2026-09-27)

**Context:** The backend hosts the six design patterns of the course. The health rules (Chain, State, Composite) must be easy to test and to explain, and the work splits naturally into data input vs. health core. Almost no code existed yet, so changing the structure was cheap.

| | Layered (previous) | Strict hexagonal | Pragmatic hexagonal (chosen) |
|---|---|---|---|
| Core testable without Spring/DB | Partly | Yes | Yes |
| Boilerplate | Low | High (commands, manual wiring, no framework anywhere near the core) | Medium (ports + JPA mappers) |
| Fit with the course patterns | Neutral | Good | Good: Adapter = input adapter, Observer = output port + adapters |
| Work split | By layer | By port | By port: input side / core |

**Consequences:**
- `domain` is pure Java; an ArchUnit test fails the build if it imports Spring, JPA or an adapter.
- ~15 extra interfaces (ports) and one mapper per table.
- Controllers depend on use case interfaces, so they can be tested with fakes.
- Replacing a technology (e.g. Postgres, WebSocket, adding a push-notification observer) means writing a new adapter, not touching the core.
