# Contract — REST API and WebSocket

> Source of truth for the dashboard (`cuy-monitor-dashboard`) and for any client of the backend. Events sent by producers are in [`events.md`](events.md); login and account endpoints will be in `auth-api.md` (Tasks 18.18 and 19.8).

## 1. Conventions

| Topic | Rule |
|---|---|
| Base | Same origin as the dashboard (`https://cuymonitor.duckdns.org`). Caddy sends `/api/*`, `/ws*` and `/actuator/health*` to the backend. |
| Paths | `kebab-case` and plural. |
| JSON | `camelCase`. Enums are `UPPER_SNAKE_CASE` strings (see `events.md`, section 3). |
| Timestamps | ISO-8601 in UTC with `Z` (`2026-10-05T14:32:00Z`). Query params `from` and `to` use the same format. |
| Cage id in paths | The cage **code** (`cage-1`), same as `cageId` in events. Guinea pigs and alerts use their numeric `id`. |
| Errors | `{ "error": "<code>", "message": "..." }` |
| Hashes and secrets | Never returned. |

### Access

| Path | Access |
|---|---|
| `/actuator/health`, `/api/auth/**` | Public |
| `/api/ingestion/**` | `X-API-Key` header (no JWT) |
| everything else under `/api/**` | `Authorization: Bearer <jwt>` |
| `/ws` | JWT on the STOMP `CONNECT` frame (section 4) |

Common errors for every JWT endpoint: `401` without a token, with an invalid or expired token, or for a `DISABLED` account.

| Status | `error` | When |
|---|---|---|
| `400` | `bad_request` | Invalid body, query param or date range |
| `401` | `unauthorized` | Missing, invalid or expired token |
| `404` | `not_found` | Cage, guinea pig or alert does not exist |
| `409` | `conflict` | Duplicate (for example, color already used in the cage) |

---

## 2. Ingestion

### `POST /api/ingestion/events`

Header `X-API-Key`. Body is the event envelope. Full field rules, payloads and retry behavior: [`events.md`](events.md).

| Status | Body |
|---|---|
| `202` | `{ "eventId": "...", "status": "ACCEPTED" }` |
| `400` | invalid envelope (do not retry) |
| `401` | wrong or missing API key (do not retry) |

---

## 3. Dashboard endpoints (JWT)

Summary:

| Method and path | Purpose | Success |
|---|---|---|
| `GET /api/cages/{cageId}/health` | Cage status summary | `200` |
| `GET /api/cages/{cageId}/guinea-pigs` | Guinea pigs with current status | `200` |
| `POST /api/cages/{cageId}/guinea-pigs` | Register a guinea pig | `201` |
| `GET /api/guinea-pigs/{id}/history?from=&to=` | State changes and behavior windows | `200` |
| `GET /api/alerts?status=` | List alerts | `200` |
| `PATCH /api/alerts/{id}` | Mark an alert as reviewed | `200` |
| `GET /api/cages/{cageId}/weight?from=&to=` | Weight readings | `200` |

### 3.1 `GET /api/cages/{cageId}/health`

Result of the Composite: the cage status and its parts.

```json
{
  "cageId": "cage-1",
  "status": "OBSERVED",
  "guineaPigs": [
    { "id": 1, "name": "Canela", "markColor": "RED", "status": "OBSERVED" }
  ],
  "audio":  { "status": "NORMAL", "lastEventAt": "2026-10-05T14:30:00Z" },
  "weight": { "status": "NORMAL", "lastGrams": 812.4, "lastMeasuredAt": "2026-10-05T14:32:00Z" },
  "updatedAt": "2026-10-05T14:32:05Z"
}
```

| Field | Notes |
|---|---|
| `status` | `HealthStatus` of the whole cage (worst of its parts). |
| `audio`, `weight` | Cage-level signals. `lastEventAt`, `lastGrams`, `lastMeasuredAt` are `null` if nothing arrived yet. |

### 3.2 `GET /api/cages/{cageId}/guinea-pigs`

Active guinea pigs only.

```json
[
  { "id": 1, "name": "Canela", "markColor": "RED", "status": "OBSERVED", "statusSince": "2026-10-05T14:10:00Z" }
]
```

### 3.3 `POST /api/cages/{cageId}/guinea-pigs`

```json
{ "name": "Canela", "markColor": "RED" }
```

| Field | Rules |
|---|---|
| `name` | Required, 1–100 characters. |
| `markColor` | Required, a `MarkColor`. One color per guinea pig in a cage. |

`201` returns the created guinea pig (same shape as an item of 3.2, `status: "NORMAL"`). `400` invalid body · `409` color already used in the cage · `404` unknown cage.

### 3.4 `GET /api/guinea-pigs/{id}/history?from=&to=`

`from` and `to` are optional; the default is the last 24 hours. `to` must be after `from`.

