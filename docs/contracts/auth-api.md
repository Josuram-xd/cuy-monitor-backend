# Auth API

Base path: `/api/v1/auth`. All bodies are JSON (`Content-Type: application/json`), timestamps are ISO-8601 UTC.

There is a single basic user type. Anyone can register from the login screen. Login always takes two steps: password first, then a 6-digit code sent by email.

## Flow

```
register / login  ──►  { challengeId, expiresAt }  ──►  email with 6-digit code
                                                              │
otp/verify { challengeId, code }  ◄───────────────────────────┘
        │
        ▼
204 + two HttpOnly cookies (access_token, refresh_token)  ──►  the browser sends them by itself
```

**The tokens never appear in a response body and JavaScript cannot read them.** They live in cookies the server sets:

| Cookie | Holds | Path | Lasts |
|---|---|---|---|
| `access_token` | JWT (HS256) | `/` | 15 minutes |
| `refresh_token` | random opaque value (only its SHA-256 is stored) | `/api/v1/auth` | 7 days |

Both are `HttpOnly; Secure; SameSite=Strict`. Over `fetch` the dashboard needs `credentials: "include"` in development, when it runs on `localhost:5173` and the API on `localhost:8080` (in production it is the same origin).

## Endpoints

### `POST /api/v1/auth/register`

```json
{ "username": "juan", "fullName": "Juan Perez", "email": "juan@mail.com", "password": "secret-pass" }
```

| Field | Rules |
|---|---|
| `username` | required, max 50. Stored trimmed and lowercase |
| `fullName` | required, max 150 |
| `email` | required, valid email, max 254. Stored lowercase |
| `password` | 10 to 64 characters with at least one lowercase letter, one uppercase letter, one digit and one special character (anything that is not a letter, digit or space). No spaces, not a well-known password (`Password123!` counts) and not containing the username or a part of the email. |

`201 Created`

```json
{ "challengeId": "6f1c…", "expiresAt": "2026-10-01T10:05:00Z" }
```

The account is created as `PENDING_VERIFICATION` and a code is emailed.

A password that breaks the rules answers `400` with the broken ones, so the form can tick its checklist:

```json
{ "error": "bad_request", "message": "password does not meet the requirements", "fields": { "password": "MIN_LENGTH,SPECIAL" } }
```

| Code | Rule |
|---|---|
| `MIN_LENGTH` / `MAX_LENGTH` | fewer than 10 or more than 64 characters (also more than 72 bytes: BCrypt would cut it) |
| `LOWERCASE`, `UPPERCASE`, `DIGIT`, `SPECIAL` | missing that kind of character |
| `NO_SPACES` | contains a space |
| `NOT_COMMON` | a well-known password, even decorated (`Password123!`) |
| `NOT_PERSONAL` | contains the username or a part of the email |

Existing accounts keep working with the password they have: the rules apply when a password is created or changed.

### `POST /api/v1/auth/login`

```json
{ "username": "juan", "password": "secret-pass" }
```

`200 OK`: same body as register. A new code is emailed and any previous pending code of that user stops working. It also works for accounts that were never verified, so it doubles as "resend code".

### `POST /api/v1/auth/otp/verify`

```json
{ "challengeId": "6f1c…", "code": "123456" }
```

`204 No Content`, no body. The response carries two `Set-Cookie` headers (`access_token` and `refresh_token`, see above). The dashboard knows it is logged in by calling `GET /api/v1/account/profile` afterwards.

If the account was `PENDING_VERIFICATION` it becomes `ACTIVE`.

### `POST /api/v1/auth/google`

Sign in or sign up with Google. No code to type: Google already proved the email.

```json
{ "idToken": "eyJhbGciOi…" }
```

`idToken` is the `credential` that Google's button hands to the page (max 4096 characters). `204 No Content` with the same two cookies as `/otp/verify`.

The server checks the signature against Google's public keys, the expiry, the issuer (`accounts.google.com`), that the token was made for **our** client id (`GOOGLE_CLIENT_ID`) and that `email_verified` is true. Then:

| Case | What happens |
|---|---|
| The Google id (`sub`) is already linked | That account logs in |
| Unknown `sub`, but the email belongs to an `ACTIVE` account | The account is linked to Google and keeps its password |
| Unknown `sub`, the email belongs to a `PENDING_VERIFICATION` account | Linked, activated and **its password is removed**: someone may have registered that email first with a password only they know |
| Nothing known | A new `ACTIVE` account without password; username taken from the email (`ana.ruiz@gmail.com` becomes `ana.ruiz`, with 4 digits appended if it is taken) |

An account with no password cannot use `/auth/login`; it answers `invalid credentials` like a wrong password. It can set a first password in `PUT /account/password` without `currentPassword`.

`401 unauthorized` / `invalid google token`: bad signature, expired, other client id, other issuer, or unverified email. `401` / `account disabled` for a `DISABLED` account. `404 not_found` if the server has no `GOOGLE_CLIENT_ID` (feature off).

### `POST /api/v1/auth/refresh`

No body. Reads the `refresh_token` cookie (the browser only sends it to `/api/v1/auth`).

`204 No Content` with a new `access_token` **and a new `refresh_token`**. Every use rotates the refresh token: the old one stops working. If an old one is presented again later, the server assumes it was stolen and revokes the whole session (all the tokens that came from that login), so the user has to log in again. Two tabs refreshing at the very same moment are tolerated for 10 seconds.

`401 unauthorized` / `invalid or expired session`: cookie missing, unknown, expired, already used, or the account is `DISABLED`.

### `POST /api/v1/auth/logout`

No body. Always answers `204 No Content` and expires both cookies, even if the access token is already expired or no cookie was sent. It revokes the `jti` of the access token, the whole session (`sid`) and the refresh-token family, so a copied token stops working immediately, including the ones issued before the last refresh.

