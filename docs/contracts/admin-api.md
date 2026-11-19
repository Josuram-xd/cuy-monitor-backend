# Admin API

The catalog of breeds, coat colors and mark colors lives in the database, not in the dashboard's code. This is
the surface that edits it, plus the accounts. Everything is under `/api/v1/admin` and needs an **admin** account.

## `UserRole`

The shared enum. Column `app_user.role` in `cuy-monitor-db` (migration `V16`), `VARCHAR(20)` with
`CHECK (role IN ('BREEDER', 'ADMIN'))`.

| Value | Who | Can |
|---|---|---|
| `BREEDER` | the default for every account that signs up | their own cages, guinea pigs and alerts |
| `ADMIN` | named by another admin, or by `V16` for the pilot cage's owner | the same, plus everything below |

A new account is always `BREEDER`. Nothing in the sign-up flow can produce an `ADMIN`.

## How the dashboard knows

`GET /api/v1/account/profile` now answers with the role as well:

```json
{ "username": "dev", "fullName": "Dev", "hasPassword": true, "role": "ADMIN" }
```

That is what the dashboard uses to decide whether to draw the admin section — the session cookie is HttpOnly,
so the page cannot read the token's claim itself.

## How the permission works

The session's JWT also carries a `role` claim, for anything that reads the token server side. **Neither that
claim nor the profile field is the permission.** Every call below is checked by `AdminAccessProxy`, which reads
the account's role from the database at that moment — a token still saying `ADMIN` after the account was
demoted buys nothing, and nobody has to wait for a token to expire.

An account that is not an admin — or does not exist — gets the same `403`:

```json
{ "error": "FORBIDDEN", "message": "this account can't administer the system" }
```

## Endpoints

### `GET /api/v1/admin/catalog`

Every option, **including the ones switched off** (unlike `GET /api/v1/catalog`, which the pickers use).

```json
[
  { "kind": "BREED", "code": "PERUVIAN", "label": "Peruano", "hex": null, "sortOrder": 2, "active": true },
  { "kind": "MARK_COLOR", "code": "RED", "label": "Rojo", "hex": "#d94c4c", "sortOrder": 1, "active": true }
]
```

`kind` is `BREED`, `COAT_COLOR` or `MARK_COLOR`.

### `PUT /api/v1/admin/catalog`

Creates an option, or overwrites the one with the same `kind` and `code`. This is how a breed that did not
exist gets added without a release:

```json
{ "kind": "BREED", "code": "colombiano", "label": "Cuy colombiano", "sortOrder": 9 }
```

- `code` is uppercased and may only hold `A-Z`, `0-9` and `_`, up to 20 characters — the same rule as
  `ck_catalog_option_code` in the database, so a bad code is a `400` and never a failed insert.
- `hex` is required by nothing but a `MARK_COLOR` in practice, and must look like `#a1b2c3`, lower case.
- `active` defaults to `true`.

### `DELETE /api/v1/admin/catalog/{kind}/{code}`

Switches the option **off**; it is not deleted. The guinea pigs already registered with that breed or color
keep it, they just can't be picked any more. `PUT` with `"active": true` brings it back.

### `GET /api/v1/admin/users`

Every account, newest first. No password hash and no Google subject. Each one carries a **summary** of its use:
its active cages, their active guinea pigs, their open alerts, and when the last event arrived for any of them
(`null` if never). The admin sees counts, never what is inside someone else's cages.

```json
[
  { "id": "…", "username": "dev", "fullName": "Dev", "email": "dev@cuymonitor.local",
    "status": "ACTIVE", "role": "ADMIN", "createdAt": "2026-11-12T10:00:00Z",
    "cages": 2, "guineaPigs": 7, "openAlerts": 1, "lastActivityAt": "2026-11-18T14:02:11Z" }
]
```

The `PATCH` below answers with the same shape.

### `GET /api/v1/admin/settings` · `PUT` · `DELETE`

