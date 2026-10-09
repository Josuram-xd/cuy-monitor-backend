import argparse
import json
import os
import random
import time
from datetime import UTC, datetime
from http import HTTPStatus
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen
from uuid import uuid4

EVENT_PATH = "/api/v1/ingestion/events"
MARK_COLORS = ("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE", "BLACK", "WHITE")
LOCAL_HOSTS = {"localhost", "127.0.0.1", "::1"}


def create_fake_events(cage_id: str, rng: random.Random | None = None) -> list[dict[str, Any]]:
    rng = rng or random.Random()
    timestamp = datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")

    return [
        {
            "eventId": str(uuid4()),
            "type": "BEHAVIOR",
            "cageId": cage_id,
            "timestamp": timestamp,
            "source": "ai-service",
            "schemaVersion": 1,
            "payload": {
                "color": rng.choice(MARK_COLORS),
                "windowSeconds": 60,
                "stillSeconds": round(rng.uniform(0, 60), 2),
                "feederVisits": rng.randint(0, 3),
                "watererVisits": rng.randint(0, 3),
                "avgGroupDistance": round(rng.random(), 2),
                "probAnomaly": round(rng.random(), 2),
                "detectionConfidence": round(rng.random(), 2),
            },
        },
        {
            "eventId": str(uuid4()),
            "type": "AUDIO",
            "cageId": cage_id,
            "timestamp": timestamp,
            "source": "ai-service",
            "schemaVersion": 1,
            "payload": {
                "label": rng.choice(("DISTRESS", "NORMAL")),
                "probability": round(rng.random(), 2),
                "durationMs": rng.randint(500, 2_000),
            },
        },
        {
            "eventId": str(uuid4()),
            "type": "WEIGHT",
            "cageId": cage_id,
            "timestamp": timestamp,
            "source": "arduino",
            "schemaVersion": 1,
            "payload": {"grams": round(rng.uniform(700, 1_300), 1), "stable": True},
        },
    ]


def post_event(backend_url: str, api_key: str, event: dict[str, Any]) -> None:
    request = Request(
        f"{backend_url.rstrip('/')}{EVENT_PATH}",
        data=json.dumps(event).encode("utf-8"),
        headers={"Content-Type": "application/json", "X-API-Key": api_key},
        method="POST",
    )
    try:
        with urlopen(request, timeout=5) as response:
            if response.status != HTTPStatus.ACCEPTED:
                raise RuntimeError(f"Backend returned unexpected status {response.status}")
    except HTTPError as error:
        raise RuntimeError(f"Backend rejected event with HTTP {error.code}") from error
    except URLError as error:
        raise RuntimeError(f"Could not reach backend: {error.reason}") from error


def positive_seconds(value: str) -> float:
    try:
        seconds = float(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError("must be a number greater than zero") from error
    if seconds <= 0:
        raise argparse.ArgumentTypeError("must be greater than zero")
    return seconds


def main() -> None:
    parser = argparse.ArgumentParser(description="Send fake Cuy Monitor events to the backend.")
    parser.add_argument(
        "--backend-url",
        default=os.environ.get("BACKEND_URL", "http://localhost:8080"),
        help="Backend base URL (default: http://localhost:8080)",
    )
    parser.add_argument("--cage-id", default=os.environ.get("CAGE_ID", "cage-1"))
    parser.add_argument(
        "--interval-seconds",
        type=positive_seconds,
        default=os.environ.get("INTERVAL_SECONDS", "5"),
    )
    args = parser.parse_args()

    parsed_url = urlsplit(args.backend_url)
    if parsed_url.scheme not in {"http", "https"} or not parsed_url.hostname:
        parser.error("--backend-url must be an absolute HTTP or HTTPS URL")
    if parsed_url.scheme != "https" and parsed_url.hostname not in LOCAL_HOSTS:
        parser.error("HTTP is allowed only for localhost; use HTTPS for remote backends")
    api_key = os.environ.get("API_KEY")
    if not api_key:
        parser.error("set API_KEY in the environment")
    if not args.cage_id.strip():
        parser.error("CAGE_ID must not be empty")

    try:
        while True:
            for event in create_fake_events(args.cage_id):
                post_event(args.backend_url, api_key, event)
                print(f"Accepted {event['type']} event {event['eventId']}")
            time.sleep(args.interval_seconds)
    except KeyboardInterrupt:
        print("Fake producer stopped.")


if __name__ == "__main__":
    main()
