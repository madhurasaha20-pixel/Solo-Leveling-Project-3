import {
  HttpErrorResponse,
  HttpEvent,
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, delay, mergeMap, of, throwError, timer } from 'rxjs';

import { environment } from '../../../environments/environment';
import { MockDb, UserRecord } from '../mock/mock-db';
import {
  Account,
  ApiError,
  ApiErrorCode,
  Transaction,
  TransactionResponse,
  TransactionType
} from '../models';

/**
 * THE MOCK SERVER.
 *
 * Services make normal HttpClient calls to environment.apiBase (e.g. GET /api/accounts).
 * This interceptor catches those calls and answers them exactly as CONTRACTS.md says
 * the real backend will, including status codes, error bodies and a fake delay.
 * /api/auth/* is not mocked: it always goes to the real backend.
 *
 * As each route goes live on the backend, delete its handler here. Once none are left,
 * set environment.useMockBackend = false. No service or component has to change.
 */
export const mockBackendInterceptor: HttpInterceptorFn = (req, next) => {
  if (!environment.useMockBackend || !req.url.startsWith(environment.apiBase)) {
    return next(req);
  }
  const path = req.url.slice(environment.apiBase.length).split('?')[0];
  // auth is served by the real backend; only the account routes are still mocked
  if (path.startsWith('/auth/')) {
    return next(req);
  }
  const db = inject(MockDb);
  return route(req, path, db);
};

// ---------------------------------------------------------------------------
// Routing
// ---------------------------------------------------------------------------

function route(req: HttpRequest<unknown>, path: string, db: MockDb): Observable<HttpEvent<unknown>> {
  const { method } = req;
  let m: RegExpMatchArray | null;

  if (method === 'GET' && path === '/accounts/me') return myAccount(req, db);
  if (method === 'GET' && path === '/accounts') return myAccounts(req, db);

  if (method === 'GET' && (m = path.match(/^\/accounts\/([^/]+)\/transactions$/))) {
    return listTransactions(req, db, decodeURIComponent(m[1]));
  }
  if (method === 'PATCH' && (m = path.match(/^\/accounts\/([^/]+)$/))) {
    return updateAccount(req, db, decodeURIComponent(m[1]));
  }
  if (method === 'POST' && (m = path.match(/^\/accounts\/([^/]+)\/(deposit|withdraw|transfer)$/))) {
    return moveMoney(req, db, decodeURIComponent(m[1]), m[2] as 'deposit' | 'withdraw' | 'transfer');
  }

  return fail(404, 'NOT_FOUND', `No mock endpoint for ${method} ${path}`);
}
function deleteMe(req: HttpRequest<unknown>, db: MockDb) {
  const user = currentUser(req, db);
  if (!user) return unauthorized();

  const password = str(asRecord(req.body)['password']);
  if (!password) return fail(400, 'VALIDATION_ERROR', 'Password is required.', 'password');
  if (password !== user.password) return fail(400, 'VALIDATION_ERROR', 'Incorrect password.', 'password');

  // every account must be emptied first, so no money disappears
  const remaining = money(db.accounts.filter(a => a.userId === user.id).reduce((sum, a) => sum + a.balance, 0));
  if (remaining > 0) {
    const amount = remaining.toLocaleString('en-US', { style: 'currency', currency: 'USD' });
    return fail(409, 'BALANCE_NOT_ZERO',
        `You still have ${amount} in your accounts. Withdraw or transfer it first, so every account is at $0.`);
  }

  // same order as the real backend: history, then accounts, then the user
  const accountIds = new Set(db.accounts.filter(a => a.userId === user.id).map(a => a.id));
  removeWhere(db.transactions, t => accountIds.has(t.accountId));
  removeWhere(db.accounts, a => a.userId === user.id);
  removeWhere(db.users, u => u.id === user.id);
  db.save();
  return ok(null, 204);
}

/** Removes matching items in place (MockDb exposes its arrays, not setters). */
function removeWhere<T>(items: T[], matches: (item: T) => boolean): void {
  for (let i = items.length - 1; i >= 0; i--) {
    if (matches(items[i])) items.splice(i, 1);
  }
}

