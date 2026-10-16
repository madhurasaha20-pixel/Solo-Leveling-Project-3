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
  AuthResponse,
  Transaction,
  TransactionResponse,
  TransactionType,
  User
} from '../models';

/**
 * THE MOCK SERVER.
 *
 * Services make normal HttpClient calls to environment.apiBase (e.g. POST /api/auth/login).
 * This interceptor catches those calls and answers them exactly as CONTRACTS.md says
 * the real backend will, including status codes, error bodies and a fake delay.
 *
 * When the real backend is ready: set environment.useMockBackend = false.
 * No service or component has to change.
 */
export const mockBackendInterceptor: HttpInterceptorFn = (req, next) => {
  if (!environment.useMockBackend || !req.url.startsWith(environment.apiBase)) {
    return next(req);
  }
  const db = inject(MockDb);
  const path = req.url.slice(environment.apiBase.length).split('?')[0];
  return route(req, path, db);
};

// ---------------------------------------------------------------------------
// Routing
// ---------------------------------------------------------------------------

function route(req: HttpRequest<unknown>, path: string, db: MockDb): Observable<HttpEvent<unknown>> {
  const { method } = req;
  let m: RegExpMatchArray | null;

  if (method === 'POST' && path === '/auth/login') return login(req, db);
  if (method === 'POST' && path === '/auth/register') return register(req, db);
  if (method === 'POST' && path === '/auth/logout') return ok(null, 204);

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

// ---------------------------------------------------------------------------
// Auth endpoints
// ---------------------------------------------------------------------------

function login(req: HttpRequest<unknown>, db: MockDb) {
  const body = asRecord(req.body);
  const email = str(body['email']).trim().toLowerCase();
  const password = str(body['password']);

  if (!email) return fail(400, 'VALIDATION_ERROR', 'Email is required.', 'email');
  if (!password) return fail(400, 'VALIDATION_ERROR', 'Password is required.', 'password');

  const user = db.users.find(u => u.email.toLowerCase() === email);
  if (!user || user.password !== password) {
    // Same message either way, so the UI can't reveal which emails exist
    return fail(401, 'INVALID_CREDENTIALS', 'Incorrect email or password.');
  }
  const res: AuthResponse = { token: tokenFor(user), user: publicUser(user) };
  return ok(res);
}

function register(req: HttpRequest<unknown>, db: MockDb) {
  const body = asRecord(req.body);
  const name = str(body['name']).trim();
  const email = str(body['email']).trim().toLowerCase();
  const password = str(body['password']);

  if (!name) return fail(400, 'VALIDATION_ERROR', 'Name is required.', 'name');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return fail(400, 'VALIDATION_ERROR', 'Enter a valid email address.', 'email');
  }
  if (password.length < 6) {
    return fail(400, 'VALIDATION_ERROR', 'Password must be at least 6 characters.', 'password');
  }
  if (db.users.some(u => u.email.toLowerCase() === email)) {
    return fail(409, 'EMAIL_TAKEN', 'An account with that email already exists.', 'email');
  }

  const user: UserRecord = {
    id: db.newId('u'),
    name,
    email,
    password,
    createdAt: new Date().toISOString()
  };
  const checkingAccount: Account = {
  id: db.newId('ACC-'),
  userId: user.id,
  type: 'checking',
  balance: 0,
  currency: 'USD'
};

const savingsAccount: Account = {
  id: db.newId('ACC-'),
  userId: user.id,
  type: 'savings',
  balance: 0,
  currency: 'USD'
};
  db.users.push(user);
  db.accounts.push(checkingAccount, savingsAccount);
  db.save();

  const res: AuthResponse = { token: tokenFor(user), user: publicUser(user) };
  return ok(res, 201);
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

function tokenFor(user: User): string {
  return `mock-token.${user.id}`;
}

function currentUser(req: HttpRequest<unknown>, db: MockDb): UserRecord | undefined {
  const header = req.headers.get('Authorization') ?? '';
  const match = header.match(/^Bearer mock-token\.(.+)$/);
  return match ? db.users.find(u => u.id === match[1]) : undefined;
}

function publicUser(u: UserRecord): User {
  return { id: u.id, name: u.name, email: u.email, createdAt: u.createdAt };
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
