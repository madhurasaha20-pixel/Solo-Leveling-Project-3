import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, distinctUntilChanged, filter, finalize, map, of, switchMap } from 'rxjs';

import { Account, ApiError, Transaction, isCredit } from '../../../core/models';
import { AccountService } from '../../../core/services/account.service';
import { TransactionItemComponent } from '../../../shared/components/transaction-item/transaction-item.component';
import { TransactionService } from '../transaction.service';
import { TransactionSummary } from './transaction-summary/transaction-summary';

@Component({
    selector: 'app-transaction-history',
    standalone: true,
    imports: [TransactionItemComponent, TransactionSummary],
    templateUrl: './transaction-history.component.html',
    // styleUrls: ['./transaction-history.component.css']
})

export class TransactionHistoryComponent {
    private readonly accounts = inject(AccountService);
    private readonly transactionService = inject(TransactionService);

    protected readonly transactions = toSignal(this.transactionService.recent$, {initialValue: [] as Transaction[],});
    protected readonly loading = signal(true);
    protected readonly error = signal<string | null>(null);

    protected readonly totalIn = computed(() => this.transactions().filter(isCredit).reduce((sum, t) => sum + t.amount, 0));
    protected readonly totalOut = computed(() => this.transactions().filter(t => !isCredit(t)).reduce((sum, t) => sum + t.amount, 0));

    constructor() {
        // Opening /transactions directly means the account isn't loaded yet
        if (!this.accounts.account) {
            this.accounts.loadMyAccount().pipe(takeUntilDestroyed()).subscribe({
                error: (e: ApiError) => {
                    this.error.set(e.message);
                    this.loading.set(false);
                },
            });
        }

        this.accounts.account$
            .pipe(
                filter((a): a is Account => a !== null),
                map(a => a.id),
                distinctUntilChanged(),
                switchMap(accountId => {
                    this.loading.set(true);
                    this.error.set(null);
                    return this.transactionService.loadRecent(accountId, 100).pipe(
                        catchError((e: ApiError) => {
                            this.error.set(e.message);
                            return of([]);
                        }),
                        finalize(() => this.loading.set(false))
                    );
                }),
                takeUntilDestroyed()
            )
            .subscribe();
    }

    protected label(t: Transaction): string {
        if (t.description) return t.description;
        switch (t.type) {
            case 'deposit': return 'Deposit';
            case 'withdraw': return 'Withdrawal';
            case 'transfer-in': return 'Transfer in';
            case 'transfer-out': return 'Transfer out';
        }
    }
}