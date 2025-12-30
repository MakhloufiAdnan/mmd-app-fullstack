import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, startWith } from 'rxjs';

import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { AuthLayout } from '../../components/auth-layout/auth-layout';
import { passwordPolicyValidator } from '@shared/validators/password-policy.validator';
import { AuthFacade } from '../../state/auth.facade';
import { isApiErrorResponse, toFieldErrorMap } from '@core/api/api-error.model';

/**
 * - Appel via facade (register)
 * - Redirection vers /login ensuite 
 */
@Component({
  selector: 'mdd-register',
  standalone: true,
  imports: [ReactiveFormsModule, AuthLayout, MatFormFieldModule, MatInputModule, MatButtonModule],
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

  private readonly formStatus$ = this.form.statusChanges.pipe(startWith(this.form.status));
  readonly formStatus = toSignal(this.formStatus$, { initialValue: this.form.status });
  readonly canSubmit = computed(() => this.formStatus() === 'VALID' && !this.submitting());

  /**
   * Inscription :
   * - csrf() best-effort
   * - puis register()
   * - redirection /login (décision MVP standard)
   */
  submit(): void {
    this.globalError.set(null);
    this.fieldErrors.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const payload = this.form.getRawValue();

    const register$ = this.auth.register(payload).pipe(
      finalize(() => this.submitting.set(false)),
      takeUntilDestroyed(this.destroyRef)
    );

    register$.subscribe({
      next: () => void this.router.navigateByUrl('/login'),
      error: (err) => this.handleError(err),
    });
  }

  private handleError(err: unknown): void {
    if (err instanceof HttpErrorResponse && isApiErrorResponse(err.error)) {
      this.globalError.set(err.error.message);
      this.fieldErrors.set(toFieldErrorMap(err.error.fieldErrors));
      return;
    }
    this.globalError.set('Une erreur est survenue. Réessaie plus tard.');
  }
}
