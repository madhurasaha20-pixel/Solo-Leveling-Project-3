import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';

import { Account, ApiError, UpdateAccountRequest } from '../../core/models';
import { AccountService } from '../../core/services/account.service';
import { ToastComponent } from '../../shared/toast/toast.component';
import { PageShellComponent } from '../page-shell.component';
import { BalanceCardComponent } from './balance-card.component';
import { EditAccountCardComponent } from './edit-account-card.component';

/**
 * Dashboard page. Pages call services; the cards below only receive data.
 * Layout: 3 columns on large screens. The balance card takes 1; the
 * transaction history card should go next to it with class="lg:col-span-2".
 */
@Component({
  selector: 'app-dashboard',
  imports: [PageShellComponent, BalanceCardComponent, EditAccountCardComponent, ToastComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-shell />
    <div class="mt-8 grid gap-6 lg:grid-cols-3">

    
  @if (account(); as acct) {
  @if (editing(acct); as editingAcct) {
    <app-edit-account-card
      [account]="editingAcct"
      [saving]="saving()"
      [error]="editError()"
      (save)="saveEdit(editingAcct.id, $event)"
      (cancel)="cancelEdit()"
    />
  } @else {
    <app-balance-card
      [account]="acct"
      [updatedAt]="updatedAt()"
      [error]="error()"
      (retry)="refresh()"
      (edit)="startEdit($event)"
    />
  }
}

  @if (savingsAccount(); as acct) {
  @if (editing(acct); as editingAcct) {
    <app-edit-account-card
      [account]="editingAcct"
      [saving]="saving()"
      [error]="editError()"
      (save)="saveEdit(editingAcct.id, $event)"
      (cancel)="cancelEdit()"
    />
  } @else {
    <app-balance-card
      title="Savings"
      [account]="acct"
      [updatedAt]="updatedAt()"
      [error]="error()"
      (retry)="refresh()"
      (edit)="startEdit($event)"
    />
  }
}
      
      <!-- Transaction history card goes here: <app-... class="lg:col-span-2" /> -->
    </div>
    <app-toast [message]="toastMessage()" [type]="toastType()" />
  `,
})
export class DashboardComponent {
  private readonly accounts = inject(AccountService);
  private readonly destroyRef = inject(DestroyRef);

  /** Cached account; updates by itself after every deposit, withdraw or transfer. */
  protected readonly account = toSignal(this.accounts.account$, { initialValue: null });
  protected readonly accountList = toSignal(this.accounts.accounts$, { initialValue: [] as Account[] });
  /** The user's other account (savings by default). Not matched by type, since the type can be edited. */
  protected readonly savingsAccount = () =>
    this.accountList().find(a => a.id !== this.account()?.id) ?? null;
  /** Time the balance last changed or was re-fetched; shown as "as of 12:31 PM". */
  protected readonly updatedAt = toSignal(
    this.accounts.account$.pipe(filter(a => a !== null), map(() => new Date())),
    { initialValue: null },
  );
  protected readonly error = signal<string | null>(null);

  /** Id of the account being edited; that card turns into the edit form. One at a time. */
  protected readonly editingId = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly editError = signal<string | null>(null);
  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<'success' | 'error'>('success');

  constructor() {
    this.refresh();
  }

  /** The account when it is the one being edited, otherwise null. */
  protected editing(account: Account | null): Account | null {
    return account && account.id === this.editingId() ? account : null;
  }

  protected startEdit(account: Account): void {
    this.editError.set(null);
    this.editingId.set(account.id);
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.editError.set(null);
  }

  /** The cached accounts update on success, so both cards show the change without a reload. */
  protected saveEdit(accountId: string, changes: UpdateAccountRequest): void {
    this.saving.set(true);
    this.editError.set(null);
    this.accounts
      .updateAccount(accountId, changes)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.editingId.set(null);
          this.toastMessage.set('Account updated.');
          this.toastType.set('success');
        },
        error: (err: ApiError) => {
          this.saving.set(false);
          this.editError.set(err.message);
        },
      });
  }

  /** Re-fetch so the balance is current whenever the dashboard opens. A cached balance stays visible meanwhile. */
  protected refresh(): void {
    this.error.set(null);
    this.accounts
      .loadMyAccount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ error: (err: ApiError) => this.error.set(err.message) });
    this.accounts
      .loadMyAccounts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ error: (err: ApiError) => this.error.set(err.message) });
  }
}
