import { Account, User } from '../../core/models';
import { AccountMenuComponent } from './account-menu.component';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';




export interface NavLink { path: string; label: string; }

/** Presentation only: the app shell supplies the user and handles sign-out. */
@Component({
  selector: 'bc-app-header',
  imports: [RouterLink, RouterLinkActive, AccountMenuComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sticky top-0 z-30 block border-b border-border bg-surface-raised' },
  template: `
    <header class="mx-auto max-w-content px-4 sm:px-6">
      <div class="flex h-14 items-center gap-3 sm:gap-6">
        <a routerLink="/dashboard" aria-label="Bank of CLI home"
           class="inline-flex min-h-10 shrink-0 items-center gap-2 rounded-md text-heading-3 font-semibold text-ink transition-colors duration-[120ms] ease-out hover:text-brand">
          <span aria-hidden="true" class="font-mono font-medium text-brand">&gt;_</span>Bank of CLI
        </a>
        <nav aria-label="Main" class="hidden flex-1 gap-1 md:flex">
          @for (link of links(); track link.path) {
            <a [routerLink]="link.path" routerLinkActive="bg-brand-tint !text-ink" ariaCurrentWhenActive="page"
               class="inline-flex min-h-10 items-center rounded-md px-3 py-2 text-label font-medium text-ink-muted transition-colors duration-[120ms] ease-out hover:bg-surface-sunken hover:text-ink">{{ link.label }}</a>
          }
        </nav>
        <div class="ml-auto flex min-w-0 items-center gap-2 text-label font-medium text-ink-muted">
        @if (user(); as u) {
               <bc-account-menu [user]="u" [account]="account()" (signOut)="signOut.emit()" (accountDeleted)="accountDeleted.emit()" />
          } @else {
            <a routerLink="/login" class="inline-flex h-10 shrink-0 items-center rounded-md px-4 text-body font-medium text-brand transition-colors duration-[120ms] ease-out hover:bg-brand-tint">Sign in</a>
          }
        </div>
      </div>
      @if (links().length) {
        <nav aria-label="Main" class="flex flex-wrap gap-1 pb-3 md:hidden">
          @for (link of links(); track link.path) {
            <a [routerLink]="link.path" routerLinkActive="bg-brand-tint !text-ink" ariaCurrentWhenActive="page"
               class="inline-flex min-h-10 items-center rounded-md px-3 py-2 text-label font-medium text-ink-muted transition-colors duration-[120ms] ease-out hover:bg-surface-sunken hover:text-ink">{{ link.label }}</a>
          }
        </nav>
      }
    </header>
  `,
})
export class AppHeaderComponent {
  readonly user = input<User | null>(null);
  readonly account = input<Account | null>(null);
  readonly links = input<NavLink[]>([]);
  readonly signOut = output<void>();
  readonly accountDeleted = output<void>();
  
}
