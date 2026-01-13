import { AsyncPipe } from '@angular/common';
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

/**
 * ✅ Helper factorisé :
 * - reset des erreurs UI (global + map champ) + suppression erreur "server" sur les controls
 * - mapping HttpErrorResponse(API typée) -> globalError + fieldErrors + projection dans les controls
 * - extraction "premier message" par champ
 */
import {
  firstServerError,
  handleApiErrorToUi,
  resetUiErrors,
} from '@shared/forms/server-form-errors';

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
  styleUrl: './post-create.component.scss',
})
export class PostCreateComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly topicsApi = inject(TopicsApiService);
  private readonly postsApi = inject(PostsApiService);

  /**
   * État "data loading" pour la liste de topics.
   * On garde BehaviorSubject car c’est pratique à consommer en AsyncPipe.
   */
  readonly topics$ = new BehaviorSubject<TopicListItem[]>([]);
  readonly loading$ = new BehaviorSubject<boolean>(true);
  readonly error$ = new BehaviorSubject<string | null>(null);

  /**
   * Erreurs UI :
   * - submitting : évite double-submit
   * - globalError : message en haut (ex: "Validation failed")
   * - fieldErrors : map champ -> messages (ex: { title: ["required"] })
   */
  readonly submitting = signal(false);
  readonly globalError = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string[]>>({});

  /**
   * Form :
   * - topicId nullable au départ (aucun choix)
   * - title/content NON NULLABLES => payload sans "string | null"
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
   * (utilitaire UI)
   */
  hasAnySubscribedTopics(topics: TopicListItem[]): boolean {
    return topics.some((t) => t.subscribed);
  }

  /**
   * Renvoie le 1er message serveur pour un champ.
   * ✅ factorisé via helper (évite duplication + style cohérent)
   */
  getFieldErrorFirst(field: 'topicId' | 'title' | 'content'): string | null {
    return firstServerError(this.fieldErrors(), field);
  }

  /**
   * Charge la liste de topics :
   * - loading true au début
   * - topics$ alimenté sur succès
   * - fallback user-friendly sur erreur
   */
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

  /**
   * Submit création de post :
   * - reset erreurs UI + nettoyage erreurs "server" dans les controls
   * - validation (invalid / submitting)
   * - call API
   * - navigation vers /posts/:id sur succès
   */
  onSubmit(): void {
    // ✅ factorisé : reset + clear server errors
    resetUiErrors(this.globalError, this.fieldErrors, this.form);

    // Guard : invalide OU déjà en soumission
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    // Construction payload typé
    const v = this.form.getRawValue();
    const payload: CreatePostRequest = {
      topicId: v.topicId!, // safe car required + form.valid déjà check
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
          // navigate retourne une Promise : on ignore le résultat
          void this.router.navigate(['/posts', res.id]);
        },
        error: (err) => this.handleApiError(err),
      });
  }

  /**
   * Handler d’erreur :
   * - si erreur API typée => globalError + fieldErrors + projection dans les controls
   * - sinon => message générique
   *
   * ✅ factorisé via helper
   */
  private handleApiError(err: unknown): void {
    // isApiErrorResponse est utilisé dans le helper, import conservé si utilisé ailleurs
    // (sinon tu peux retirer l'import de isApiErrorResponse ci-dessus)
    handleApiErrorToUi(err, this.globalError, this.fieldErrors, this.form);
  }
}