// ---------------------------------------------------------------------------
// Account endpoints
// ---------------------------------------------------------------------------

function myAccount(req: HttpRequest<unknown>, db: MockDb) {
  const user = currentUser(req, db);
  if (!user) return unauthorized();

  const account = db.accounts.find(a => a.userId === user.id);
  if (!account) return fail(404, 'ACCOUNT_NOT_FOUND', 'No account found for this user.');
  return ok({ ...account });
}

function myAccounts(req: HttpRequest<unknown>, db: MockDb) {
  const user = currentUser(req, db);
  if (!user) return unauthorized();

  const accounts = db.accounts
    .filter(a => a.userId === user.id)
    .map(a => ({ ...a }));

  return ok(accounts);
}

function updateAccount(req: HttpRequest<unknown>, db: MockDb, accountId: string) {
  const user = currentUser(req, db);
  if (!user) return unauthorized();

  const account = db.accounts.find(a => a.id === accountId);
  if (!account) return fail(404, 'ACCOUNT_NOT_FOUND', `Account ${accountId} does not exist.`);
  if (account.userId !== user.id) return fail(403, 'FORBIDDEN', 'You do not have access to this account.');

  const body = asRecord(req.body);
  if (body['nickname'] !== undefined && body['nickname'] !== null) {
    const nickname = str(body['nickname']).trim();
    if (nickname.length > 30) {
      return fail(400, 'VALIDATION_ERROR', 'Nickname must be 30 characters or fewer.', 'nickname');
    }
    if (nickname) account.nickname = nickname;
    else delete account.nickname;
  }
  if (body['type'] !== undefined && body['type'] !== null) {
    const type = str(body['type']);
    if (type !== 'checking' && type !== 'savings') {
      return fail(400, 'VALIDATION_ERROR', 'Account type must be checking or savings.', 'type');
    }
    account.type = type;
  }

  db.save();
  return ok({ ...account });
}

function listTransactions(req: HttpRequest<unknown>, db: MockDb, accountId: string) {
  const user = currentUser(req, db);
  if (!user) return unauthorized();

  const account = db.accounts.find(a => a.id === accountId);
  if (!account) return fail(404, 'ACCOUNT_NOT_FOUND', `Account ${accountId} does not exist.`);
  if (account.userId !== user.id) return fail(403, 'FORBIDDEN', 'You do not have access to this account.');

  const limitParam = Number(req.params.get('limit') ?? 10);
  const limit = Number.isInteger(limitParam) && limitParam > 0 ? Math.min(limitParam, 100) : 10;

  const list: Transaction[] = db.transactions
    .filter(t => t.accountId === accountId)
    .sort((a, b) => b.date.localeCompare(a.date)) // newest first
    .slice(0, limit)
    .map(t => ({ ...t }));
  return ok(list);
}

