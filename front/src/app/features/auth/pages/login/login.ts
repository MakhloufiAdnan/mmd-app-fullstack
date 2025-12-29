import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, finalize, of, startWith, switchMap } from 'rxjs';

import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { AuthLayout } from '../../components/auth-layout/auth-layout';
import { AuthApiService } from '../../services/auth-api.service';
import { isApiErrorResponse, toFieldErrorMap } from '../../../../core/api/api-error.model';

/**
 * Page "Se connecter" 
 */
@Component({
  selector: 'mdd-login',
  standalone: true,
  imports: [ReactiveFormsModule, AuthLayout, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** Empêche le double submit. */
  readonly submitting = signal(false);

  /** Message d'erreur global renvoyé par l'API. */
  readonly globalError = signal<string | null>(null);

  /** Erreurs API par champ. */
  readonly fieldErrors = signal<Record<string, string[]> | null>(null);

  /** Formulaire (identifier = email OU username). */
  readonly form = this.fb.nonNullable.group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  /**
   * Permet de rendre le bouton réactif (computed ne réagit pas à `form.valid`).
   */
  private readonly formStatus$ = this.form.statusChanges.pipe(startWith(this.form.status));

  /** Signal basé sur `formStatus$` pour recalculer `canSubmit` automatiquement. */
  readonly formStatus = toSignal(this.formStatus$, { initialValue: this.form.status });

  /** Bouton actif si formulaire VALID et pas en cours de soumission. */
  readonly canSubmit = computed(() => this.formStatus() === 'VALID' && !this.submitting());

  /**
   * Soumission :
   * - `csrf()` best-effort (selon config back)
   * - `takeUntilDestroyed` ferme la subscription si le composant est détruit
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

    const login$ = this.api.csrf().pipe(
      catchError(() => of(void 0)),
      switchMap(() => this.api.login(payload)),
      finalize(() => this.submitting.set(false)),
      takeUntilDestroyed(this.destroyRef)
    );

    login$.subscribe({
      next: () => void this.router.navigateByUrl('/feed'),
      error: (err) => this.handleError(err),
    });
  }

  /**
   * Affichage d'erreurs :
   * - API standard => global + fieldErrors
   * - sinon => message générique (pas de fuite technique)
   */
  private handleError(err: unknown): void {
    if (err instanceof HttpErrorResponse && isApiErrorResponse(err.error)) {
      this.globalError.set(err.error.message);
      this.fieldErrors.set(toFieldErrorMap(err.error.fieldErrors));
      return;
    }
    this.globalError.set('Une erreur est survenue. Réessaie plus tard.');
  }
}
