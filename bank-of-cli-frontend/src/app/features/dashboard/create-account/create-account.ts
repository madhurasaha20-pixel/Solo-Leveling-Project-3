import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError, Account, CreateAccountRequest } from '../../../core/models';
import { AccountService } from '../../../core/services/account.service';
import { ButtonComponent, CardComponent } from '../../../shared/ui';

@Component({
  selector: 'app-create-account',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, CardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './create-account.html',
})
export class CreateAccountComponent {
  private readonly fb = inject(FormBuilder);
  private readonly accounts = inject(AccountService);

  readonly created = output<Account>();

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    type: ['checking' as 'checking' | 'savings', Validators.required],
    nickname: ['', Validators.maxLength(30)],
  });

  protected submit(): void {
    this.form.markAllAsTouched();

    if (this.form.invalid || this.saving()) {
      return;
    }

    const values = this.form.getRawValue();
    const request: CreateAccountRequest = {
      type: values.type,
      nickname: values.nickname.trim() || undefined,
    };

    this.saving.set(true);
    this.error.set(null);

    this.accounts.createAccount(request).subscribe({
      next: account => {
        this.saving.set(false);
        this.form.reset({ type: 'checking', nickname: '' });
        this.created.emit(account);
      },
      error: (err: ApiError) => {
        this.saving.set(false);
        this.error.set(err.message);
      },
    });
  }
}