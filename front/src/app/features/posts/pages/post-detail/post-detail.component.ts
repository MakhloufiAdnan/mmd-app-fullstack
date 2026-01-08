import { AsyncPipe, DatePipe, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
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
import { isApiErrorResponse, toFieldErrorMap } from '@core/api/api-error.model';

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

  /** Refresh trigger : après POST commentaire, on refetch le détail */
  private readonly refresh$ = new Subject<void>();

  /** UI state erreurs */
  readonly submitting = signal(false);
  readonly globalError = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string[]>>({});

  /** Form commentaire : required uniquement, le back valide le reste. */
  readonly commentForm = this.fb.nonNullable.group({
    content: this.fb.nonNullable.control('', { validators: [Validators.required] }),
  });

  /** PostId depuis l’URL */
  private readonly postId$ = this.route.paramMap.pipe(
    map((pm) => Number(pm.get('postId'))),
    distinctUntilChanged()
  );

  /** VM = (postId + refresh) -> GET /api/posts/{id} */
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

  commentErrorMessage(): string | null {
    const server = this.fieldErrors()['content']?.[0];
    if (server) return server;

    const ctrl = this.commentForm.get('content');
    if (ctrl?.touched && ctrl.hasError('required')) return 'Le commentaire est requis.';

    return null;
  }

  onSubmitComment(postId: number): void {
    this.globalError.set(null);
    this.fieldErrors.set({});
    this.clearServerErrorsOnControls();

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
          this.commentForm.reset({ content: '' });
          this.refresh$.next();
        },
        error: (err) => this.handleApiError(err),
      });
  }

  goBack(): void {
    const historyLen = globalThis.history?.length ?? 0;

    if (historyLen > 1) {
      this.location.back();
      return;
    }
    this.router.navigateByUrl('/feed');
  }

  private handleApiError(err: unknown): void {
    if (err instanceof HttpErrorResponse && isApiErrorResponse(err.error)) {
      this.globalError.set(err.error.message);

      const map = toFieldErrorMap(err.error.fieldErrors);
      this.fieldErrors.set(map);
      this.applyServerErrorsToControls(map);

      return;
    }

    this.globalError.set('Une erreur est survenue. Réessaie plus tard.');
  }

  private applyServerErrorsToControls(map: Record<string, string[]>): void {
    for (const [field, messages] of Object.entries(map)) {
      const ctrl = this.commentForm.get(field);
      if (!ctrl) continue;

      const msg = messages[0] ?? 'Erreur';
      const current = ctrl.errors;

      ctrl.setErrors(current ? { ...current, server: msg } : { server: msg });
      ctrl.markAsTouched();
    }
  }

  private clearServerErrorsOnControls(): void {
    const ctrl = this.commentForm.get('content');
    if (!ctrl?.errors) return;

    const errors = ctrl.errors as Record<string, unknown>;
    if (!('server' in errors)) return;

    const { server, ...rest } = errors;
    ctrl.setErrors(Object.keys(rest).length ? rest : null);
  }
}
