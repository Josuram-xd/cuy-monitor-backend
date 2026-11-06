# Postman (Task 8.5)

Every endpoint of `docs/contracts/` with its checks: 32 requests, 48 assertions. The collection creates its own
cage and guinea pig and deletes them at the end, so it can run against any account.

| File | What |
|---|---|
| `cuy-monitor.postman_collection.json` | The collection |
| `local.postman_environment.json` | Backend on `localhost:8080`, the `dev` / `dev-password` account of the dev seed, `dev-key` |
| `production.postman_environment.json` | `https://cuymonitor.duckdns.org`; fill in a real account and the `API_KEY` (never commit them) |
| `run-local.sh` | Runs both folders with Newman against a local backend, reading the code from its log |

## Folders

1. **Pedir código**: the backend is up, nothing is open without a session, and the login sends a code.
2. **Sesión y API**: verify the code (the session arrives as cookies), account, catalog, cages, guinea pigs, the
   photo, history and weight, ingestion with `X-API-Key` (weight, behavior, a distress clip and a wrong key), the
   alert the distress clip opened, reviewing it, the alert history, the assistant (200, or 503 when it is off),
   and at the end deleting what it created, refreshing and closing the session.

## Running it

- **Postman**: import the collection and an environment, run folder 1, put the code in `otpCode`, run folder 2.
  Locally the code is in the backend log (`OTP for ...`); in production it arrives by email.
- **Local, one command**: see the header of `run-local.sh`. Run the backend with `APP_COOKIE_SECURE=false`: over
  plain `http://localhost` a `Secure` cookie is never sent back.

Last local run (2026-11-06, backend `main` at #50, dev seed): 32 requests, 48 assertions, 0 failures.
