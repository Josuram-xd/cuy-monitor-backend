# ADR-003: Direct HTTP ingestion

**Status:** Accepted

**Context:** One pilot cage produces ~8 behavior events per minute, a few audio events and one weight reading every 10 s. The EC2 has 2 GB of RAM.

| | Direct HTTP (chosen) | Message queue in the middle |
|---|---|---|
| Complexity | Low: one endpoint | Extra service, configuration, consumers |
| Memory | 0 extra | Hundreds of MB |
| If the backend is down | The producer retries | The queue buffers events |
| Enough for 1–few cages | Yes | Yes (overkill) |

**Consequences:**
- Producers retry with backoff on network errors / 5xx and keep a small bounded buffer.
- `eventId` is the idempotency key, so retries don't create duplicates.
- The ai-service calls the backend inside the Docker network; the laptop (serial bridge) calls it over HTTPS through Caddy with the API key. The database and the backend port are never exposed.
- If the system grows to many cages, a queue (e.g. Amazon SQS) can be put in front of the backend with the same envelope, without touching the health core.
