"""Drives the health escalation chain end to end against a LOCAL backend and checks every step.

    python dev/escalation-check/escalation_check.py

NORMAL -> OBSERVED -> ALERT -> CRITICAL -> NORMAL, over the real policy (3 anomalous windows in a
row to confirm an anomaly, 30 min to ALERT, 60 min to CRITICAL, 5 normal windows to recover)
without waiting 90 minutes: the escalation reads
`now - guinea_pig.status_since` from the wall clock, so between windows the script back-dates
status_since in the database instead of accelerating the configuration. The thresholds under test
are the ones the service really runs with.

It writes to the database and deletes rows, so it refuses any backend that is not local.

Needs: the db repo's `docker compose up -d` and the backend on :8080 (`./mvnw spring-boot:run`).
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import UTC, datetime
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen
from uuid import uuid4

EVENT_PATH = "/api/v1/ingestion/events"
LOCAL_HOSTS = {"localhost", "127.0.0.1", "::1"}
PG_CONTAINER = "cuy-monitor-db-postgres-1"
RECOVERY_WINDOWS = 5  # health.recovery-windows in application.yml
CONSECUTIVE_WINDOWS = 3  # health.consecutive-anomaly-windows: what confirms an anomaly

SICK = {
    "windowSeconds": 60, "stillSeconds": 57, "feederVisits": 0, "watererVisits": 0,
    "avgGroupDistance": 0.85, "probAnomaly": 0.9, "detectionConfidence": 0.92,
}
HEALTHY = {
    "windowSeconds": 60, "stillSeconds": 12, "feederVisits": 2, "watererVisits": 1,
    "avgGroupDistance": 0.25, "probAnomaly": 0.05, "detectionConfidence": 0.93,
}


class Failure(Exception):
    pass


def psql(sql: str) -> str:
    """One query against the LOCAL dev database, through the compose container."""
    result = subprocess.run(
        ["docker", "exec", PG_CONTAINER, "psql", "-U", "cuymonitor", "-d", "cuymonitor",
         "-tA", "-c", sql],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        raise Failure(f"psql failed: {result.stderr.strip() or result.stdout.strip()}")
    return result.stdout.strip()


def send_window(backend: str, api_key: str, cage: str, color: str, sick: bool) -> None:
    payload = dict(SICK if sick else HEALTHY, color=color)
    event = {
        "eventId": str(uuid4()),
        "type": "BEHAVIOR",
        "cageId": cage,
        "timestamp": datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source": "escalation-check",
        "schemaVersion": 1,
        "payload": payload,
    }
    request = Request(
        backend.rstrip("/") + EVENT_PATH,
        data=json.dumps(event).encode(),
        headers={"Content-Type": "application/json", "X-API-Key": api_key},
        method="POST",
    )
    try:
        with urlopen(request, timeout=15) as response:
            if response.status != 202:
                raise Failure(f"the backend answered {response.status}, expected 202")
    except HTTPError as error:
        hint = " (wrong --api-key?)" if error.code == 401 else ""
        raise Failure(f"the backend answered {error.code}{hint}") from error
    except URLError as error:
        raise Failure(f"no backend at {backend}: {error.reason}") from error


def status_of(cage: str, color: str) -> str:
    status = psql(
        "SELECT g.current_status FROM guinea_pig g JOIN cage c ON c.id = g.cage_id "
        f"WHERE c.code = '{cage}' AND g.mark_color = '{color}' AND g.active"
    )
    if not status:
        raise Failure(f"no active {color} guinea pig in {cage}: run the dev seed first")
    return status


def back_date_status_since(cage: str, color: str, minutes: int) -> None:
    psql(
        f"UPDATE guinea_pig g SET status_since = now() - interval '{minutes} minutes' "
        f"FROM cage c WHERE c.id = g.cage_id AND c.code = '{cage}' "
        f"AND g.mark_color = '{color}' AND g.active"
    )


def reset(cage: str, color: str) -> None:
    """Back to NORMAL with a clean history, so the run always starts from the same place."""
    psql(
        "DELETE FROM state_transition st USING guinea_pig g, cage c "
        f"WHERE st.guinea_pig_id = g.id AND c.id = g.cage_id AND c.code = '{cage}' "
        f"AND g.mark_color = '{color}'"
    )
    psql(
        "DELETE FROM alert a USING guinea_pig g, cage c "
        f"WHERE a.guinea_pig_id = g.id AND c.id = g.cage_id AND c.code = '{cage}' "
        f"AND g.mark_color = '{color}'"
    )
    psql(
        "UPDATE guinea_pig g SET current_status = 'NORMAL', status_since = now() "
        f"FROM cage c WHERE c.id = g.cage_id AND c.code = '{cage}' "
        f"AND g.mark_color = '{color}' AND g.active"
    )


def alerts_for(cage: str, color: str) -> int:
    return int(psql(
        "SELECT count(*) FROM alert a JOIN guinea_pig g ON g.id = a.guinea_pig_id "
        f"JOIN cage c ON c.id = g.cage_id WHERE c.code = '{cage}' AND g.mark_color = '{color}'"
    ) or 0)


def step(name: str, expected: str, got: str, failures: list[str]) -> None:
    ok = got == expected
    print(f"{name:<34}{expected:<11}{got:<11}{'ok' if ok else 'FAIL'}")
    if not ok:
        failures.append(f"{name}: expected {expected}, got {got}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backend", default="http://localhost:8080")
    parser.add_argument("--api-key", default="dev-key")
    parser.add_argument("--cage", default="cage-1")
    parser.add_argument("--color", default="RED")
    args = parser.parse_args()

    host = urlsplit(args.backend).hostname
    if host not in LOCAL_HOSTS:
        print(f"refusing to run against {host}: this script deletes rows and rewrites "
              f"status_since, so it only runs against {sorted(LOCAL_HOSTS)}", file=sys.stderr)
        return 2

    failures: list[str] = []
    try:
        print(f"Escalation chain - {args.color} guinea pig of {args.cage} on {args.backend}\n")
        reset(args.cage, args.color)
        print(f"{'step':<34}{'expected':<11}{'got':<11}")
        step("start", "NORMAL", status_of(args.cage, args.color), failures)

        # An anomaly is only confirmed after health.consecutive-anomaly-windows in a row, so one
        # bad window does not move anything: that is what stops a cuy that just sat still briefly
        # from raising an alarm.
        for _ in range(CONSECUTIVE_WINDOWS - 1):
            send_window(args.backend, args.api_key, args.cage, args.color, sick=True)
        step(f"{CONSECUTIVE_WINDOWS - 1} sick windows", "NORMAL",
             status_of(args.cage, args.color), failures)

        # NORMAL -> OBSERVED: the confirmed anomaly starts the watch at once, with no waiting
        send_window(args.backend, args.api_key, args.cage, args.color, sick=True)
        step(f"window {CONSECUTIVE_WINDOWS}, anomaly confirmed", "OBSERVED",
             status_of(args.cage, args.color), failures)

        # OBSERVED -> ALERT: only once the anomaly has lasted health.observed-to-alert (30 min)
        send_window(args.backend, args.api_key, args.cage, args.color, sick=True)
        step("another, 0 min in OBSERVED", "OBSERVED", status_of(args.cage, args.color), failures)
        back_date_status_since(args.cage, args.color, 31)
        send_window(args.backend, args.api_key, args.cage, args.color, sick=True)
        step("another, 31 min in OBSERVED", "ALERT", status_of(args.cage, args.color), failures)

        # ALERT -> CRITICAL: another health.alert-to-critical (60 min) at this level
        send_window(args.backend, args.api_key, args.cage, args.color, sick=True)
        step("another, 0 min in ALERT", "ALERT", status_of(args.cage, args.color), failures)
        back_date_status_since(args.cage, args.color, 61)
        send_window(args.backend, args.api_key, args.cage, args.color, sick=True)
        step("another, 61 min in ALERT", "CRITICAL", status_of(args.cage, args.color), failures)

        # Only a move up into ALERT or CRITICAL warns the breeder: two alerts, not five
        raised = alerts_for(args.cage, args.color)
        ok = raised == 2
        print(f"{'alerts raised':<34}{'2':<11}{str(raised):<11}{'ok' if ok else 'FAIL'}")
        if not ok:
            failures.append(f"alerts raised: expected 2 (ALERT and CRITICAL), got {raised}")

        # Recovery needs health.recovery-windows normal windows in a row
        for _ in range(RECOVERY_WINDOWS - 1):
            send_window(args.backend, args.api_key, args.cage, args.color, sick=False)
        step(f"{RECOVERY_WINDOWS - 1} normal windows", "CRITICAL",
             status_of(args.cage, args.color), failures)
        send_window(args.backend, args.api_key, args.cage, args.color, sick=False)
        step(f"normal window {RECOVERY_WINDOWS}", "NORMAL",
             status_of(args.cage, args.color), failures)

        print("\nTransitions written down:")
        history = psql(
            "SELECT st.from_status || ' -> ' || st.to_status || '   ' || coalesce(st.reason, '') "
            "FROM state_transition st JOIN guinea_pig g ON g.id = st.guinea_pig_id "
            f"JOIN cage c ON c.id = g.cage_id WHERE c.code = '{args.cage}' "
            f"AND g.mark_color = '{args.color}' ORDER BY st.occurred_at"
        )
        for line in history.splitlines():
            print(f"  {line}")
    except Failure as error:
        print(f"\n{error}", file=sys.stderr)
        return 2

    print()
    if failures:
        print(f"{len(failures)} step(s) wrong:")
        for failure in failures:
            print(f"  - {failure}")
        return 1
    print("The chain escalated and recovered exactly as the policy says.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
