
import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, finalize } from 'rxjs';
import { Account } from '../../core/models';
import { AccountService } from '../../core/services/account.service';
import { ToastComponent } from '../../shared/toast/toast.component';
import { TransactionService } from './transaction.service';
import { WireService } from './wire.service';

type TransactionAction = 'deposit' | 'withdraw' | 'transfer' | 'wire';

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
    private readonly wireService = inject(WireService);

    protected readonly accountList = signal<Account[]>([]);
    protected readonly currentAccount = signal(this.accountService.account);

    protected readonly transactionTypes: TransactionAction[] = [
        'deposit',
        'withdraw',
        'transfer',
        'wire',
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
        toEmail: ['', [Validators.required, Validators.email]], // WIRE
    });

    protected readonly helpText = computed(() => {
        switch (this.selectedType()) {
            case 'deposit':
                return 'Add funds to your account in a single step.';
            case 'withdraw':
                return 'Move money out of your account without leaving any blank fields.';
            case 'transfer':
                return 'Send money to another account using a valid destination account number.';
            case 'wire':
                return 'Send money to anyone using their email address.';
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

        this.form.controls.toAccountId.setValue('');
        this.form.controls.toEmail.setValue(''); // WIRE

        if (type === 'transfer') {
            this.form.controls.toAccountId.setValidators([
                Validators.required,
                Validators.minLength(3),
            ]);
        } else {
            this.form.controls.toAccountId.clearValidators();
        }

        if (type === 'wire') { // WIRE
            this.form.controls.toEmail.setValidators([ // WIRE
                Validators.required, // WIRE
                Validators.email, // WIRE
            ]); // WIRE
        } else { // WIRE
            this.form.controls.toEmail.clearValidators(); // WIRE
        } // WIRE

        this.form.controls.toAccountId.updateValueAndValidity();
        this.form.controls.toEmail.updateValueAndValidity(); // WIRE
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

        const action = this.selectedType(); // WIRE

        if (action === 'transfer') { // WIRE
            this.form.controls.toAccountId.setValidators([ // WIRE
                Validators.required, // WIRE
                Validators.minLength(3), // WIRE
            ]); // WIRE
        } else { // WIRE
            this.form.controls.toAccountId.clearValidators(); // WIRE
        } // WIRE

        if (action === 'wire') { // WIRE
            this.form.controls.toEmail.setValidators([ // WIRE
                Validators.required, // WIRE
                Validators.email, // WIRE
            ]); // WIRE
        } else { // WIRE
            this.form.controls.toEmail.clearValidators(); // WIRE
        } // WIRE

        this.form.controls.toAccountId.updateValueAndValidity(); // WIRE
        this.form.controls.toEmail.updateValueAndValidity(); // WIRE

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
        const recipientEmail = this.form.controls.toEmail.value?.trim() ?? ''; // WIRE

        let call$: Observable<unknown>;

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
        } else if (action === 'transfer') {
            call$ = this.transactionsService.transfer(accountId, {
                destinationAccountId,
                amount,
                description,
            });
        } else { // WIRE
            call$ = this.wireService.wire({ // WIRE
                fromAccountId: accountId, // WIRE
                toEmail: recipientEmail, // WIRE
                amount, // WIRE
            }); // WIRE
        }

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

                        if (succeeded) {
                            this.toastMessage.set(
                                `${action.charAt(0).toUpperCase() + action.slice(1)} of $${amount.toFixed(2)} submitted successfully.`,
                            );
                            this.toastType.set('success');
                        }
                    }, remaining);
                }),
            )
            .subscribe({
                next: () => {
                    succeeded = true;

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
                        toEmail: '', // WIRE
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
