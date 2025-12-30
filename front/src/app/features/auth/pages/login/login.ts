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
import { AuthFacade } from '../../state/auth.facade';
import { isApiErrorResponse, toFieldErrorMap } from '@core/api/api-error.model';

/**
 * Rôle :
 * - S'appuyer sur `AuthFacade.login()` pour stocker l'access token en mémoire.
 * - Rediriger vers /feed après login.
 *
 * Note :
 * - `canSubmit()` doit réagir au statut du formulaire.
 *   `form.valid` n'est pas un signal => on utilise `statusChanges` + `toSignal`.
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
  private readonly auth = inject(AuthFacade);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** Évite le double submit et gère l'état disabled. */
  readonly submitting = signal(false);

  /** Message d'erreur global (ex: invalid credentials). */
  readonly globalError = signal<string | null>(null);

  /** Erreurs API par champ. */
  readonly fieldErrors = signal<Record<string, string[]> | null>(null);

  /** Formulaire : identifier = email OU username (contrat). */
  readonly form = this.fb.nonNullable.group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  /** Rend le statut du form réactif pour `computed()`. */
  private readonly formStatus$ = this.form.statusChanges.pipe(startWith(this.form.status));
  readonly formStatus = toSignal(this.formStatus$, { initialValue: this.form.status });

  /** Bouton actif si form VALID et pas en cours de soumission. */
  readonly canSubmit = computed(() => this.formStatus() === 'VALID' && !this.submitting());

  /**
   * Soumission :
   * - Appelle `AuthFacade.login()` (csrf best-effort inclus côté facade/service selon ton implémentation).
   * - Stocke le token en mémoire via le store, puis navigation /feed.
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

    const login$ = this.auth.login(payload).pipe(
      finalize(() => this.submitting.set(false)),
      takeUntilDestroyed(this.destroyRef)
    );

    login$.subscribe({
      next: () => void this.router.navigateByUrl('/feed'),
      error: (err: unknown) => this.handleError(err),
    });
  }

  /**
   * Normalise l'erreur :
   * - payload API standard => message global + erreurs par champ
   * - sinon => message générique (évite fuite technique)
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
