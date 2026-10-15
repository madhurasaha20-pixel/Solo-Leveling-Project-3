import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Account } from '../../core/models';
import { BalanceVisibilityService } from '../../core/services/balance-visibility.service';
import { formatMoney } from '../../shared/pipes/money.pipe';
import { AlertComponent, ButtonComponent, CardComponent, SkeletonComponent } from '../../shared/ui';

/**
 * CardView [Balance]. Presentational only: the dashboard page passes the data in.
 *
 *   <app-balance-card [account]="account()" [updatedAt]="updatedAt()" [error]="error()" (retry)="refresh()"
 *                     (edit)="startEdit($event)" />
 *
 * States
 *   account set         -> balance (dollars large, cents raised), hide/show toggle, edit button, receipt footer
 *                          (the title is the account's nickname when it has one, otherwise its type)
 *   no account + error  -> error alert with a retry button
 *   neither             -> skeleton shaped like the real card
 */
@Component({
  selector: 'app-balance-card',
  imports: [DatePipe, RouterLink, AlertComponent, ButtonComponent, CardComponent, SkeletonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <bc-card [title]="heading()">
      @if (account()) {
        <bc-button cardAction variant="ghost" size="sm" (click)="visibility.toggle()">        
          {{ hidden() ? 'Show' : 'Hide' }}<span class="sr-only"> balance</span>
        </bc-button>
      }

      @if (account(); as acct) {
        <p class="text-label font-medium text-ink-muted">Available balance</p>

        @if (hidden()) {
          <p class="mt-1 font-mono text-amount-display font-medium tabular-nums text-ink-muted"
             data-testid="balance-amount">
            <span aria-hidden="true">$•••••</span><span class="sr-only">Balance hidden</span>
          </p>
        } @else {
          <p class="mt-1 flex items-start font-mono font-medium tabular-nums text-ink"
             data-testid="balance-amount" aria-live="polite" [attr.aria-label]="'Available balance ' + full()">
            <span class="text-amount-display" aria-hidden="true">{{ parts().whole }}</span>
            <span class="mt-1 text-heading-2 text-ink-muted" aria-hidden="true">{{ parts().cents }}</span>
          </p>
        }

        <div class="mt-4 flex justify-end gap-3">
          <bc-button variant="ghost" size="sm" (click)="edit.emit(acct)">
            Edit<span class="sr-only"> account</span>
          </bc-button>
          <bc-button variant="secondary" size="sm" routerLink="/move-money">Manage Money</bc-button>
        </div>

        <!-- Receipt footer -->
        <div class="mt-4 flex items-center justify-between gap-4 border-t border-dashed border-border-strong pt-3
                    font-mono text-code tabular-nums text-ink-muted">
          <span>{{ acct.id }}</span>
          @if (updatedAt(); as at) {
            <span>as of {{ at | date: 'h:mm a' }}</span>
          }
        </div>
      } @else if (error()) {
        <bc-alert tone="error" title="We couldn't load your balance">
          <p>{{ error() }}</p>
          <bc-button class="mt-3" variant="secondary" size="sm" (click)="retry.emit()">Try again</bc-button>
        </bc-alert>
      } @else {
        <div role="status">
          <span class="sr-only">Loading balance</span>
          <bc-skeleton width="120px" [height]="12" />
          <bc-skeleton class="mt-2" width="220px" [height]="36" />
          <div class="mt-4 flex justify-between border-t border-dashed border-border-strong pt-3">
            <bc-skeleton width="80px" [height]="12" />
            <bc-skeleton width="80px" [height]="12" />
          </div>
        </div>
      }
    </bc-card>
  `,
})
export class BalanceCardComponent {
  readonly account = input<Account | null>(null);
  readonly title = input('Checking');
  /** When the balance was last confirmed by the server. */
  readonly updatedAt = input<Date | null>(null);
  /** User-facing message from ApiError.message; only shown when there is no account to display. */
  readonly error = input<string | null>(null);
  readonly retry = output<void>();
  /** User clicked Edit; the dashboard swaps this card for the edit card. */
  readonly edit = output<Account>();

/** Privacy toggle, like real banking apps. Shared with the account menu's Hide button. */  
  protected readonly visibility = inject(BalanceVisibilityService);
  protected readonly hidden = this.visibility.hidden;
  /** "Rainy day fund", or "Checking"/"Savings" from the type; falls back to [title] while loading. */
  protected readonly heading = computed(() => {
    const acct = this.account();
    if (!acct) return this.title();
    return acct.nickname || acct.type.charAt(0).toUpperCase() + acct.type.slice(1);
  });
  protected readonly full = computed(() => formatMoney(this.account()?.balance ?? 0));

  /** "$1,325.75" -> { whole: "$1,325", cents: ".75" } */
  protected readonly parts = computed(() => {
    const text = this.full();
    const dot = text.lastIndexOf('.');
    return dot === -1 ? { whole: text, cents: '' } : { whole: text.slice(0, dot), cents: text.slice(dot) };
  });
}