## Errors

Every error follows the API convention `{ "error": "<code>", "message": "..." }` (see `rest-api.md`). Validation errors also include the failing fields:

```json
{ "error": "bad_request", "message": "validation failed", "fields": { "email": "must be a well-formed email address" } }
```

| Status | `error` | `message` | When |
|---|---|---|---|
| `400` | `bad_request` | varies | Invalid body, malformed JSON, code not 6 digits. A password that breaks the rules also carries `fields.password` with the broken rules as codes (see below) |
| `401` | `unauthorized` | `invalid credentials` | Wrong username or password, unknown user, or disabled account (same message on purpose) |
| `401` | `unauthorized` | `invalid or expired session` | Refresh cookie missing, unknown, expired, already rotated, or the account is disabled |
| `401` | `unauthorized` | `invalid or expired code` | Wrong, expired, already used or revoked code, or 5 failed attempts (same message on purpose) |
| `401` | `unauthorized` | `missing, invalid or expired token` | Protected route without a valid `Authorization: Bearer` header |
| `429` | `too_many_requests` | `too many codes requested, try again later` | More than 5 codes requested for the same account in 15 minutes (login / resend) |
| `409` | `conflict` | `username or email already in use` | Username or email already taken |

## Rules

- Code: 6 digits, valid for **5 minutes**, single use, at most **5 attempts**. Asking for a new one (login) invalidates the previous ones. At most **5 codes per account every 15 minutes**; after that, login answers `429` until the window passes.
- Access token: JWT signed with HS256, valid for **15 minutes**. Claims: `sub` (user id, UUID), `iss` (`cuy-monitor-backend`), `iat`, `exp`, `jti` (unique id of the token), `sid` (id of the session: every access token born from one login shares it). A token without a valid `jti` or `sid`, or whose `jti` or `sid` was revoked, is rejected.
- Refresh token: random 256-bit value, valid for **7 days**, rotated on every use (see `/refresh`). Stored hashed.
- Logout revokes the tokens on the server (see `/logout`); deleting a cookie in the browser is not enough and is not what the dashboard does.
- `/api/v1/auth/**` ignores the access token on purpose, so login, refresh and logout work when the old cookie is already expired.
- No CSRF token: the cookies are `SameSite=Strict`, so a request that starts on another site never carries them, and the dashboard is served from the same site as the API.

## Account (`/api/v1/account`)

All these routes need `Authorization: Bearer <accessToken>`. The account is always the one in the token `sub`; there is no id in the URL and no way to list or touch other users. Every request loads the account again, so a `DISABLED` account gets `401` even if its token has not expired yet.

### `GET /api/v1/account/profile`

`200 OK`

```json
{ "username": "juan", "fullName": "Juan Perez", "hasPassword": true }
```

`hasPassword` is `false` for an account made with Google that never set one, so the screen knows not to ask for a password it does not have. On purpose it returns only what the screen shows: no id, email, status, timestamps or password hash.

### `PUT /api/v1/account/profile`

```json
{ "fullName": "Juan Carlos Perez" }
```

`200 OK`: the updated account (same body as `GET`). `fullName` is required, max 150. Username and email cannot be changed.

### `PUT /api/v1/account/password`

```json
{ "currentPassword": "secret-pass", "newPassword": "new-secret-pass" }
```

`204 No Content`. `currentPassword` is required, except for an account without password (made with Google), which may leave it out to set its first one. The new password follows the same rules as in registration. Tokens already issued keep working until they expire.

### `DELETE /api/v1/account`

```json
{ "currentPassword": "secret-pass" }
```

`204 No Content`. `currentPassword` is required, except for an account without password (made with Google). Soft delete: the account becomes `DISABLED` and can no longer log in. The dashboard should call `POST /api/v1/auth/logout` right after.

### Account errors

| Status | `error` | `message` | When |
|---|---|---|---|
| `400` | `bad_request` | varies | Invalid body or a new password that breaks the rules (with `fields.password`) |
| `401` | `unauthorized` | `missing, invalid or expired token` | No access cookie, or it is invalid, expired or revoked (the dashboard then calls `/refresh` once and retries) |
| `401` | `unauthorized` | `invalid credentials` | Wrong `currentPassword` |
| `401` | `unauthorized` | `account is disabled` | The account was deactivated but its token has not expired yet |

## Which routes need what

| Route | Auth |
|---|---|
| `/api/v1/auth/**` | public |
| `/actuator/health` | public |
| `/api/v1/ingestion/**` | `X-API-Key` header (no JWT) |
| everything else under `/api/v1/**` | the `access_token` cookie (`Authorization: Bearer <accessToken>` also works for tools like curl), otherwise `401` |
| `/ws` (STOMP) | the browser sends the `access_token` cookie on the handshake; it is validated on the STOMP `CONNECT` frame |

### WebSocket (`/ws`)

The browser sends the `access_token` cookie on the handshake by itself (same origin), so the dashboard does not handle any token:

```js
const client = new Client({ brokerURL: `wss://${location.host}/ws` });
```

- The server copies the cookie to the session on the handshake and validates it on the STOMP `CONNECT` frame, like any other request (signature, issuer, expiration and revocation).
- No cookie, or an invalid, expired, revoked or foreign token: the server answers with a STOMP `ERROR` frame and closes the connection. Do not rely on the text of the `message` header. Clients that are not browsers can still send `Authorization: Bearer <accessToken>` on the `CONNECT` frame.
- A `SUBSCRIBE` from a session that did not authenticate on `CONNECT` is rejected the same way.
- The token is only checked on `CONNECT`. An open connection is not closed when the token expires or is revoked; the dashboard reconnects after a `/refresh` and after logging in again.
