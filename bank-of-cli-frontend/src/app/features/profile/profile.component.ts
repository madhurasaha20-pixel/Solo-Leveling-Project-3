import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';

import { ApiError, User } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { AlertComponent, ButtonComponent, CardComponent, SkeletonComponent } from '../../shared/ui';
import { PageHeadingComponent } from '../../shared/ui/page-heading.component';

@Component({
  selector: 'app-profile',
  imports: [DatePipe, AlertComponent, ButtonComponent, CardComponent, PageHeadingComponent, SkeletonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <bc-page-heading title="Profile" description="Your personal details and membership information." />
    <div class="mt-8 max-w-form" [attr.aria-busy]="loading()">
      @if (loading()) {
        <bc-card title="Personal details">
          <span class="sr-only" role="status">Loading profile</span>
          <bc-skeleton class="mb-4" />
          <bc-skeleton class="mb-4" />
          <bc-skeleton />
        </bc-card>
      } @else if (error()) {
        <bc-alert tone="error" title="We couldn't load your profile">
          <p>{{ error() }}</p>
          @if (sessionExpired()) {
            <bc-button class="mt-4" variant="secondary" (click)="signIn()">Sign in again</bc-button>
          } @else {
            <bc-button class="mt-4" variant="secondary" (click)="loadProfile()">Try again</bc-button>
          }
        </bc-alert>
      } @else if (profile(); as user) {
        <bc-card title="Personal details">
          <dl class="space-y-6">
            <div>
              <dt class="text-label font-medium text-ink-muted">Full name</dt>
              <dd class="mt-1 break-words text-body text-ink">{{ user.name }}</dd>
            </div>
            <div>
              <dt class="text-label font-medium text-ink-muted">Email address</dt>
              <dd class="mt-1 break-all text-body text-ink">{{ user.email }}</dd>
            </div>
            <div>
              <dt class="text-label font-medium text-ink-muted">Member since</dt>
              <dd class="mt-1 text-body text-ink">{{ user.createdAt | date: 'longDate' : 'UTC' }}</dd>
            </div>
          </dl>
        </bc-card>
      }
    </div>
  `,
})
export class ProfileComponent {
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  protected readonly profile = signal<User | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly sessionExpired = signal(false);

  constructor() {
    this.loadProfile();
  }

  protected loadProfile(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    this.sessionExpired.set(false);
    this.auth.loadProfile().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: user => {
        this.profile.set(user);
        this.loading.set(false);
      },
      error: (error: ApiError) => {
        this.sessionExpired.set(error.status === 401);
        this.error.set(error.status === 401 ? 'Your session has expired. Please sign in again to view your profile.' : error.message);
        this.loading.set(false);
      },
    });
  }

  protected signIn(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}
