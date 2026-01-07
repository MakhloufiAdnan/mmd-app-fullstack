import { AsyncPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, finalize, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TopicsApiService } from '../../../topics/services/topics-api.service';
import { TopicListItem } from '../../../topics/interfaces/topic.models';

import { PostsApiService } from '../../services/posts-api.service';
import { CreatePostRequest } from '../../interfaces/post.models';

import { isApiErrorResponse, toFieldErrorMap } from '@core/api/api-error.model';

@Component({
  selector: 'app-post-create',
  standalone: true,
  imports: [
    AsyncPipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './post-create.component.html',
  styleUrls: ['./post-create.component.scss'],
})
export class PostCreateComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly topicsApi = inject(TopicsApiService);
  private readonly postsApi = inject(PostsApiService);

  readonly topics$ = new BehaviorSubject<TopicListItem[]>([]);
  readonly loading$ = new BehaviorSubject<boolean>(true);
  readonly error$ = new BehaviorSubject<string | null>(null);

  /**
   * UI errors 
   * - fieldErrors: map champ -> liste de messages
   */
  readonly submitting = signal(false);
  readonly globalError = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string[]>>({});

  /**
   * Form :
   * - topicId nullable au départ (aucun choix)
   * - title/content NON NULLABLES => évite "string | null" au payload
   */
  readonly form = this.fb.nonNullable.group({
    topicId: this.fb.control<number | null>(null, { validators: [Validators.required] }),
    title: this.fb.nonNullable.control('', { validators: [Validators.required] }),
    content: this.fb.nonNullable.control('', { validators: [Validators.required] }),
  });

  constructor() {
    this.loadTopics();
  }

  /**
   * Calcule si l'utilisateur a au moins un thème abonné.
   */
  hasAnySubscribedTopics(topics: TopicListItem[]): boolean {
    return topics.some((t) => t.subscribed);
  }

  /**
   * Affiche le 1er message serveur pour un champ.
   */
  getFieldErrorFirst(field: 'topicId' | 'title' | 'content'): string | null {
    const msgs = this.fieldErrors()[field];
    return msgs?.[0] ?? null;
  }

  loadTopics(): void {
    this.loading$.next(true);
    this.error$.next(null);

    this.topicsApi
      .listTopics()
      .pipe(
        tap((topics) => this.topics$.next(topics)),
        catchError(() => {
          this.error$.next('Impossible de charger les thèmes.');
          this.topics$.next([]);
          return of([]);
        }),
        finalize(() => this.loading$.next(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  onSubmit(): void {
    this.globalError.set(null);
    this.fieldErrors.set({});
    this.clearServerErrorsOnControls();

    if (this.form.invalid || this.submitting()) return;

    const v = this.form.getRawValue();

    // Validators.required + topicId! => ok 
    const payload: CreatePostRequest = {
      topicId: v.topicId!,
      title: v.title,
      content: v.content,
    };

    this.submitting.set(true);

    this.postsApi
      .createPost(payload)
      .pipe(
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (res) => {
          // router.navigate retourne une Promise => ignore le résultat
          void this.router.navigate(['/posts', res.id]);
        },
        error: (err) => this.handleApiError(err),
      });
  }

  private handleApiError(err: unknown): void {
    if (err instanceof HttpErrorResponse && isApiErrorResponse(err.error)) {
      this.globalError.set(err.error.message);

      // Le core renvoie Record<string, string[]>
      const map = toFieldErrorMap(err.error.fieldErrors);
      this.fieldErrors.set(map);
      this.applyServerErrorsToControls(map);

      return;
    }

    this.globalError.set('Une erreur est survenue. Réessaie plus tard.');
  }

  /**
   * Projette les erreurs serveur dans les contrôles pour:
   * - afficher sous champ
   * - marquer touched (sinon mat-error n’apparait pas)
   */
  private applyServerErrorsToControls(map: Record<string, string[]>): void {
    for (const [field, messages] of Object.entries(map)) {
      const ctrl = this.form.get(field);
      if (!ctrl) continue;

      const msg = messages[0] ?? 'Erreur';
      const nextErrors = { ...(ctrl.errors ?? {}), server: msg };

      ctrl.setErrors(nextErrors);
      ctrl.markAsTouched();
    }
  }

  /**
   * Nettoie uniquement l’erreur "server" sans écraser required/others.
   */
  private clearServerErrorsOnControls(): void {
    (['topicId', 'title', 'content'] as const).forEach((field) => {
      const ctrl = this.form.get(field);
      if (!ctrl?.errors) return;

      const errors = ctrl.errors as Record<string, unknown>;
      if (!('server' in errors)) return;

      const { server, ...rest } = errors;
      ctrl.setErrors(Object.keys(rest).length ? rest : null);
    });
  }
}
