import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Account } from '../../core/models';
import { AccountService } from '../../core/services/account.service';
import { ToastComponent } from '../../shared/toast/toast.component';
import { TransactionService } from './transaction.service';
import { Observable, finalize } from 'rxjs';
import { TransactionResponse } from '../../core/models/transaction';

type TransactionAction = 'deposit' | 'withdraw' | 'transfer';

@Component({
    selector: 'app-transactions',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, ToastComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './transactions.components.html',
})
export class TransactionsComponent {
    private readonly fb = inject(FormBuilder);
    private readonly transactionsService = inject(TransactionService);
    private readonly accountService = inject(AccountService);

    protected readonly accountList = signal<Account[]>([]);
    protected readonly currentAccount = signal(this.accountService.account);

    protected readonly transactionTypes: TransactionAction[] = [
        'deposit',
        'withdraw',
        'transfer',
    ];

    protected readonly selectedType = signal<TransactionAction>('deposit');
    protected readonly submitted = signal(false);
    protected readonly toastMessage = signal('');
    protected readonly toastType = signal<'success' | 'error'>('success');
    protected readonly loading = signal(false);

    protected readonly form = this.fb.group({
        fromAccountId: [''],
        amount: [
            null as number | null,
            [Validators.required, Validators.min(0.01)],
        ],
        description: [''],
        toAccountId: [''],
    });

    protected readonly helpText = computed(() => {
        switch (this.selectedType()) {
            case 'deposit':
                return 'Add funds to your account in a single step.';
            case 'withdraw':
                return 'Move money out of your account without leaving any blank fields.';
            case 'transfer':
                return 'Send money to another account using a valid destination account number.';
            default:
                return '';
        }
    });

    constructor() {
        this.accountService.loadMyAccounts().subscribe({
            next: (accounts) => {
                this.accountList.set(accounts);

                const current =
                    accounts.find((account) => account.type === 'checking') ??
                    accounts[0] ??
                    null;

                this.currentAccount.set(current);

                if (current) {
                    this.accountService.updateCachedAccount(current);
                    this.form.controls.fromAccountId.setValue(current.id);
                }
            },
            error: (err) => {
                this.toastMessage.set(
                    err?.message ?? 'Could not load your accounts.',
                );
                this.toastType.set('error');
            },
        });
    }

    protected setTransactionType(type: TransactionAction): void {
        this.selectedType.set(type);

        if (type === 'transfer') {
            this.form.controls.toAccountId.setValidators([
                Validators.required,
                Validators.minLength(3),
            ]);
        } else {
            this.form.controls.toAccountId.clearValidators();
        }

        this.form.controls.toAccountId.updateValueAndValidity();
    }

    protected onSourceAccountChange(accountId: string): void {
        const account = this.accountList().find(
            (existing) => existing.id === accountId,
        );

        this.currentAccount.set(account ?? null);

        if (account) {
            this.accountService.updateCachedAccount(account);
        }
    }

    protected isFieldInvalid(
        controlName: 'amount' | 'toAccountId',
    ): boolean {
        const control = this.form.get(controlName);

        return (
            !!control &&
            control.invalid &&
            (control.touched || this.submitted())
        );
    }

    protected submit(): void {
        this.submitted.set(true);
        this.form.markAllAsTouched();

        if (this.selectedType() === 'transfer') {
            this.form.controls.toAccountId.setValidators([
                Validators.required,
                Validators.minLength(3),
            ]);
            this.form.controls.toAccountId.updateValueAndValidity();
        }

        if (this.form.invalid) {
            return;
        }

        const accountId = this.form.value.fromAccountId;

        if (!accountId) {
            this.toastMessage.set('Please select a source account.');
            this.toastType.set('error');
            return;
        }

        const amount = Number(this.form.value.amount);
        const description = this.form.value.description?.trim() ?? '';
        const destinationAccountId = this.form.value.toAccountId ?? '';
        const action = this.selectedType();

        let call$: Observable<TransactionResponse>;

        if (action === 'deposit') {
            call$ = this.transactionsService.deposit(accountId, {
                amount,
                description,
            });
        } else if (action === 'withdraw') {
            call$ = this.transactionsService.withdraw(accountId, {
                amount,
                description,
            });
        } else {
            call$ = this.transactionsService.transfer(accountId, {
                destinationAccountId,
                amount,
                description,
            });
        }

        let transactionResponse: TransactionResponse | null = null;

        this.loading.set(true);
        const startedAt = Date.now();
        let succeeded = false;

        call$
            .pipe(
                finalize(() => {
                    const elapsed = Date.now() - startedAt;
                    const remaining = Math.max(0, 700 - elapsed);

                    setTimeout(() => {
                        this.loading.set(false);

                        if (succeeded && transactionResponse) {
                            const operation =
                                action === 'withdraw'
                                    ? 'Withdrawal'
                                    : action === 'transfer'
                                        ? 'Transfer'
                                        : 'Deposit';

                            const balance = new Intl.NumberFormat('en-US', {
                                style: 'currency',
                                currency: 'USD',
                            }).format(transactionResponse.newBalance);

                            this.toastMessage.set(
                                `${operation} succeeded. New balance: ${balance}`,
                            );
                            this.toastType.set('success');
                        }
                    }, remaining);
                }),
            )
            .subscribe({
                next: (response) => {
                    succeeded = true;
                    transactionResponse = response;

                    this.accountService.loadMyAccounts().subscribe({
                        next: (accounts) => {
                            this.accountList.set(accounts);

                            const current = accounts.find(
                                (account) => account.id === accountId,
                            );

                            if (current) {
                                this.currentAccount.set(current);
                                this.accountService.updateCachedAccount(current);
                            }
                        },
                        error: (err) => {
                            console.error('Failed to refresh accounts:', err);
                        },
                    });

                    this.transactionsService.loadRecent(accountId).subscribe({
                        error: (err) => {
                            console.error(
                                'Failed to refresh transaction history:',
                                err,
                            );
                        },
                    });

                    this.form.reset({
                        fromAccountId: accountId,
                        amount: null,
                        description: '',
                        toAccountId: '',
                    });

                    this.form.markAsPristine();
                    this.submitted.set(false);
                },
                error: (err) => {
                    this.toastMessage.set(
                        err?.message ?? 'Transaction could not be processed.',
                    );
                    this.toastType.set('error');
                },
            });
    }
}