**Reglas y mensajes**: the health rules, the texts of the alerts and the assistant's question limit. `GET` lists every
setting with its current value, the default it goes back to and its range (`min`/`max`; for `TEXT` they are the
length). The defaults come from `application.yml` (texts from the code); only the admin's changes are stored
(table `app_setting`, V18). Changes apply **at once, without a restart**: the next event is evaluated with them.

```json
[
  { "key": "health.observedToAlertMinutes", "kind": "INTEGER", "value": "20", "defaultValue": "30", "min": 1, "max": 240 },
  { "key": "message.alert", "kind": "TEXT", "value": "Comportamiento fuera de lo normal de forma sostenida", "defaultValue": "…", "min": 1, "max": 500 },
  { "key": "assistant.maxQuestions", "kind": "INTEGER", "value": "0", "defaultValue": "0", "min": 0, "max": 1000 }
]
```

`PUT` takes only what changes, `{ "health.observedToAlertMinutes": "20", "message.alert": "Revisa a tu cuy" }`, and
answers with the full list. One unknown key or one value out of range: `400` and nothing changes. `DELETE` puts every
setting back to its default.

| Group | Keys |
|---|---|
| Health rules | `health.maxEventAgeMinutes`, `minDetectionConfidence`, `consecutiveAnomalyWindows`, `distressProbability`, `observedToAlertMinutes`, `alertToCriticalMinutes`, `recoveryWindows`, `classifierThreshold`, `stillFactor`, `groupDistanceFactor`, `baselineMinSamples`, `baselineWarmupSamples`, `weightDropPercent`, `weightRecentHours`, `weightBaselineDays`, `weightMinReadings` |
| Alert texts | `message.alert`, `message.critical`, `message.distress`, `message.weightDrop` (`{porcentaje}` is replaced by the drop) |
| Assistant | `assistant.maxQuestions` (**0 = no limit**, the default), `assistant.windowMinutes` |

The texts are what every alert says. The advice under an alert (`aiNote`) is written by the AI and is not here.

### `GET /api/v1/admin/ai-log?limit=100`

What the AI did lately, newest first: windows the AI service analysed (`BEHAVIOR`), sounds it classified (`AUDIO`)
and what the assistant wrote about an alert (`NOTE`). `limit` is 1–200 (default 100); outside that, `400`.
`detail` is already written in Spanish.

```json
[
  { "at": "2026-11-18T14:02:11Z", "cageCode": "cage-1", "kind": "NOTE", "detail": "Rojo lleva casi una hora…" },
  { "at": "2026-11-18T14:01:05Z", "cageCode": "cage-1", "kind": "BEHAVIOR",
    "detail": "RED: quieto 55.0 s de 60 s, comedero 0, bebedero 0, anomalía 0.3" }
]
```

### `PATCH /api/v1/admin/users/{id}`

Either a role or whether the account is on. Send one:

```json
{ "role": "ADMIN" }
{ "enabled": false }
```

Disabling is `UserStatus.DISABLED`, the same state `DELETE /api/v1/account` leaves; `{"enabled": true}` puts an
account that was disabled back to `ACTIVE`.

## Rules that hold for every admin

These are `400`, not `403` — the caller is allowed, the system just must not do it:

| Rule | Why |
|---|---|
| can't change your own role | so the one admin left can't demote themselves by mistake |
| can't disable your own account from here | same, and `DELETE /api/v1/account` is the way to leave |

Between them those two already make it impossible to lose the last admin: demoting or disabling the only
admin would take an admin, and the only admin is you. `AdminService` checks it again anyway, as a safety net
for a caller that does not come through the proxy, but no request to this API can produce that error.

## Errors

| Status | `error` | When |
|---|---|---|
| `400` | `BAD_REQUEST` | a bad code or color, or one of the rules above |
| `403` | `FORBIDDEN` | the account is not an admin |
| `404` | `NOT_FOUND` | no account with that id |
