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

Every error has the shape `{ "error": "<message>" }`. Validation errors also include the failing fields:

```json
{ "error": "validation failed", "fields": { "email": "must be a well-formed email address" } }
```

| Status | When |
|---|---|
| `400` | Invalid body, malformed JSON, password outside 8–72 bytes, code not 6 digits |
| `401` `invalid credentials` | Wrong username or password, unknown user, or disabled account (same message on purpose) |
| `401` `invalid or expired code` | Wrong, expired, already used or revoked code, or 5 failed attempts (same message on purpose) |
| `409` | Username or email already in use |

## Rules

- Code: 6 digits, valid for **5 minutes**, single use, at most **5 attempts**. Asking for a new one (login) invalidates the previous ones.
- Token: JWT signed with HS256, valid for **30 minutes**. Claims: `sub` (user id, UUID), `iss` (`cuy-monitor-backend`), `iat`, `exp`. There is no refresh token: when it expires the user logs in again.
- Logout: the API is stateless. The dashboard just deletes the token; it stops working on its own when it expires.

## Which routes need what

| Route | Auth |
|---|---|
| `/api/v1/auth/**` | public |
| `/actuator/health` | public |
| `/api/v1/ingestion/**` | `X-API-Key` header (no JWT) |
| everything else under `/api/v1/**` | `Authorization: Bearer <accessToken>`, otherwise `401` |
