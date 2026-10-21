import {
    ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, inject, output, signal, viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { Account, ApiError } from '../../core/models';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { formatMoney } from '../pipes/money.pipe';
import { fieldError } from '../validators/form-errors';
import { AlertComponent } from './alert.component';
import { ButtonComponent } from './button.component';
import { SpinnerComponent } from './spinner.component';
import { TextFieldComponent } from './text-field.component';

let nextId = 0;

/** checking: loading balances · blocked: money left, must be moved out · ready: can confirm with password */
type Step = 'checking' | 'blocked' | 'ready';

/**
 * Confirmation dialog for permanently deleting the logged-in user's profile.
 * Every account must be at $0 first (the server enforces this too; see CONTRACTS.md, BALANCE_NOT_ZERO).
 * The user re-enters their password; on success AuthService clears the session and `deleted` fires.
 *
 *   <bc-delete-account-dialog #deleteDialog (deleted)="..." (closed)="..." />
 *   deleteDialog.open();
 *
 * Uses the native <dialog> element, so focus trapping, Escape and the backdrop come from the browser.
 */
@Component({
    selector: 'bc-delete-account-dialog',
    imports: [ReactiveFormsModule, AlertComponent, ButtonComponent, SpinnerComponent, TextFieldComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <dialog #dialog [attr.aria-labelledby]="titleId" [attr.aria-describedby]="descriptionId"
            (cancel)="onCancel($event)" (close)="onClose()"
            class="m-auto w-[calc(100%-2rem)] max-w-form rounded-md border bg-surface-raised p-6 text-left text-ink shadow-overlay backdrop:bg-scrim">
      <div class="flex flex-col gap-4">
        <div>
          <h2 [id]="titleId" class="text-heading-2 font-semibold text-ink">Delete your account?</h2>
          <p [id]="descriptionId" class="mt-2 text-body text-ink-muted">
            This permanently deletes your profile, all of your bank accounts and their transaction history.
            You can't undo this.
          </p>
        </div>

        @switch (step()) {
          @case ('checking') {
            <p class="flex items-center gap-2 text-body-sm text-ink-muted">
              <bc-spinner label="Checking your balances" />Checking your balances…
            </p>
            <div class="mt-2 flex justify-end">
              <bc-button variant="secondary" (click)="close()">Cancel</bc-button>
            </div>
          }

          @case ('blocked') {
            <bc-alert tone="warning" title="Empty your accounts first">{{ blockedMessage() }}</bc-alert>
            <div class="mt-2 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <bc-button variant="secondary" (click)="close()">Cancel</bc-button>
              <bc-button data-autofocus (click)="goToMoveMoney()">Go to Move money</bc-button>
            </div>
          }

          @case ('ready') {
            <form [formGroup]="form" (ngSubmit)="submit()" novalidate class="flex flex-col gap-4">
              @if (formError(); as err) {
                <bc-alert tone="error" title="We couldn't delete your account">
                  {{ err.message }}
                  <span class="mt-1 block font-mono text-code text-ink-muted">Error code: {{ err.code }}</span>
                </bc-alert>
              }

              <bc-text-field data-autofocus label="Password" type="password" autocomplete="current-password"
                             formControlName="password" hint="Enter your password to confirm."
                             [error]="passwordError()" />

              <div class="mt-2 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <bc-button variant="secondary" [disabled]="busy()" (click)="close()">Cancel</bc-button>
                <bc-button variant="danger" type="submit" [loading]="busy()">Delete account</bc-button>
              </div>
            </form>
          }
        }
      </div>
    </dialog>
  `,
})
export class DeleteAccountDialogComponent {
    /** The profile is gone and the session is cleared. Navigate to /login. */
    readonly deleted = output<void>();
    /** Closed without deleting (Cancel, Escape). */
    readonly closed = output<void>();

    private readonly auth = inject(AuthService);
    private readonly accounts = inject(AccountService);
    private readonly router = inject(Router);
    private readonly injector = inject(Injector);
    private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
    private balanceCheck?: Subscription;

    protected readonly titleId = `bc-delete-account-title-${++nextId}`;
    protected readonly descriptionId = `bc-delete-account-desc-${nextId}`;
    protected readonly isOpen = signal(false);
    protected readonly step = signal<Step>('checking');
    protected readonly blockedMessage = signal('');
    protected readonly busy = signal(false);
    protected readonly submitted = signal(false);
    private readonly apiError = signal<ApiError | null>(null);

    protected readonly form = inject(FormBuilder).nonNullable.group({
        password: ['', [Validators.required]],
    });

    open(): void {
        this.form.reset();
        this.submitted.set(false);
        this.apiError.set(null);
        this.busy.set(false);
        this.step.set('checking');
        this.isOpen.set(true);
        this.dialog().nativeElement.showModal();

        // fresh balances, so a deposit made a moment ago in another tab still counts
        this.balanceCheck?.unsubscribe();
        this.balanceCheck = this.accounts.loadMyAccounts().subscribe({
            next: accounts => this.applyBalances(accounts),
            // couldn't check (e.g. endpoint not built yet): let them try; the server refuses if money is left
            error: () => this.goTo('ready'),
        });
    }

    close(): void {
        if (this.busy()) return;
        this.dialog().nativeElement.close();
    }

    protected goToMoveMoney(): void {
        this.close();
        void this.router.navigateByUrl('/move-money');
    }

    protected passwordError(): string | null {
        const err = this.apiError();
        if (err?.field === 'password') return err.message;
        return fieldError(this.form.controls.password, this.submitted());
    }

    protected formError(): ApiError | null {
        const err = this.apiError();
        return err && err.field !== 'password' ? err : null;
    }

    protected submit(): void {
        if (this.busy()) return;
        this.submitted.set(true);
        this.apiError.set(null);
        if (this.form.invalid) return;

        this.busy.set(true);
        this.auth.deleteAccount(this.form.getRawValue().password).subscribe({
            next: () => {
                this.busy.set(false);
                this.isOpen.set(false);
                this.dialog().nativeElement.close();
                this.deleted.emit();
            },
            error: (err: ApiError) => {
                this.busy.set(false);
                if (err.code === 'BALANCE_NOT_ZERO') {
                    this.blockedMessage.set(err.message);
                    this.goTo('blocked');
                } else {
                    this.apiError.set(err);
                }
            },
        });
    }

    /** Escape: don't close while the request is in flight. */
    protected onCancel(event: Event): void {
        if (this.busy()) event.preventDefault();
    }

    protected onClose(): void {
        this.balanceCheck?.unsubscribe();
        this.form.reset(); // don't leave the typed password sitting in the hidden dialog
        if (!this.isOpen()) return; // already handled (successful delete)
        this.isOpen.set(false);
        this.closed.emit();
    }

    private applyBalances(accounts: Account[]): void {
        const remaining = Math.round(accounts.reduce((sum, a) => sum + a.balance, 0) * 100) / 100;
        if (remaining > 0) {
            this.blockedMessage.set(
                `You still have ${formatMoney(remaining)} in your accounts. ` +
                'Withdraw or transfer it first, so every account is at $0.',
            );
            this.goTo('blocked');
        } else {
            this.goTo('ready');
        }
    }

    /** Switches step, then moves focus to that step's main control once it has rendered. */
    private goTo(step: Step): void {
        this.step.set(step);
        afterNextRender(() => {
            this.dialog().nativeElement
                .querySelector<HTMLElement>('[data-autofocus] input, [data-autofocus] button')
                ?.focus();
        }, { injector: this.injector });
    }
}