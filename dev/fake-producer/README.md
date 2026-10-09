# Fake event producer

This development-only script continuously sends one randomized `BEHAVIOR`,
`AUDIO`, and `WEIGHT` event to `POST /api/v1/ingestion/events`. It uses the
shared event contract and requires the backend's `X-API-Key`.

For a local backend using its development defaults, run from the repository
root:

```powershell
$env:API_KEY = "dev-key"
python dev/fake-producer/fake_producer.py
```

For a remote backend, set `BACKEND_URL` to its HTTPS base URL and `API_KEY` to
the configured local secret. `CAGE_ID` defaults to `cage-1`, and
`INTERVAL_SECONDS` defaults to `5`. The producer never sends an API key over
plain HTTP to a non-local host. Press Ctrl+C to stop it.

The event generator and HTTP request can be checked without a running backend:

```powershell
python -m unittest discover -s dev/fake-producer -p "test_*.py"
```
