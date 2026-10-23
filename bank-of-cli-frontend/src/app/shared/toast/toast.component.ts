import {
  ChangeDetectionStrategy,
  Component,
  effect,
  input,
  OnDestroy,
  signal,
} from '@angular/core';

@Component({
  selector: 'app-toast',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
        @if (visibleMessage()) {
            <div
                class="fixed bottom-20 right-6 z-50 rounded-md border px-4 py-3 text-body-sm shadow-overlay"
                [class.animate-toast-in]="!fadingOut()"
                [class.animate-toast-out]="fadingOut()"
                [class.border-brand/30]="type() === 'success'"
                [class.bg-brand-tint]="type() === 'success'"
                [class.text-brand]="type() === 'success'"
                [class.border-debit/30]="type() === 'error'"
                [class.bg-debit-tint]="type() === 'error'"
                [class.text-debit]="type() === 'error'"
                role="alert"
            >
                {{ visibleMessage() }}
            </div>
        }
    `,
})
export class ToastComponent implements OnDestroy {
  readonly message = input('');
  readonly type = input<'success' | 'error'>('success');
  readonly visibleMessage = signal('');
  readonly fadingOut = signal(false);

  private timeoutId: ReturnType<typeof setTimeout> | undefined;
  private fadeTimeoutId: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect(() => {
      const message = this.message();

      this.clearTimers();
      this.fadingOut.set(false);
      this.visibleMessage.set(message);

      if (message) {
        // Display the toast for 3.5 seconds.
        this.timeoutId = setTimeout(() => {
          this.fadingOut.set(true);

          // Remove it after the fade-out animation finishes.
          this.fadeTimeoutId = setTimeout(() => {
            this.visibleMessage.set('');
            this.fadingOut.set(false);
          }, 500);
        }, 3500);
      }
    });
  }

  ngOnDestroy(): void {
    this.clearTimers();
  }

  private clearTimers(): void {
    if (this.timeoutId !== undefined) {
      clearTimeout(this.timeoutId);
      this.timeoutId = undefined;
    }

    if (this.fadeTimeoutId !== undefined) {
      clearTimeout(this.fadeTimeoutId);
      this.fadeTimeoutId = undefined;
    }
  }
}