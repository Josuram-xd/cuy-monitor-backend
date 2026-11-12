# escalation-check

Drives the health escalation chain against a **local** backend and checks every step, so a change to
the handlers, the state machine or the thresholds shows up as a failed step instead of as a surprise
during a demo.

```bash
python dev/escalation-check/escalation_check.py
```

Exit code 0 means every step landed where the policy says it should.

## What it needs

1. The database of `cuy-monitor-db`: `docker compose up -d` in that repo (it brings the dev seed,
   which is where the `RED` guinea pig of `cage-1` comes from).
2. This backend on `:8080`:

   ```bash
   SPRING_PROFILES_ACTIVE=dev APP_JWT_SECRET=dev-only-secret-not-for-production-32b \
   APP_API_KEY=dev-key ./mvnw spring-boot:run
   ```

   `APP_JWT_SECRET` has no default and must be at least 32 bytes, so the application does not start
   without it.

## What it checks

| Step | Expected | Rule |
|---|---|---|
| 2 sick windows | `NORMAL` | `consecutive-anomaly-windows: 3` — one bad window moves nothing |
| window 3 | `OBSERVED` | A confirmed anomaly starts the watch at once |
| another, 0 min in OBSERVED | `OBSERVED` | The clock has to run first |
| another, 31 min in OBSERVED | `ALERT` | `observed-to-alert: 30m` |
| another, 0 min in ALERT | `ALERT` | The clock again |
| another, 61 min in ALERT | `CRITICAL` | `alert-to-critical: 60m` |
| alerts raised | 2 | Only a move up into ALERT or CRITICAL warns the breeder |
| 4 normal windows | `CRITICAL` | `recovery-windows: 5` |
| normal window 5 | `NORMAL` | Recovery goes straight back to NORMAL |

It ends by printing the rows of `state_transition`, which is the history the dashboard shows:

```
NORMAL -> OBSERVED     Anomaly in 3 consecutive windows
OBSERVED -> ALERT      Anomaly in 3 consecutive windows
ALERT -> CRITICAL      Anomaly in 3 consecutive windows
CRITICAL -> NORMAL     Behavior back to its usual range
```

## Why it rewrites the database

Escalation compares `now` against `guinea_pig.status_since`, both from the wall clock, so reaching
CRITICAL honestly would take 90 minutes. Instead of lowering the thresholds — which would test a
configuration nobody runs — the script back-dates `status_since` between windows. **The thresholds
under test are the ones in `application.yml`.**

It also deletes the transitions and alerts of that one guinea pig at the start, so every run begins
from the same place.

Because it deletes rows and rewrites timestamps, it **refuses any backend that is not localhost**.
There is no flag to override that.
