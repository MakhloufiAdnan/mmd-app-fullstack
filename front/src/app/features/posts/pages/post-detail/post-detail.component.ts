import { AsyncPipe, DatePipe, Location } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, of, Subject } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  finalize,
  map,
  startWith,
  switchMap,
} from 'rxjs/operators';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PostsApiService } from '@features/posts/services/posts-api.service';
import { PostDetailResponse } from '@features/posts/interfaces/post.models';

/**
 * ✅ Helper factorisé :
 * - reset erreurs UI + suppression "server" sur controls
 * - mapping erreur API typée -> globalError + fieldErrors + projection controls
 * - lecture premier message serveur (pour prioriser sur required)
 */
import {
  firstServerError,
  handleApiErrorToUi,
  resetUiErrors,
} from '@shared/forms/server-form-errors';

type PostDetailVm =
  | { loading: true; error: null; post: null }
  | { loading: false; error: string | null; post: PostDetailResponse | null };

@Component({
  selector: 'app-post-detail',
  standalone: true,
  imports: [
    AsyncPipe,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './post-detail.component.html',
  styleUrl: './post-detail.component.scss',
})
export class PostDetailComponent {
  private readonly postsApi = inject(PostsApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  /**
   * Trigger "refresh" :
   * - après POST commentaire, on déclenche un refetch du détail (vm$)
   */
  private readonly refresh$ = new Subject<void>();

  /**
   * UI state erreurs :
   * - submitting : bloque double submit
   * - globalError : message global
   * - fieldErrors : erreurs par champ (server-side)
   */
  readonly submitting = signal(false);
  readonly globalError = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string[]>>({});

  /**
   * Form commentaire :
   * - required seulement côté front, le back valide le reste.
   */
  readonly commentForm = this.fb.nonNullable.group({
    content: this.fb.nonNullable.control('', { validators: [Validators.required] }),
  });

  /**
   * PostId depuis l’URL :
   * - on convertit en number
   * - distinctUntilChanged pour éviter re-fetch inutile
   */
  private readonly postId$ = this.route.paramMap.pipe(
    map((pm) => Number(pm.get('postId'))),
    distinctUntilChanged()
  );

  /**
   * VM :
   * - combineLatest(postId$, refresh$) => permet refetch après refresh$.next()
   * - GET /api/posts/{id}
   * - tri des commentaires par date desc (UI-friendly)
   */
  readonly vm$ = combineLatest([this.postId$, this.refresh$.pipe(startWith(void 0))]).pipe(
    switchMap(([postId]) =>
      this.postsApi.getPost(postId).pipe(
        map((post) => {
          const sorted: PostDetailResponse = {
            ...post,
            comments: [...post.comments].sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
          };
          return { loading: false, error: null, post: sorted } as PostDetailVm;
        }),
        startWith({ loading: true, error: null, post: null } as PostDetailVm),
        catchError(() =>
          of({
            loading: false,
            error: "Impossible de charger l'article.",
            post: null,
          } as PostDetailVm)
        )
      )
    ),
    takeUntilDestroyed(this.destroyRef)
  );

  /**
   * Message d’erreur pour le commentaire :
   * - priorité à l’erreur serveur (si présent)
   * - sinon required si touched
   *
   * ✅ Fix + factorisation :
   * - fieldErrors est un signal => this.fieldErrors()
   */
  commentErrorMessage(): string | null {
    const server = firstServerError(this.fieldErrors(), 'content');
    if (server) return server;

    const ctrl = this.commentForm.get('content');
    if (ctrl?.touched && ctrl.hasError('required')) return 'Le commentaire est requis.';

    return null;
  }

  /**
   * Submit commentaire :
   * - reset erreurs UI + nettoyage "server" sur control
   * - validation (invalid / submitting)
   * - call API
   * - reset form + refresh détail sur succès
   */
  onSubmitComment(postId: number): void {
    // ✅ factorisé : reset + clear server errors
    resetUiErrors(this.globalError, this.fieldErrors, this.commentForm);

    // Guard
    if (this.commentForm.invalid || this.submitting()) return;

    const content = this.commentForm.getRawValue().content;

    this.submitting.set(true);

    this.postsApi
      .addComment(postId, content)
      .pipe(
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          // reset du champ + refresh de la VM
          this.commentForm.reset({ content: '' });
          this.refresh$.next();
        },
        error: (err) => this.handleApiError(err),
      });
  }

  /**
   * Navigation back :
   * - si historique suffisant => back
   * - sinon fallback vers /feed
   */
  goBack(): void {
    const historyLen = globalThis.history?.length ?? 0;

    if (historyLen > 1) {
      this.location.back();
      return;
    }
    this.router.navigateByUrl('/feed');
  }

  /**
   * Handler d’erreur API :
   * - typed API => globalError + fieldErrors + setErrors(server) sur control
   * - sinon => message générique
   *
   * ✅ factorisé via helper
   */
  private handleApiError(err: unknown): void {
    // isApiErrorResponse est utilisé dans le helper, import conservé si utilisé ailleurs
    // (sinon tu peux retirer l'import de isApiErrorResponse ci-dessus)
    handleApiErrorToUi(err, this.globalError, this.fieldErrors, this.commentForm);
  }
}
