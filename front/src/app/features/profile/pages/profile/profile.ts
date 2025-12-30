import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, startWith } from 'rxjs';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { passwordPolicyValidator } from '../../../../shared/validators/password-policy.validator';
import { isApiErrorResponse, toFieldErrorMap } from '../../../../core/api/api-error.model';
import { UserMeApiService } from '../../services/user-me-api.service';
import type {
  UpdateMeRequest,
  UpdatedResponse,
  UserMeResponse,
} from '../../interfaces/user-me.models';

/**
 * Erreurs typées (évite l'erreur Angular "index signature" dans le template).
 */
interface ProfileFieldErrors {
  email?: string[];
  username?: string[];
  password?: string[];
}

/**
 * Page Profil : consulter + modifier.
 *
 * Point clé :
 * - Reactive Forms n'est pas "signal-based" : pour que `computed()` réagisse aux changements du form,
 *   convertit `form.valueChanges` en signal via `toSignal`.
 */
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(UserMeApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly snackBar = inject(MatSnackBar);

  /** Snapshot initial (SIGNAL) pour comparer les changements. */
  readonly initialMe = signal<Pick<UserMeResponse, 'email' | 'username'> | null>(null);

  /** Profil complet (inclut abonnements). */
  readonly me = signal<UserMeResponse | null>(null);

  /** États UI */
  readonly loading = signal(true);
  readonly saving = signal(false);

  /** Erreurs */
  readonly globalError = signal<string | null>(null);
  readonly fieldErrors = signal<ProfileFieldErrors | null>(null);

  /** Formulaire */
  readonly form = this.fb.nonNullable.group({
    email: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.email,
      Validators.maxLength(254),
    ]),
    username: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(50)]),
    password: this.fb.nonNullable.control('', [
      Validators.maxLength(72),
      passwordPolicyValidator(),
    ]),
  });

  /**
   * Signal du form (clé pour rendre `isPristine` réactif).
   */
  private readonly formValue = toSignal(
    this.form.valueChanges.pipe(startWith(this.form.getRawValue())),
    { initialValue: this.form.getRawValue() }
  );

  /**
   * True si aucune modification n'est présente (désactive "Enregistrer").
   * - Computed dépend de 2 signaux : `initialMe()` et `formValue()`.
   * - Ainsi il se recalculera quand je tape dans le mot de passe.
   */
  readonly isPristine = computed(() => {
    const init = this.initialMe();
    if (!init) return true;

    const v = this.formValue();
    const email = v.email ?? '';
    const username = v.username ?? '';
    const pwdChanged = !!(v.password ?? '').trim();

    return email === init.email && username === init.username && !pwdChanged;
  });

  ngOnInit(): void {
    this.loadMe();
  }

  /**
   * Charge GET /api/users/me.
   */
  private loadMe(): void {
    this.loading.set(true);
    this.globalError.set(null);
    this.fieldErrors.set(null);

    const me$ = this.api.me().pipe(
      finalize(() => this.loading.set(false)),
      takeUntilDestroyed(this.destroyRef)
    );

    me$.subscribe({
      next: (me: UserMeResponse) => {
        this.me.set(me);
        this.initialMe.set({ email: me.email, username: me.username });
        this.form.patchValue({ email: me.email, username: me.username, password: '' });
      },
      error: (err: unknown) => this.handleError(err),
    });
  }

  /**
   * Soumet PUT /api/users/me.
   */
  submit(): void {
    this.globalError.set(null);
    this.fieldErrors.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.buildUpdatePayload();
    if (!payload) {
      this.snackBar.open('Aucune modification à enregistrer.', 'OK', { duration: 2500 });
      return;
    }

    this.saving.set(true);

    const update$ = this.api.updateMe(payload).pipe(
      finalize(() => this.saving.set(false)),
      takeUntilDestroyed(this.destroyRef)
    );

    update$.subscribe({
      next: (res: UpdatedResponse) => {
        if (res.updated) {
          const v = this.form.getRawValue();
          this.initialMe.set({ email: v.email, username: v.username });

          // Vide le password après update
          this.form.controls.password.reset('');

          this.loadMe();
          this.snackBar.open('Profil mis à jour ✅', 'OK', { duration: 2500 });
        } else {
          this.snackBar.open('Aucune modification détectée.', 'OK', { duration: 2500 });
        }
      },
      error: (err: unknown) => this.handleError(err),
    });
  }

  /**
   * Payload minimal : uniquement les champs modifiés.
   * @returns payload si changement, sinon `null`
   */
  private buildUpdatePayload(): UpdateMeRequest | null {
    const init = this.initialMe();
    if (!init) return null;

    const v = this.form.getRawValue();
    const payload: UpdateMeRequest = {};

    if (v.email !== init.email) payload.email = v.email;
    if (v.username !== init.username) payload.username = v.username;

    const pwd = v.password?.trim();
    if (pwd) payload.password = pwd;

    return Object.keys(payload).length ? payload : null;
  }

  /**
   * Normalisation erreurs API.
   */
  private handleError(err: unknown): void {
    if (err instanceof HttpErrorResponse && isApiErrorResponse(err.error)) {
      this.globalError.set(err.error.message);

      const map = toFieldErrorMap(err.error.fieldErrors);
      this.fieldErrors.set({
        email: map['email'],
        username: map['username'],
        password: map['password'],
      });
      return;
    }

    this.globalError.set('Une erreur est survenue. Réessaie plus tard.');
  }
}
