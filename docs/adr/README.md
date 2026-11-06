# Architecture decision records

| ADR | Decision |
|---|---|
| [ADR-001](ADR-001-multi-repo.md) | Multi-repo; the contracts live in the backend |
| [ADR-002](ADR-002-ml-in-its-own-service.md) | Machine learning in a separate Python service; the backend only knows events |
| [ADR-003](ADR-003-direct-http-ingestion.md) | Direct HTTP ingestion, no message queue |
| [ADR-004](ADR-004-single-ec2.md) | Single EC2 with Docker Compose |
| [ADR-005](ADR-005-ai-in-the-cloud-first.md) | AI in the cloud first |
| [ADR-006](ADR-006-lts-versions.md) | LTS versions, no betas, pinned Docker tags |
| [ADR-007](ADR-007-pragmatic-hexagonal.md) | Pragmatic hexagonal architecture inside the backend |

ADR-008 to ADR-011 (database repo, RDS, EC2 over Lambda, own user accounts) are in
[ARCHITECTURE.md, section 10](../ARCHITECTURE.md#10-architecture-decisions).
