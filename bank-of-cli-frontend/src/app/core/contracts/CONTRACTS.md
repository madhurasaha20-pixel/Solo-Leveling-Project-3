# Bank of CLI: API Contracts

This is the agreement between the frontend and the backend. The mock backend
(`core/interceptors/mock-backend.interceptor.ts`) follows it exactly for the routes it still
answers (everything except `/api/auth/*`, which goes to the real backend), and the real backend
must return the same shapes. **If you need a field changed, change it here first, then in
`core/models/`, then in the mock.**

- Base URL: `/api` (set in `environments/environment.ts` as `apiBase`)
- All bodies are JSON. All dates are ISO 8601 strings in UTC.
- Money is a plain number in dollars with at most 2 decimal places (`1325.75`).
- Endpoints marked 🔒 need the header `Authorization: Bearer <token>`.
  The `authTokenInterceptor` adds it automatically after login.

## Backend status

The Spring backend serves every endpoint under `/api` and authenticates with a JWT.

- The token is an opaque string to the frontend: never decode it, just send it back as
  `Authorization: Bearer <token>`. It is signed (HS256) and carries only the user's id; it has
  **no expiry yet**, so `logout` only needs to discard it on the client.
- Ids from the real backend are not `u1`-style: `User.id` and `Account.userId` are UUID strings.
  Treat all ids as opaque strings. (Account ids such as `ACC-1001` are unchanged.)
- A missing, malformed, wrongly signed or stale token gives `401 UNAUTHORIZED`; the
  `Authorization` header is ignored on login and register.
- The backend allows browser requests from the origin(s) in `CORS_ALLOWED_ORIGINS`
  (default `http://localhost:4200`).

| Endpoint | Backend |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout` | done |
| `PATCH /api/accounts/{accountId}` | done |
| `GET /api/accounts/me`, `GET /api/accounts` | not yet |
| `GET /api/accounts/{accountId}/transactions` | not yet |
| deposit, withdraw, transfer | not yet |

`GET /api/accounts` (all of the user's accounts) is called by `AccountService.loadMyAccounts()`
but has no section below yet.

---

## Data models

TypeScript versions live in `core/models/`.

### User
```json
{
  "id": "u1",
  "name": "Demo User",
  "email": "user@example.com",
  "createdAt": "2026-01-05T15:00:00.000Z"
}
```

### Account
`id` is also the public account number people type into the Transfer form.
```json
{
  "id": "ACC-1001",
  "userId": "u1",
  "type": "checking",
  "nickname": "Rainy day fund",
  "balance": 1325.75,
  "currency": "USD"
}
```
`nickname` is optional and left out when the owner hasn't set one.

### Transaction
| Field | Type | Notes |
|---|---|---|
| `id` | string | |
| `accountId` | string | The account this record belongs to |
| `type` | `"deposit"` \| `"withdraw"` \| `"transfer-in"` \| `"transfer-out"` | |
| `amount` | number | **Always positive.** Use `type` to show + or - |
| `balanceAfter` | number | Balance right after this transaction |
| `date` | string | ISO 8601 |
| `description` | string (optional) | |
| `counterpartyAccountId` | string (optional) | Only on transfers: the other account |

```json
{
  "id": "t4",
  "accountId": "ACC-1001",
  "type": "transfer-out",
  "amount": 150,
  "balanceAfter": 1013.58,
  "date": "2026-09-09T12:00:00.000Z",
  "description": "Concert tickets",
  "counterpartyAccountId": "ACC-1002"
}
```
A transfer creates **two** records: `transfer-out` on the sender's account and `transfer-in`
on the receiver's account.

### Error (every failed request)
```json
{
  "status": 422,
  "code": "INSUFFICIENT_FUNDS",
  "message": "Insufficient funds. Available balance is $25.00.",
  "field": "amount"
}
```
- `message` is written for users, so it can go straight into a toast.
- `field` (optional) names the form input at fault, so forms can highlight it.

| Code | HTTP status | When |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Missing or invalid input (see rules below) |
| `INVALID_CREDENTIALS` | 401 | Wrong email or password at login |
| `UNAUTHORIZED` | 401 | Missing or bad token on a 🔒 endpoint |
| `FORBIDDEN` | 403 | Account belongs to someone else |
| `ACCOUNT_NOT_FOUND` | 404 | Account id does not exist |
| `NOT_FOUND` | 404 | Unknown endpoint |
| `EMAIL_TAKEN` | 409 | Register with an email already in use |
| `INSUFFICIENT_FUNDS` | 422 | Withdraw or transfer more than the balance |
| `NETWORK_ERROR` | 0 | Server unreachable (created by the frontend, never sent by the server) |
| `UNKNOWN` | 500 | Anything else |

---

## Validation rules

The server enforces these. Forms should check the same rules **before** submitting so the
user sees errors instantly.

| Field | Rule | Message |
|---|---|---|
| `name` | Not empty | Name is required. |
| `email` | Valid email format | Enter a valid email address. |
| `password` (register) | At least 6 characters | Password must be at least 6 characters. |
| `amount` | A number | Amount must be a number. |
| `amount` | Greater than 0 | Amount must be greater than zero. |
| `amount` | At most 2 decimal places | Amount can have at most 2 decimal places. |
| `amount` | At most 1,000,000 | Amount cannot exceed $1,000,000. |
| `amount` (withdraw, transfer) | Not more than the balance | Insufficient funds. Available balance is $X. |
| `toAccountId` | Not empty, not your own account, must exist | (see endpoint) |
| `description` | Optional, at most 100 characters | Description must be 100 characters or fewer. |
| `nickname` | Optional, at most 30 characters (blank clears it) | Nickname must be 30 characters or fewer. |
| `type` (edit account) | `"checking"` or `"savings"` | Account type must be checking or savings. |

---

## Endpoints

### POST `/api/auth/login`
Request
```json
{ "email": "user@example.com", "password": "password" }
```
200 OK
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": { "id": "3f6c1a2e-8b4d-4e1f-9a7c-2d5e8f0b1c34", "name": "Demo User", "email": "user@example.com", "createdAt": "2026-01-05T15:00:00.000Z" }
}
```
Errors: `400 VALIDATION_ERROR` (empty field), `401 INVALID_CREDENTIALS`
```json
{ "status": 401, "code": "INVALID_CREDENTIALS", "message": "Incorrect email or password." }
```

