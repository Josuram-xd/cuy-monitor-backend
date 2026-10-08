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
{ accessToken, tokenType: "Bearer", expiresAt }  ──►  Authorization: Bearer <accessToken>
```

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
| `password` | 8 to 72 bytes (UTF-8) |

`201 Created`

```json
{ "challengeId": "6f1c…", "expiresAt": "2026-10-01T10:05:00Z" }
```

The account is created as `PENDING_VERIFICATION` and a code is emailed.

### `POST /api/v1/auth/login`

```json
{ "username": "juan", "password": "secret-pass" }
```

`200 OK`: same body as register. A new code is emailed and any previous pending code of that user stops working. It also works for accounts that were never verified, so it doubles as "resend code".

### `POST /api/v1/auth/otp/verify`

```json
{ "challengeId": "6f1c…", "code": "123456" }
```

`200 OK`

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9…", "tokenType": "Bearer", "expiresAt": "2026-10-01T10:35:00Z" }
```

If the account was `PENDING_VERIFICATION` it becomes `ACTIVE`.

## Errors

Every error follows the API convention `{ "error": "<code>", "message": "..." }` (see `rest-api.md`). Validation errors also include the failing fields:

```json
{ "error": "bad_request", "message": "validation failed", "fields": { "email": "must be a well-formed email address" } }
```

| Status | `error` | `message` | When |
|---|---|---|---|
| `400` | `bad_request` | varies | Invalid body, malformed JSON, password outside 8–72 bytes, code not 6 digits |
| `401` | `unauthorized` | `invalid credentials` | Wrong username or password, unknown user, or disabled account (same message on purpose) |
| `401` | `unauthorized` | `invalid or expired code` | Wrong, expired, already used or revoked code, or 5 failed attempts (same message on purpose) |
| `401` | `unauthorized` | `missing, invalid or expired token` | Protected route without a valid `Authorization: Bearer` header |
| `409` | `conflict` | `username or email already in use` | Username or email already taken |

## Rules

- Code: 6 digits, valid for **5 minutes**, single use, at most **5 attempts**. Asking for a new one (login) invalidates the previous ones.
- Token: JWT signed with HS256, valid for **30 minutes**. Claims: `sub` (user id, UUID), `iss` (`cuy-monitor-backend`), `iat`, `exp`. There is no refresh token: when it expires the user logs in again.
- Logout: the API is stateless. The dashboard just deletes the token; it stops working on its own when it expires.

## Account (`/api/v1/users/me`)

All these routes need `Authorization: Bearer <accessToken>`. The account is always the one in the token `sub`; there is no id in the URL and no way to list or touch other users. Every request loads the account again, so a `DISABLED` account gets `401` even if its token has not expired yet.

### `GET /api/v1/users/me`

`200 OK`

```json
{
  "id": "3f2a…", "username": "juan", "fullName": "Juan Perez", "email": "juan@mail.com",
  "status": "ACTIVE", "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
}
```

The password hash is never returned.

### `PUT /api/v1/users/me`

```json
{ "fullName": "Juan Carlos Perez" }
```

`200 OK`: the updated account (same body as `GET`). `fullName` is required, max 150. Username and email cannot be changed.

### `PUT /api/v1/users/me/password`

```json
{ "currentPassword": "secret-pass", "newPassword": "new-secret-pass" }
```

`204 No Content`. The new password follows the same 8–72 bytes rule. Tokens already issued keep working until they expire.

### `DELETE /api/v1/users/me`

```json
{ "currentPassword": "secret-pass" }
```

`204 No Content`. Soft delete: the account becomes `DISABLED` and can no longer log in. The dashboard should delete its token right after.

### Account errors

| Status | `error` | `message` | When |
|---|---|---|---|
| `400` | `bad_request` | varies | Invalid body or new password outside 8–72 bytes |
| `401` | `unauthorized` | `missing, invalid or expired token` | No token, or invalid or expired token |
| `401` | `unauthorized` | `invalid credentials` | Wrong `currentPassword` |
| `401` | `unauthorized` | `account is disabled` | The account was deactivated but its token has not expired yet |

## Which routes need what

| Route | Auth |
|---|---|
| `/api/v1/auth/**` | public |
| `/actuator/health` | public |
| `/api/v1/ingestion/**` | `X-API-Key` header (no JWT) |
| everything else under `/api/v1/**` | `Authorization: Bearer <accessToken>`, otherwise `401` |
| `/ws` (STOMP) | handshake is open; `Authorization: Bearer <accessToken>` goes on the STOMP `CONNECT` frame |

### WebSocket (`/ws`)

Browsers cannot set headers on the WebSocket handshake, so the token travels as a STOMP header of the `CONNECT` frame. With `@stomp/stompjs`:

```js
const client = new Client({
  brokerURL: `wss://${location.host}/ws`,
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
});
```

- No token, a header without the `Bearer ` prefix, or an invalid, expired or foreign token: the server answers with a STOMP `ERROR` frame and closes the connection. Do not rely on the text of the `message` header.
- A `SUBSCRIBE` from a session that did not authenticate on `CONNECT` is rejected the same way.
- The token is only checked on `CONNECT`. An open connection is not closed when the token expires; after logging in again the dashboard reconnects with the new token.
