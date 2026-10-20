import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, output, signal, viewChild } from '@angular/core';

import { Account, User } from '../../core/models';
import { BalanceVisibilityService } from '../../core/services/balance-visibility.service';
import { ThemeService } from '../../core/services/theme.service';
import { MoneyPipe } from '../pipes/money.pipe';
import { ButtonComponent } from './button.component';
import { IconComponent } from './icon.component';
import { DeleteAccountDialogComponent } from './delete-account-dialog.component';



/**
 * Account pop-up in the header: identity, account number (copy), balance (hide/show), appearance (mock), sign out.
 * The app shell passes the user and account in and handles sign-out.
 * Hide/show reads BalanceVisibilityService, so it stays in sync with the dashboard's balance card.
 */
@Component({
  selector: 'bc-account-menu',
  imports: [DatePipe, MoneyPipe, ButtonComponent, IconComponent, DeleteAccountDialogComponent],
  templateUrl: './account-menu.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'relative block',
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'onEscape()',
  },
})
export class AccountMenuComponent {
  readonly user = input.required<User>();
  readonly account = input<Account | null>(null);
  readonly signOut = output<void>();
  readonly accountDeleted = output<void>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly trigger = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');
  private readonly deleteDialog = viewChild.required(DeleteAccountDialogComponent);
  private copiedTimer?: ReturnType<typeof setTimeout>;

  protected readonly open = signal(false);
  protected readonly copied = signal(false);
  protected readonly visibility = inject(BalanceVisibilityService);

  protected readonly theme = inject(ThemeService);
 
  protected readonly appearanceOptions = [
    { value: 'light', label: 'Light', icon: 'sun' },
    { value: 'dark', label: 'Dark', icon: 'moon' },
  ] as const;

  protected readonly initials = computed(() =>
    this.user().name.trim().split(/\s+/).map(part => part[0]).join('').slice(0, 2).toUpperCase(),
  );

  protected onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) this.open.set(false);
  }

  protected onEscape(): void {
    if (!this.open()) return;
    this.open.set(false);
    this.trigger().nativeElement.focus();
  }
  protected openDeleteDialog(): void {
    this.open.set(false);
    this.deleteDialog().open();
  }

  /** Cancelled: give focus back to the menu button, since the menu item that opened the dialog is gone. */
  protected onDeleteDialogClosed(): void {
    this.trigger().nativeElement.focus();
  }
  protected async copyAccountNumber(id: string): Promise<void> {
    await navigator.clipboard.writeText(id);
    this.copied.set(true);
    clearTimeout(this.copiedTimer);
    this.copiedTimer = setTimeout(() => this.copied.set(false), 2000);
  }
}