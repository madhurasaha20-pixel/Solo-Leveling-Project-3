import { ChangeDetectionStrategy, Component, effect, input, output, signal, untracked } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { Account, UpdateAccountRequest } from '../../core/models';
import { AlertComponent, ButtonComponent, CardComponent, TabItem, TabsComponent, TextFieldComponent } from '../../shared/ui';

/**
 * CardView [Edit account]. Presentational only: the dashboard page passes the account in
 * and calls AccountService.updateAccount when (save) fires.
 *
 *   <app-edit-account-card [account]="acct" [saving]="saving()" [error]="editError()"
 *                          (save)="saveEdit(acct.id, $event)" (cancel)="cancelEdit()" />
 */
@Component({
  selector: 'app-edit-account-card',
  imports: [ReactiveFormsModule, AlertComponent, ButtonComponent, CardComponent, TabsComponent, TextFieldComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <bc-card title="Edit account">
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <bc-text-field
          label="Nickname"
          placeholder="e.g. Rainy day fund"
          autocomplete="off"
          formControlName="nickname"
          hint="Optional. Shown on the card instead of the account type."
          [error]="nicknameError()"
        />

        <p aria-hidden="true" class="mt-5 mb-2 text-label font-medium text-ink">Account type</p>
        <bc-tabs label="Account type" [tabs]="typeTabs" [(value)]="type" />

        @if (error()) {
          <bc-alert class="mt-5" tone="error" title="We couldn't save your changes">
            <p>{{ error() }}</p>
          </bc-alert>
        }

        <div class="mt-6 flex justify-end gap-3">
          <bc-button variant="secondary" size="sm" [disabled]="saving()" (click)="cancel.emit()">Cancel</bc-button>
          <bc-button type="submit" size="sm" [loading]="saving()">Save changes</bc-button>
        </div>

        <!-- Receipt footer, same as the balance card -->
        <div class="mt-4 border-t border-dashed border-border-strong pt-3 font-mono text-code tabular-nums text-ink-muted">
          {{ account().id }}
        </div>
      </form>
    </bc-card>
  `,
})
export class EditAccountCardComponent {
  readonly account = input.required<Account>();
  readonly saving = input(false);
  /** User-facing message from ApiError.message. */
  readonly error = input<string | null>(null);
  /** Only the fields that changed. Not emitted when nothing changed. */
  readonly save = output<UpdateAccountRequest>();
  readonly cancel = output<void>();

  protected readonly typeTabs: TabItem[] = [
    { id: 'checking', label: 'Checking' },
    { id: 'savings', label: 'Savings' },
  ];
  protected readonly form = new FormGroup({
    nickname: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(30)] }),
  });
  private readonly nickname = this.form.controls.nickname;
  protected readonly type = signal<string>('checking');
  private readonly submitted = signal(false);

  constructor() {
    // start from the account's current values. untracked: setValue reads signals inside the
    // form, and the effect must only re-run when a different account comes in, not on every keystroke
    effect(() => {
      const acct = this.account();
      untracked(() => {
        this.nickname.setValue(acct.nickname ?? '');
        this.type.set(acct.type);
      });
    });
  }

  /** Same message the server sends, so the user sees one wording either way. */
  protected nicknameError(): string | null {
    const c = this.nickname;
    if (!c.errors || !(c.touched || this.submitted())) return null;
    return c.errors['maxlength'] ? 'Nickname must be 30 characters or fewer.' : 'Check this field.';
  }

  protected submit(): void {
    this.submitted.set(true);
    this.nickname.markAsTouched();
    if (this.nickname.invalid) return;

    const acct = this.account();
    const changes: UpdateAccountRequest = {};
    const nickname = this.nickname.value.trim();
    if (nickname !== (acct.nickname ?? '')) changes.nickname = nickname;
    if (this.type() !== acct.type) changes.type = this.type() as Account['type'];

    if (Object.keys(changes).length === 0) {
      this.cancel.emit(); // nothing to save
      return;
    }
    this.save.emit(changes);
  }
}
