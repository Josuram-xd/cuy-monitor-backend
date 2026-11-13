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

Every account, newest first. No password hash and no Google subject.

```json
[
  { "id": "…", "username": "dev", "fullName": "Dev", "email": "dev@cuymonitor.local",
    "status": "ACTIVE", "role": "ADMIN", "createdAt": "2026-11-12T10:00:00Z" }
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
| can't demote or disable the **last** admin | the catalog would become uneditable with no way back through the app |

## Errors

| Status | `error` | When |
|---|---|---|
| `400` | `BAD_REQUEST` | a bad code or color, or one of the rules above |
| `403` | `FORBIDDEN` | the account is not an admin |
| `404` | `NOT_FOUND` | no account with that id |