function moveMoney(
  req: HttpRequest<unknown>,
  db: MockDb,
  accountId: string,
  action: 'deposit' | 'withdraw' | 'transfer'
) {
  const user = currentUser(req, db);
  if (!user) return unauthorized();

  const account = db.accounts.find(a => a.id === accountId);
  if (!account) return fail(404, 'ACCOUNT_NOT_FOUND', `Account ${accountId} does not exist.`);
  if (account.userId !== user.id) return fail(403, 'FORBIDDEN', 'You do not have access to this account.');

  const body = asRecord(req.body);
  const amount = body['amount'];
  const description = str(body['description']).trim() || undefined;

  const amountError = validateAmount(amount);
  if (amountError) return fail(400, 'VALIDATION_ERROR', amountError, 'amount');
  if (description && description.length > 100) {
    return fail(400, 'VALIDATION_ERROR', 'Description must be 100 characters or fewer.', 'description');
  }
  const value = amount as number;

  // ---- deposit ----
  if (action === 'deposit') {
    account.balance = money(account.balance + value);
    const tx = record(db, account, 'deposit', value, description ?? 'Deposit');
    db.save();
    return ok<TransactionResponse>({ transaction: { ...tx }, account: { ...account } });
  }

  // ---- withdraw ----
  if (action === 'withdraw') {
    if (value > account.balance) {
      return fail(422, 'INSUFFICIENT_FUNDS', `Insufficient funds. Available balance is $${account.balance.toFixed(2)}.`, 'amount');
    }
    account.balance = money(account.balance - value);
    const tx = record(db, account, 'withdraw', value, description ?? 'Withdrawal');
    db.save();
    return ok<TransactionResponse>({ transaction: { ...tx }, account: { ...account } });
  }

  // ---- transfer ----
  const toAccountId = str(body['toAccountId']).trim().toUpperCase();
  if (!toAccountId) {
    return fail(400, 'VALIDATION_ERROR', 'Destination account is required.', 'toAccountId');
  }
  if (toAccountId === account.id) {
    return fail(400, 'VALIDATION_ERROR', 'You cannot transfer to the same account.', 'toAccountId');
  }
  const target = db.accounts.find(a => a.id === toAccountId);
  if (!target) {
    return fail(404, 'ACCOUNT_NOT_FOUND', `Account ${toAccountId} does not exist.`, 'toAccountId');
  }
  if (value > account.balance) {
    return fail(422, 'INSUFFICIENT_FUNDS', `Insufficient funds. Available balance is $${account.balance.toFixed(2)}.`, 'amount');
  }

  account.balance = money(account.balance - value);
  target.balance = money(target.balance + value);
  const out = record(db, account, 'transfer-out', value, description ?? `Transfer to ${target.id}`, target.id);
  record(db, target, 'transfer-in', value, description ?? `Transfer from ${account.id}`, account.id);
  db.save();
  return ok<TransactionResponse>({ transaction: { ...out }, account: { ...account } });
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function record(
  db: MockDb,
  account: Account,
  type: TransactionType,
  amount: number,
  description: string,
  counterpartyAccountId?: string
): Transaction {
  const tx: Transaction = {
    id: db.newId('t'),
    accountId: account.id,
    type,
    amount,
    balanceAfter: account.balance,
    date: new Date().toISOString(),
    description,
    ...(counterpartyAccountId ? { counterpartyAccountId } : {})
  };
  db.transactions.push(tx);
  return tx;
}

/** Server-side rules. The forms should check the same things first. */
function validateAmount(amount: unknown): string | null {
  if (typeof amount !== 'number' || !Number.isFinite(amount)) return 'Amount must be a number.';
  if (amount <= 0) return 'Amount must be greater than zero.';
  if (Math.abs(amount * 100 - Math.round(amount * 100)) > 1e-6) return 'Amount can have at most 2 decimal places.';
  if (amount > 1_000_000) return 'Amount cannot exceed $1,000,000.';
  return null;
}

function money(n: number): number {
  return Math.round(n * 100) / 100;
}

function currentUser(req: HttpRequest<unknown>, db: MockDb): UserRecord | undefined {
  const header = req.headers.get('Authorization') ?? '';
  const match = header.match(/^Bearer mock-token\.(.+)$/);
  return match ? db.users.find(u => u.id === match[1]) : undefined;
}

function asRecord(body: unknown): Record<string, unknown> {
  return body && typeof body === 'object' ? (body as Record<string, unknown>) : {};
}

function str(v: unknown): string {
  return typeof v === 'string' ? v : '';
}

function latency(): number {
  // a little jitter so it feels like a real network
  return environment.mockLatencyMs + Math.floor(Math.random() * 300);
}

function ok<T>(body: T, status = 200): Observable<HttpEvent<T>> {
  return of(new HttpResponse<T>({ status, body })).pipe(delay(latency()));
}

function fail(status: number, code: ApiErrorCode, message: string, field?: string): Observable<never> {
  const error: ApiError = { status, code, message, ...(field ? { field } : {}) };
  return timer(latency()).pipe(
    mergeMap(() => throwError(() => new HttpErrorResponse({ status, error, statusText: code })))
  );
}

function unauthorized(): Observable<never> {
  return fail(401, 'UNAUTHORIZED', 'Your session has expired. Please log in again.');
}
