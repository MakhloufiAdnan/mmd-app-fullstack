import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthLayout } from '../../components/auth-layout/auth-layout';
import { passwordPolicyValidator } from '@shared/validators/password-policy.validator';
import { AuthFacade } from '../../state/auth.facade';
import {
  createFormSubmitSignals,
  prepareSubmit,
  setApiErrors,
} from '@shared/forms/auth-submit.helpers';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';

type RegisterPayload = {
  username: string;
  email: string;
  password: string;
};

@Component({
  selector: 'mdd-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    AuthLayout,
    MatFormFieldModule,  
    MatInputModule,      
    MatButtonModule,
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthFacade);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly submitting = signal(false);
  readonly globalError = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string[]> | null>(null);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, passwordPolicyValidator()]],
  });

  private readonly submitSignals = createFormSubmitSignals(this.form, this.submitting);
  readonly formStatus = this.submitSignals.formStatus;
  readonly canSubmit = this.submitSignals.canSubmit;

  submit(): void {
    const payload = prepareSubmit<RegisterPayload>(
      this.form,
      this.submitting,
      this.globalError,
      this.fieldErrors
    );
    if (!payload) return;

    this.auth
      .register(payload)
      .pipe(
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.router.navigateByUrl('/login').catch(() => undefined);
        },
        error: (err: unknown) => setApiErrors(err, this.globalError, this.fieldErrors),
      });
  }
}