```json
{
  "guineaPigId": 1,
  "from": "2026-10-04T14:32:00Z",
  "to": "2026-10-05T14:32:00Z",
  "transitions": [
    { "fromStatus": "NORMAL", "toStatus": "OBSERVED", "reason": "Anomaly in 3 consecutive windows", "occurredAt": "2026-10-05T14:10:00Z" }
  ],
  "windows": [
    { "occurredAt": "2026-10-05T14:32:00Z", "stillSeconds": 48, "feederVisits": 0, "watererVisits": 1, "avgGroupDistance": 0.72 }
  ]
}
```

Both lists are ordered by `occurredAt`, oldest first. `windows` comes from stored `BEHAVIOR` events. `404` unknown guinea pig.

### 3.5 `GET /api/alerts?status=`

`status` is optional (`OPEN` or `REVIEWED`); without it, all alerts. Newest first.

```json
[
  {
    "id": 7,
    "cageId": "cage-1",
    "guineaPigId": 1,
    "level": "ALERT",
    "type": "BEHAVIOR",
    "message": "Canela has been still for much longer than usual",
    "status": "OPEN",
    "createdAt": "2026-10-05T14:20:00Z",
    "reviewedAt": null
  }
]
```

| Field | Notes |
|---|---|
| `guineaPigId` | `null` for cage-level alerts (`AUDIO`, `WEIGHT`). |
| `level` | `ALERT` or `CRITICAL`. |
| `type` | `BEHAVIOR`, `AUDIO` or `WEIGHT`. |

### 3.6 `PATCH /api/alerts/{id}`

```json
{ "status": "REVIEWED" }
```

Only `REVIEWED` is accepted. Returns `200` with the updated alert (same shape as 3.5, with `reviewedAt` set). Marking an already reviewed alert is a no-op that also returns `200`. `400` any other status · `404` unknown alert.

### 3.7 `GET /api/cages/{cageId}/weight?from=&to=`

Same defaults and rules for `from`/`to` as 3.4.

```json
{
  "cageId": "cage-1",
  "readings": [
    { "grams": 812.4, "stable": true, "measuredAt": "2026-10-05T14:32:00Z" }
  ]
}
```

Ordered by `measuredAt`, oldest first. Includes unstable readings; the client filters by `stable` if it needs to.

---

## 4. WebSocket (STOMP)

| Item | Value |
|---|---|
| Endpoint | `wss://<domain>/ws` (same origin) |
| Protocol | STOMP over WebSocket |
| Auth | Header `Authorization: Bearer <jwt>` on the STOMP **`CONNECT`** frame. Browsers cannot set headers on the WebSocket handshake, so the handshake itself is open. |
| Rejection | `CONNECT` without a token, or with an invalid or expired one, is answered with an `ERROR` frame and the connection is closed. Subscriptions from unauthenticated sessions are rejected. |
| Direction | Server → client only. The client sends nothing besides `CONNECT` and `SUBSCRIBE`. |
| Token expiry | The token lasts 30 min. The check happens at `CONNECT`; the dashboard reconnects with a new token after logging in again. |

### Topic `/topic/cages/{cageId}`

One topic per cage, with the cage **code** (`/topic/cages/cage-1`). Every message has the same envelope:

```json
{ "type": "ALERT", "cageId": "cage-1", "occurredAt": "2026-10-05T14:20:00Z", "data": {} }
```

| `type` | When | `data` |
|---|---|---|
| `ALERT` | A guinea pig or the cage goes to `ALERT` or `CRITICAL` and an alert is created | The alert, same shape as an item of 3.5 |
| `STATUS_CHANGED` | A guinea pig changes status (any direction, including back to `NORMAL`) | `{ "guineaPigId": 1, "fromStatus": "NORMAL", "toStatus": "OBSERVED", "reason": "..." }` |
| `CAGE_HEALTH` | The cage summary changed | Same shape as 3.1 |

Messages are notifications to update the screen quickly; the REST endpoints stay the source of truth. After a reconnect the dashboard reloads with REST, because messages sent while disconnected are not replayed.

---

## 5. Other endpoints

| Method and path | Access | Notes |
|---|---|---|
| `GET /actuator/health` | Public | `{ "status": "UP" }`. Used by Caddy and the team. |
| `GET /api/system/status` | JWT | Temporary smoke test (counts cages). Removed in Task 17.1. Not for the dashboard. |
| `/api/auth/**`, `/api/users/me/**` | see `auth-api.md` | Registration, login with OTP, own account. |

---

## 6. Changing this contract

Update this file first, then tell the other repos (`cuy-monitor-dashboard` mainly). Adding an optional field is compatible; renaming, removing or changing the type of a field, or changing a path, is breaking and must be agreed with the dashboard before it is merged.
