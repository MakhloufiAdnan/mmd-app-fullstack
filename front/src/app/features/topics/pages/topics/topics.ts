import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { BehaviorSubject, Subject, combineLatest, EMPTY, of } from 'rxjs';
import { catchError, exhaustMap, finalize, map, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';

import { TopicsApiService } from '../../services/topics-api.service';
import type { TopicListItem } from '../../interfaces/topic.models';

type TopicsVm = {
  loading: boolean;
  error: string | null;
  topics: TopicListItem[];
  pendingIds: Set<number>;
};

@Component({
  selector: 'app-topics',
  standalone: true,
  imports: [AsyncPipe, MatCardModule, MatButtonModule, MatProgressSpinnerModule, MatDividerModule],
  templateUrl: './topics.html',
  styleUrl: './topics.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopicsComponent {
  private readonly api = inject(TopicsApiService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly topics$ = new BehaviorSubject<TopicListItem[]>([]);
  private readonly loading$ = new BehaviorSubject<boolean>(true);
  private readonly error$ = new BehaviorSubject<string | null>(null);
  private readonly pendingIds$ = new BehaviorSubject<Set<number>>(new Set());

  private readonly subscribeClicks$ = new Subject<number>();

  readonly vm$ = combineLatest({
    topics: this.topics$,
    loading: this.loading$,
    error: this.error$,
    pendingIds: this.pendingIds$,
  }).pipe(map((v): TopicsVm => v));

  constructor() {
    this.loadTopics();

    this.subscribeClicks$
      .pipe(
        // Ignore tous les clics tant qu'un subscribe est en cours
        exhaustMap((topicId) => {
          // Si déjà pending => on n'appelle pas
          if (this.pendingIds$.value.has(topicId)) return EMPTY;

          // Si déjà abonné => on n'appelle pas
          const topic = this.topics$.value.find((t) => t.id === topicId);
          if (!topic || topic.subscribed) return EMPTY;

          this.setPending(topicId, true);

          return this.api.subscribeToTopic(topicId).pipe(
            tap(() => this.markSubscribed(topicId)),
            catchError((err) => {
              // Contrat possible : 409 déjà abonné => on "répare" l'UI silencieusement
              if (err?.status === 409) {
                this.markSubscribed(topicId);
                return EMPTY;
              }
              this.error$.next("Impossible de s'abonner pour le moment.");
              return EMPTY;
            }),
            finalize(() => this.setPending(topicId, false))
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  onSubscribe(topicId: number): void {
    this.error$.next(null);
    this.subscribeClicks$.next(topicId);
  }

  trackById = (_: number, item: TopicListItem) => item.id;

  private loadTopics(): void {
    this.loading$.next(true);
    this.error$.next(null);

    this.api
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

  private setPending(topicId: number, isPending: boolean): void {
    const next = new Set(this.pendingIds$.value);
    if (isPending) next.add(topicId);
    else next.delete(topicId);
    this.pendingIds$.next(next);
  }

  private markSubscribed(topicId: number): void {
    const next = this.topics$.value.map((t) => (t.id === topicId ? { ...t, subscribed: true } : t));
    this.topics$.next(next);
  }
}
