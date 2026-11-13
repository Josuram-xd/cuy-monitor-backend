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
| `/actuator/health`, `/api/v1/auth/**` | Public |
| `/api/v1/ingestion/**` | `X-API-Key` header (no JWT) |
| everything else under `/api/v1/**` | the `access_token` HttpOnly cookie (see `auth-api.md`) |
| `/ws` | the same cookie, validated on the STOMP `CONNECT` frame (section 4) |

Common errors for every JWT endpoint: `401` without a token, with an invalid or expired token, or for a `DISABLED` account.

| Status | `error` | When |
|---|---|---|
| `400` | `bad_request` | Invalid body, query param or date range |
| `401` | `unauthorized` | Missing, invalid or expired token |
| `403` | `forbidden` | The cage (or the alert's cage) belongs to another account |
| `404` | `not_found` | Cage, guinea pig or alert does not exist |
| `409` | `conflict` | Duplicate (for example, color already used in the cage) |
| `429` | `too_many_requests` | Too many requests of one kind (codes, assistant questions) |
| `503` | `service_unavailable` | A dependency is down (the assistant's model) |

---

## 2. Ingestion

### `POST /api/v1/ingestion/events`

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
| `GET /api/v1/cages` | Cages of the signed-in account | `200` |
| `POST /api/v1/cages` | Create a cage | `201` |
| `GET /api/v1/cages/{cageId}` | One cage | `200` |
| `PUT /api/v1/cages/{cageId}` | Rename a cage / change its location | `200` |
| `DELETE /api/v1/cages/{cageId}` | Delete a cage (soft delete) | `204` |
| `GET /api/v1/cages/{cageId}/health` | Cage status summary | `200` |
| `GET /api/v1/cages/{cageId}/guinea-pigs` | Guinea pigs with current status | `200` |
| `GET /api/v1/cages/{cageId}/guinea-pigs/{id}/photo` | The photo of a guinea pig | `200` |
| `POST /api/v1/cages/{cageId}/guinea-pigs` | Register a guinea pig | `201` |
| `DELETE /api/v1/cages/{cageId}/guinea-pigs/{id}` | Delete a guinea pig (soft delete) | `204` |
| `GET /api/v1/guinea-pigs/{id}/history?from=&to=` | State changes and behavior windows | `200` |
| `GET /api/v1/alerts?status=` | List alerts | `200` |
| `PATCH /api/v1/alerts/{id}` | Mark an alert as reviewed | `200` |
| `GET /api/v1/cages/{cageId}/weight?from=&to=` | Weight readings | `200` |

### 3.0 Cages

A farmer can have several cages. Each one belongs to the account that created it (see the cage access proxy in
`ARCHITECTURE.md`): another account gets `403` on it.

`GET /api/v1/cages` → `200`, the active cages of the account, oldest first:

```json
[
  { "cageId": "cage-1", "name": "Jaula piloto", "location": "Galpón", "createdAt": "2026-09-20T10:00:00Z" },
  { "cageId": "cage-0a1b2c3d", "name": "Jaula del patio", "location": null, "createdAt": "2026-11-02T14:00:00Z" }
]
```

`POST /api/v1/cages` and `PUT /api/v1/cages/{cageId}` take `{ "name": "Jaula del patio", "location": "Patio" }`:
`name` 1–100 characters (trimmed), `location` optional, up to 200 (blank removes it). The backend picks the code of a
new cage (`cage-` + 8 hex characters); that code is what the camera, the AI service and the scale send as `cageId`.
Answers `201` / `200` with the cage, `400` for an invalid body, `404` for an unknown cage.

`DELETE /api/v1/cages/{cageId}` → `204`. Soft delete: the cage leaves the list and its events are rejected from then
on; its guinea pigs, alerts and history are kept.

### 3.1 `GET /api/v1/cages/{cageId}/health`

Result of the Composite: the cage status and its parts.

```json
{
  "cageId": "cage-1",
  "status": "OBSERVED",
  "guineaPigs": [
    { "id": 1, "photoUrl": "/api/v1/cages/cage-1/guinea-pigs/1/photo", "markColor": "RED", "status": "OBSERVED" }
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

### 3.2 `GET /api/v1/cages/{cageId}/guinea-pigs`

Active guinea pigs only.

```json
[
  {
    "id": 1, "photoUrl": "/api/v1/cages/cage-1/guinea-pigs/1/photo", "markColor": "RED",
    "status": "OBSERVED", "statusSince": "2026-10-05T14:10:00Z",
    "breed": "TEDDY", "coatColor": "CREAM", "initialWeightGrams": 850, "notes": "Muy tranquila"
  }
]
```

`breed`, `coatColor`, `initialWeightGrams` and `notes` are `null` for a guinea pig registered without them
(all the ones created before they existed). `photoUrl` is the path of 3.3c, or `null` for a guinea pig
registered before the photo existed: the dashboard draws a placeholder for those.

### 3.3 `POST /api/v1/cages/{cageId}/guinea-pigs`

```json
{
  "photo": "iVBORw0KGgoAAAANSUhEUg...", "photoContentType": "image/jpeg", "markColor": "RED",
  "breed": "TEDDY", "coatColor": "CREAM", "initialWeightGrams": 850, "notes": "Muy tranquila"
}
```

| Field | Rules |
|---|---|
| `photo` | **Required**, the image in base64 (no `data:` prefix), up to 300 KB once decoded. It replaces the name: the breeder recognizes a cuy by its picture. The dashboard shrinks it before sending it. |
| `photoContentType` | Required, one of `image/jpeg`, `image/png`, `image/webp`. No SVG: those bytes are served back to a browser. |
| `markColor` | Required, a `MARK_COLOR` code of 3.8, up to 20 characters. One color per guinea pig in a cage: it is what the camera uses to tell them apart. |
| `breed` | Optional, a `BREED` code of 3.8. |
| `coatColor` | Optional, a `COAT_COLOR` code of 3.8: the real color of the fur, not the mark. |
| `initialWeightGrams` | Optional, integer from 50 to 2000. Weight on arrival. |
| `notes` | Optional, up to 500 characters (trimmed; blank is stored as `null`). |

`201` returns the created guinea pig (same shape as an item of 3.2, `status: "NORMAL"`, and `photoUrl` already
filled). `400` invalid body — including a photo that is not base64, is over 300 KB or is not one of the three
types (`{ "error": "bad_request", "message": "photo must be at most 307200 bytes" }`) — or a
code that is not an active option (`{ "error": "bad_request", "message": "unknown breed: DRAGON" }`) · `409` color
already used in the cage · `404` unknown cage.

The codes are read upper case: `red` and `RED` are the same option.

### 3.3c `GET /api/v1/cages/{cageId}/guinea-pigs/{id}/photo`

The bytes of the photo, with the `Content-Type` they were registered with. Not JSON: the dashboard puts the path
straight into an `img` tag. `Cache-Control: private, max-age=86400` (a photo never changes; a new cuy is a new id)
and `X-Content-Type-Options: nosniff`.

`404` if the cage does not exist, the id is not an **active** guinea pig of that cage, or it has no photo. Needs a
session like every other endpoint of section 3, so the picture is not public.

### 3.3b `DELETE /api/v1/cages/{cageId}/guinea-pigs/{id}`

No body. `204 No Content`. **Soft delete**: the guinea pig stops appearing in the list and in the cage health, and its mark color is free again; its events, alerts and history are kept. `404` if the cage does not exist, or the id is not an **active** guinea pig of that cage (another cage's id, unknown, or already deleted). Deleting twice answers `404` the second time.

### 3.4 `GET /api/v1/guinea-pigs/{id}/history?from=&to=`

`from` and `to` are optional (ISO-8601); the default is the last 24 hours. `to` must be after `from` and the range can't be longer than 31 days (`400` otherwise). `403` if the guinea pig is in another account's cage.

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
  ],
  "baseline": { "avgStillSeconds": 30, "avgFeederVisits": 1, "avgGroupDistance": 0.3 }
}
```

Both lists are ordered by `occurredAt`, oldest first. `windows` comes from stored `BEHAVIOR` events. `baseline` is what is normal for this guinea pig (the default until it is learned): the dashboard draws it as the normal range. `404` unknown guinea pig.

### 3.5 `GET /api/v1/alerts?status=&cageId=`

`status` is optional (`OPEN` or `REVIEWED`); without it, all alerts. `cageId` is optional; without it, the alerts of
every cage of the account (never another account's). Newest first. `403` if `cageId` is another account's cage.

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
    "reviewedAt": null,
    "aiNote": "Canela se movió mucho menos de lo normal en la última media hora. Revisa si está comiendo y bebiendo, y si se esconde o se aparta del grupo. Si sigue así mañana, llama al veterinario.",
    "aiNoteAt": "2026-10-05T14:20:04Z"
  }
]
```

| Field | Notes |
|---|---|
| `guineaPigId` | `null` for cage-level alerts (`AUDIO`, `WEIGHT`). |
| `level` | `ALERT` or `CRITICAL`. |
| `type` | `BEHAVIOR`, `AUDIO` or `WEIGHT`. |
| `aiNote` | What the assistant wrote about this alert **on its own**, with no question from the breeder. `null` for a few seconds after the alert is raised, while the note is being written, and `null` for good if the model was off or throttled. The dashboard shows the alert either way, and receives the note over WebSocket when it is ready. |
| `aiNoteAt` | When the note was written. `null` exactly when `aiNote` is. |

### 3.6 `PATCH /api/v1/alerts/{id}`

```json
{ "status": "REVIEWED" }
```

Only `REVIEWED` is accepted. Returns `200` with the updated alert (same shape as 3.5, with `reviewedAt` set). Marking an already reviewed alert is a no-op that also returns `200`. `400` any other status · `404` unknown alert.

### 3.7 `GET /api/v1/cages/{cageId}/weight?from=&to=`

Same defaults and rules for `from`/`to` as 3.4. `403` if it is another account's cage.

```json
{
  "cageId": "cage-1",
  "readings": [
    { "grams": 812.4, "stable": true, "measuredAt": "2026-10-05T14:32:00Z" }
  ]
}
```

Ordered by `measuredAt`, oldest first. Includes unstable readings; the client filters by `stable` if it needs to.

### 3.9 `GET /api/v1/alerts/history?cageId=&guineaPigId=&level=&type=&status=&from=&to=&page=&size=`

The alert history of the account's cages, one page at a time, **newest first** (then by `id`). Every parameter is optional:

| Parameter | Notes |
|---|---|
| `cageId` | One cage. Without it, every cage of the account. `403` if it is another account's cage. |
| `guineaPigId` | One guinea pig. `403` if its cage is another account's · `404` unknown guinea pig. |
| `level` | `ALERT` or `CRITICAL` (`400` for any other value). |
| `type` | `BEHAVIOR`, `AUDIO` or `WEIGHT`. |
| `status` | `OPEN` or `REVIEWED`. |
| `from`, `to` | ISO-8601. Same rules as 3.4, but the default is the **last 7 days**. Max 31 days. |
| `page`, `size` | `page` from 0 (default 0); `size` 1 to 100 (default 20). `400` outside. |

```json
{
  "items": [
    {
      "id": 7, "cageId": "cage-1", "guineaPigId": 1, "level": "CRITICAL", "type": "BEHAVIOR",
      "message": "Canela has been still for much longer than usual", "status": "REVIEWED",
      "createdAt": "2026-10-05T14:20:00Z", "reviewedAt": "2026-10-05T14:45:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 41,
  "totalPages": 3,
  "hasNext": true,
  "from": "2026-09-28T14:32:00Z",
  "to": "2026-10-05T14:32:00Z"
}
```

`items` have the shape of 3.5. A page past the last one is `200` with no items. Filtering and paging happen in the database (indexes `ix_alert_cage_created` and `ix_alert_guinea_pig_created`, `cuy-monitor-db` V14).

---

### 3.8 `GET /api/v1/catalog`

The lists the dashboard draws its pickers with. They are **rows of the `catalog_option` table**, not values of an
enum: adding a mark color or renaming a breed is an `INSERT` or an `UPDATE` in `cuy-monitor-db`, with no change to
this repo or to the dashboard.

```json
{
  "markColors": [{ "code": "RED", "label": "Rojo", "hex": "#d62828" }],
  "coatColors": [{ "code": "CINNAMON", "label": "Canela", "hex": "#c9763d" }],
  "breeds": [{ "code": "TEDDY", "label": "Teddy", "hex": null }]
}
```

| Field | Meaning |
|---|---|
| `code` | What travels in the API and is stored in the database (`RED`). Upper case, letters, digits and `_`, up to 20 characters |
| `label` | What the breeder reads, in Spanish. The dashboard shows it as it comes; it does not translate it |
| `hex` | Color of the dot the dashboard paints (`#d62828`). `null` for a breed, which has no sample |

Each list is already in the order it should be shown, and only holds the **active** options. A retired option
(`active = false`) disappears from here but the guinea pigs that use it keep their code, so an old record stays
readable. `200 OK`. Needs a session like every other endpoint of section 3.

### 3.10 `POST /api/v1/assistant/messages`

The help assistant ("Cuchi") of the dashboard. It answers how to use the app, general guinea pig care and what the
state of the breeder's **own** cages means. It runs on Amazon Bedrock (Converse API); it never decides or changes
anything, and it is not a veterinarian.

The backend keeps no chat history: the dashboard sends the conversation it shows, oldest first, ending with the new
question.

```json
{
  "language": "es",
  "messages": [
    { "role": "ASSISTANT", "text": "¡Hola! Soy Cuchi. ¿En qué te ayudo?" },
    { "role": "USER", "text": "¿Por qué Rojo está en observación?" }
  ]
}
```

| Field | Rule |
|---|---|
| `language` | `es` (default) or `en`: the language of the answer |
| `messages` | 1 to 12 messages. The last one must be `USER` |
| `role` | `USER` or `ASSISTANT` |
| `text` | 1 to 1000 characters |

`200 OK`:

```json
{ "reply": "Rojo está en observación desde las 11:00 porque se movió menos de lo normal. ..." }
```

| Status | `error` | When |
|---|---|---|
| `400` | `bad_request` | The rules of the table above are broken |
| `429` | `too_many_requests` | More than 20 questions from the account in 10 minutes |
| `503` | `service_unavailable` | The assistant is turned off or the model did not answer: show "try again later" |

The answer only uses the cages of the account in the session (read through the cage access proxy). Needs a session
like every other endpoint of section 3.

## 4. WebSocket (STOMP)

| Item | Value |
|---|---|
| Endpoint | `wss://<domain>/ws` (same origin) |
| Protocol | STOMP over WebSocket |
| Auth | The browser sends the `access_token` cookie on the handshake; the server validates it on the STOMP **`CONNECT`** frame. |
| Rejection | `CONNECT` without a token, or with an invalid or expired one, is answered with an `ERROR` frame and the connection is closed. Subscriptions from unauthenticated sessions are rejected. |
| Direction | Server → client only. The client sends nothing besides `CONNECT` and `SUBSCRIBE`. |
| Token expiry | The access token lasts 15 min. The check happens at `CONNECT`; the dashboard reconnects after a `/refresh` or after logging in again. |
| Client example | `new Client({ brokerURL: "wss://<domain>/ws" })` in `@stomp/stompjs`, no headers needed (see `auth-api.md`, WebSocket section) |

### Topic `/topic/cages/{cageId}`

One topic per cage, with the cage **code** (`/topic/cages/cage-1`). Only the owner of the cage can subscribe; any other
account gets a STOMP `ERROR` frame. Every message has the same envelope:

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
| `/api/v1/auth/**`, `/api/v1/account/**` | see `auth-api.md` | Registration, login with OTP, own account. |
| `/api/v1/admin/**` | see `admin-api.md` | The catalog and the accounts. Admins only. |

---

## 6. Changing this contract

Update this file first, then tell the other repos (`cuy-monitor-dashboard` mainly). Adding an optional field is compatible; renaming, removing or changing the type of a field, or changing a path, is breaking and must be agreed with the dashboard before it is merged.
