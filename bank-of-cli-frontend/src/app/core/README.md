# core/: Shared Services, Models, Contracts (owner: Joshua Easo)

**Service organization:** Shared services may be placed in `core/services`. Feature-specific services may remain within their corresponding feature folder. The location should follow the service's scope and ownership.

**Read `contracts/CONTRACTS.md` first.** It lists every endpoint, every JSON shape and every error code.

## Rules
- Components **never** use HttpClient or the mock JSON directly. Call a service.
- Import types from `core/models` (`import { Account, Transaction, ApiError } from '../core/models';`).
- Every service method returns an `Observable`. On failure it errors with an `ApiError`,
  so `err.message` can go straight into a toast and `err.field` tells you which input to highlight.
- Need a new field or endpoint? Message me, and we'll update CONTRACTS.md, the model and the mock together.

## Setup (one time, whoever owns AppModule)
```ts
import { provideCore } from './core/core.providers';

@NgModule({ ..., providers: [provideCore()] })
```

## Cheat sheet
| I'm building... | Use |
|---|---|
| Login page | `auth.login({ email, password })` |
| Register page | `auth.register({ name, email, password })` |
| Header / Nav (user name, logout) | `auth.currentUser$`, `auth.logout()` |
| Route guard | `auth.isLoggedIn()` |
| Balance card | `accounts.loadMyAccount()` once, then `accounts.account$` |
| Transaction history card | `transactions.loadRecent(accountId, 10)` once, then `transactions.recent$` |
| Deposit / Withdraw / Transfer forms | `transactions.deposit / withdraw / transfer(accountId, { amount, ... })` |

`account$` and `recent$` refresh automatically after any deposit, withdraw or transfer.

## Test logins
Login and register go to the real backend (run it on port 8080; `ng serve` proxies `/api` to it).
Register a user there. The old mock logins in CONTRACTS.md no longer work.

## How the mock works
`interceptors/mock-backend.interceptor.ts` pretends to be the server for every route except `/api/auth/*`,
with a ~0.7 to 1 second delay so loading states show. Data starts from `src/assets/mock-data/*.json` and
changes are kept until the tab closes. It only accepts the old mock tokens, so account routes answer
`401` to a real login until they are connected. When a route is live on the backend, delete its handler
from the mock; once none are left, set `useMockBackend: false` (or remove the mock).
