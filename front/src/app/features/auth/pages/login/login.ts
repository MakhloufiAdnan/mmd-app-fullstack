import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthLayout } from '../../components/auth-layout/auth-layout';
import { AuthFacade } from '../../state/auth.facade';
import {
  createFormSubmitSignals,
  prepareSubmit,
  setApiErrors,
} from '@shared/forms/auth-submit.helpers';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';

type LoginPayload = {
  identifier: string;
  password: string;
};

@Component({
  selector: 'mdd-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    AuthLayout,
    MatFormFieldModule, 
    MatInputModule,      
    MatButtonModule,     
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthFacade);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly submitting = signal(false);
  readonly globalError = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string[]> | null>(null);

  readonly form = this.fb.nonNullable.group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  private readonly submitSignals = createFormSubmitSignals(this.form, this.submitting);
  readonly formStatus = this.submitSignals.formStatus;
  readonly canSubmit = this.submitSignals.canSubmit;

  submit(): void {
    const payload = prepareSubmit<LoginPayload>(
      this.form,
      this.submitting,
      this.globalError,
      this.fieldErrors
    );
    if (!payload) return;

    this.auth
      .login(payload)
      .pipe(
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.router.navigateByUrl('/feed').catch(() => undefined);
        },
        error: (err: unknown) => setApiErrors(err, this.globalError, this.fieldErrors),
      });
  }
}
