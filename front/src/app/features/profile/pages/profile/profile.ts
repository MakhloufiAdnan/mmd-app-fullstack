import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, startWith } from 'rxjs';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { passwordPolicyValidator } from '@shared/validators/password-policy.validator';
import { isApiErrorResponse, toFieldErrorMap } from '@core/api/api-error.model';

import { TopicsApiService } from '@features/topics/services/topics-api.service';
import type { TopicListItem } from '@features/topics/interfaces/topic.models';

import { UserMeApiService } from '../../services/user-me-api.service';
import type {
  UpdatedResponse,
  UserMeResponse,
  UpdateMeRequest,
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
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(UserMeApiService);
  private readonly topicsApi = inject(TopicsApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly snackBar = inject(MatSnackBar);

  /** Snapshot initial (SIGNAL) pour comparer les changements. */
  readonly initialMe = signal<Pick<UserMeResponse, 'email' | 'username'> | null>(null);

  /** Profil complet (inclut abonnements). */
  readonly me = signal<UserMeResponse | null>(null);

  /** Topics (source UI pour Abonnements). */
  readonly topics = signal<TopicListItem[] | null>(null);

  /** États UI */
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly topicsLoading = signal(true);

  /** UI : topics en cours de désabonnement (évite double-clic). */
  readonly unsubPendingIds = signal<Set<number>>(new Set());

  /** Erreurs */
  readonly globalError = signal<string | null>(null);
  readonly fieldErrors = signal<ProfileFieldErrors | null>(null);

  /** Erreurs spécifiques abonnements */
  readonly topicsError = signal<string | null>(null);

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
   * True si aucune modification n'est présente (désactive "Sauvegarder").
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

  /** Liste affichée : uniquement les thèmes abonnés (topics.subscribed=true). */
  readonly subscribedTopics = computed(() => (this.topics() ?? []).filter((t) => t.subscribed));

  ngOnInit(): void {
    this.loadMe();
    this.loadTopics();
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

  /** Charge GET /api/topics (source pour Abonnements). */
  private loadTopics(): void {
    this.topicsLoading.set(true);
    this.topicsError.set(null);

    const topics$ = this.topicsApi.listTopics().pipe(
      finalize(() => this.topicsLoading.set(false)),
      takeUntilDestroyed(this.destroyRef)
    );

    topics$.subscribe({
      next: (topics) => this.topics.set(topics),
      error: () => {
        this.topicsError.set('Impossible de charger vos abonnements.');
        this.snackBar.open('Impossible de charger vos abonnements.', 'OK', { duration: 3000 });
      },
    });
  }

  submit(): void {
    if (this.form.invalid) return;

    this.saving.set(true);
    this.globalError.set(null);
    this.fieldErrors.set(null);

    const raw = this.form.getRawValue();
    const payload: UpdateMeRequest = {
      email: raw.email,
      username: raw.username,
      password: raw.password?.trim() ? raw.password : null,
    };

    const update$ = this.api.updateMe(payload).pipe(
      finalize(() => this.saving.set(false)),
      takeUntilDestroyed(this.destroyRef)
    );

    update$.subscribe({
      next: (_res: UpdatedResponse) => {
        this.snackBar.open('Profil mis à jour.', 'OK', { duration: 2000 });
        this.loadMe();
      },
      error: (err: unknown) => this.handleError(err),
    });
  }

  /**
   * Se désabonner d'un thème (depuis le profil).
   * Endpoint : DELETE /api/users/me/subscriptions/{topicId}
   */
  unsubscribe(topicId: number): void {
    // Garde-fou : empêche le double-clic (UX) et évite les appels inutiles.
    if (this.unsubPendingIds().has(topicId)) return;

    this.globalError.set(null);

    const before = this.topics();
    const wasSubscribed = (before ?? []).some((t) => t.id === topicId && t.subscribed);

    if (wasSubscribed) {
      this.topics.update((list) => {
        if (!list) return list;
        return list.map((t) => (t.id === topicId ? { ...t, subscribed: false } : t));
      });
    }

    this.setUnsubPending(topicId, true);

    const unsubscribe$ = this.api.unsubscribeFromTopic(topicId).pipe(
      finalize(() => this.setUnsubPending(topicId, false)),
      takeUntilDestroyed(this.destroyRef)
    );

    unsubscribe$.subscribe({
      next: () => {
        this.snackBar.open('Abonnement supprimé.', 'OK', { duration: 2000 });
        // Re-sync avec le back
        this.loadTopics();
      },
      error: (err: unknown) => {
        // Rollback ciblé (évite d'annuler d'autres actions en parallèle)
        if (wasSubscribed) {
          this.topics.update((list) => {
            if (!list) return list;
            return list.map((t) => (t.id === topicId ? { ...t, subscribed: true } : t));
          });
        }

        if (err instanceof HttpErrorResponse && isApiErrorResponse(err.error)) {
          this.snackBar.open(err.error.message, 'OK', { duration: 3000 });
          return;
        }
        this.snackBar.open('Une erreur est survenue. Réessaie plus tard.', 'OK', {
          duration: 3000,
        });
      },
    });
  }

  private setUnsubPending(topicId: number, isPending: boolean): void {
    const next = new Set(this.unsubPendingIds());
    if (isPending) next.add(topicId);
    else next.delete(topicId);
    this.unsubPendingIds.set(next);
  }

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