### POST `/api/auth/register`
Creates the user, then logs them in. No accounts are created; the user opens them later.

Request
```json
{ "name": "Alex Kim", "email": "alex@example.com", "password": "secret1" }
```
201 Created: same body as login.

Errors: `400 VALIDATION_ERROR`, `409 EMAIL_TAKEN`
```json
{ "status": 409, "code": "EMAIL_TAKEN", "message": "An account with that email already exists.", "field": "email" }
```

### POST `/api/auth/logout` 🔒
No body. `204 No Content`.

### GET `/api/accounts/me` 🔒
200 OK: an **Account**.

Errors: `401 UNAUTHORIZED`, `404 ACCOUNT_NOT_FOUND`

### GET `/api/accounts/{accountId}/transactions?limit=10` 🔒
`limit` is optional (default 10, max 100).

200 OK: an array of **Transaction**, newest first.

Errors: `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404 ACCOUNT_NOT_FOUND`

### POST `/api/accounts/{accountId}/deposit` 🔒
Request
```json
{ "amount": 100.5, "description": "Birthday money" }
```
200 OK
```json
{
  "transaction": {
    "id": "t1004", "accountId": "ACC-1001", "type": "deposit", "amount": 100.5,
    "balanceAfter": 1426.25, "date": "2026-10-05T14:02:11.000Z", "description": "Birthday money"
  },
  "account": { "id": "ACC-1001", "userId": "u1", "type": "checking", "balance": 1426.25, "currency": "USD" }
}
```
Errors: `400 VALIDATION_ERROR`, `401`, `403`, `404`

### POST `/api/accounts/{accountId}/withdraw` 🔒
Same request and response shape as deposit (`type` is `"withdraw"`).

Errors: `400 VALIDATION_ERROR`, `422 INSUFFICIENT_FUNDS`, `401`, `403`, `404`

### PATCH `/api/accounts/{accountId}` 🔒
Edits one of **your** accounts. Send only what changes; a blank `nickname` clears it.

Request
```json
{ "nickname": "Rainy day fund", "type": "savings" }
```
200 OK: the updated **Account**.

Errors: `400 VALIDATION_ERROR` (`field: "nickname"` or `"type"`), `401`, `403 FORBIDDEN`, `404 ACCOUNT_NOT_FOUND`

### POST `/api/accounts/{accountId}/transfer` 🔒
Request
```json
{ "toAccountId": "ACC-1002", "amount": 25, "description": "Lunch" }
```
200 OK: same shape as deposit. `transaction` is the `transfer-out` record on **your** account,
and `account` is **your** updated account.

Errors:
- `400 VALIDATION_ERROR` (`field: "toAccountId"`) when empty or your own account
- `404 ACCOUNT_NOT_FOUND` (`field: "toAccountId"`) when the destination doesn't exist
- `422 INSUFFICIENT_FUNDS`
- `401`, `403`

---

## Mock test accounts

From `src/assets/mock-data/`. Changes are kept in sessionStorage until the tab closes.

> **These logins no longer work.** Login and register now go to the real backend, so register a
> user there instead. The mock's account routes only accept the old mock tokens, so they answer
> `401 UNAUTHORIZED` to a real token until each route is connected to the backend.

| Email | Password | Account | Starting balance | Good for testing |
|---|---|---|---|---|
| user@example.com | password | ACC-1001 | $1,325.75 | Normal flows, full history |
| jordan@example.com | password123 | ACC-1002 | $2,884.50 | Transfer target |
| sam@example.com | password123 | ACC-1003 | $25.00 | Insufficient funds |
