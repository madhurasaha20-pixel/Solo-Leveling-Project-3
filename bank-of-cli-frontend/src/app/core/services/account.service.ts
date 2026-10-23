import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Account, CreateAccountRequest, UpdateAccountRequest } from '../models';
import { toApiError } from './api-error.util';
import { AuthService } from './auth.service';

/**
 * The logged-in user's account and balance.
 *
 * Usage (Balance card):
 *   account$ = this.accounts.account$;            // template: (account$ | async)?.balance
 *   ngOnInit() { this.accounts.loadMyAccount().subscribe({ error: e => ... }); }
 *
 * account$ updates automatically after every deposit/withdraw/transfer,
 * so the Balance card never has to reload by hand.
 */
@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private api = environment.apiBase;

  private accountSubject = new BehaviorSubject<Account | null>(null);
  private accountsSubject = new BehaviorSubject<Account[]>([]);
  readonly accounts$: Observable<Account[]> = this.accountsSubject.asObservable();

  /** Current account, or null before loading / after logout. */
  readonly account$: Observable<Account | null> = this.accountSubject.asObservable();

  constructor() {
    inject(AuthService).currentUser$.subscribe(user => {
      if (!user) {
        this.accountSubject.next(null);
        this.accountsSubject.next([]);
      }
    });
  }

  get account(): Account | null {
    return this.accountSubject.value;
  }

  /** GET /accounts/me. Errors: UNAUTHORIZED, ACCOUNT_NOT_FOUND */
  loadMyAccount(): Observable<Account> {
    return this.http.get<Account>(`${this.api}/accounts/me`).pipe(
      tap(account => this.accountSubject.next(account)),
      catchError(err => throwError(() => toApiError(err)))
    );
  }

  /** GET /accounts. Returns the logged-in user's checking and savings accounts. */
  loadMyAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.api}/accounts`).pipe(
      tap(accounts => this.accountsSubject.next(accounts)),
      catchError(err => throwError(() => toApiError(err)))
    );
  }

  /** POST /accounts. Creates an account for the logged-in user. */
  createAccount(request: CreateAccountRequest): Observable<Account> {
    return this.http.post<Account>(`${this.api}/accounts`, request).pipe(
        tap(account => {
          this.accountsSubject.next([...this.accountsSubject.value, account]);
          this.accountSubject.next(account);
        }),
        catchError(err => throwError(() => toApiError(err)))
    );
  }

  /**
   * PATCH /accounts/{accountId}. Renames the account and/or changes its type.
   * Errors: VALIDATION_ERROR (field: nickname | type), UNAUTHORIZED, FORBIDDEN, ACCOUNT_NOT_FOUND
   */
  updateAccount(accountId: string, changes: UpdateAccountRequest): Observable<Account> {
    return this.http.patch<Account>(`${this.api}/accounts/${encodeURIComponent(accountId)}`, changes).pipe(
      tap(account => {
        // unlike updateCachedAccount, only touch account$ when it is the account that changed
        if (this.accountSubject.value?.id === account.id) this.accountSubject.next(account);
        this.accountsSubject.next(
          this.accountsSubject.value.map(existing => (existing.id === account.id ? account : existing))
        );
      }),
      catchError(err => throwError(() => toApiError(err)))
    );
  }

  /** Called by TransactionService after a successful transaction. Components don't need this. */
  updateCachedAccount(account: Account): void {
    this.accountSubject.next(account);

    const updatedAccounts = this.accountsSubject.value.map(existing =>
      existing.id === account.id ? account : existing
    );

    this.accountsSubject.next(updatedAccounts);
  }
}
