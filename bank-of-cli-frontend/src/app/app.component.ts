import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, distinctUntilChanged, filter, switchMap } from 'rxjs';
import { AccountService } from './core/services/account.service';

@Component({
  selector: 'app-root',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a href="#main-content" class="sr-only fixed left-4 top-4 z-50 min-h-10 items-center rounded-md bg-brand px-4 py-3 text-label font-medium text-on-brand transition-colors duration-[120ms] ease-out focus:not-sr-only">Skip to content</a>
    @let user = auth.currentUser$ | async;
    <bc-app-header [user]="user" [account]="accounts.account$ | async" [links]="user ? links : []" (signOut)="signOut()" (accountDeleted)="accountDeleted()"/>
    <main id="main-content" tabindex="-1" class="mx-auto max-w-content px-4 pb-12 pt-6 sm:px-6 sm:pt-8">
      <router-outlet />
           @if (signOutMessage() && router.url === '/login') {
           <p role="status" class="mt-4 text-body text-ink">{{ signOutMessage() }}</p>
         }
    </main>
  `,
})
export class AppComponent {
  protected readonly auth = inject(AuthService);
  protected readonly accounts = inject(AccountService);
  protected readonly router = inject(Router);
  protected readonly signOutMessage = signal<string | null>(null);
  protected readonly links = [
    { path: '/dashboard', label: 'Dashboard' },
    { path: '/move-money', label: 'Move money' },
    { path: '/transactions', label: 'History' },
  ];
  constructor() {
    this.auth.currentUser$.pipe(
      distinctUntilChanged((a, b) => a?.id === b?.id),
      filter(user => user !== null),
      switchMap(() => this.accounts.loadMyAccount().pipe(catchError(() => EMPTY))),
      takeUntilDestroyed(),
    ).subscribe();
  }
 

  protected signOut(): void {
    this.auth.logout();
    this.signOutMessage.set('You’re signed out.');
    void this.router.navigateByUrl('/login');
  }

  protected accountDeleted(): void {
    this.signOutMessage.set('Your account has been deleted.');
    void this.router.navigateByUrl('/login');
  }
}

