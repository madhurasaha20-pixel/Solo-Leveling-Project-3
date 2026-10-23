import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  DepositRequest,
  Transaction,
  TransactionResponse,
  TransferRequest,
  WithdrawRequest,
} from '../../core/models';
import { toApiError } from '../../core/services/api-error.util';
import { AuthService } from '../../core/services/auth.service';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly api = environment.apiBase;

  private readonly recentSubject = new BehaviorSubject<Transaction[]>([]);

  readonly recent$: Observable<Transaction[]> =
      this.recentSubject.asObservable();

  constructor() {
    this.auth.currentUser$.subscribe((user) => {
      if (!user) {
        this.recentSubject.next([]);
      }
    });
  }

  loadRecent(accountId: string, limit = 10): Observable<Transaction[]> {
    const params = new HttpParams().set('limit', String(limit));

    return this.http
        .get<Transaction[]>(
            `${this.api}/accounts/${encodeURIComponent(accountId)}/transactions`,
            { params },
        )
        .pipe(
            tap((transactions) => {
              this.recentSubject.next(transactions);
            }),
            catchError((err) => throwError(() => toApiError(err))),
        );
  }

  deposit(
      accountId: string,
      req: DepositRequest,
  ): Observable<TransactionResponse> {
    return this.post(accountId, 'deposit', req);
  }

  withdraw(
      accountId: string,
      req: WithdrawRequest,
  ): Observable<TransactionResponse> {
    return this.post(accountId, 'withdraw', req);
  }

  transfer(
      accountId: string,
      req: TransferRequest,
  ): Observable<TransactionResponse> {
    return this.post(accountId, 'transfer', req);
  }

  private post(
      accountId: string,
      action: 'deposit' | 'withdraw' | 'transfer',
      body: DepositRequest | WithdrawRequest | TransferRequest,
  ): Observable<TransactionResponse> {
    return this.http
        .post<TransactionResponse>(
            `${this.api}/accounts/${encodeURIComponent(accountId)}/${action}`,
            body,
        )
        .pipe(
            catchError((err) => throwError(() => toApiError(err))),
        );
  }
}