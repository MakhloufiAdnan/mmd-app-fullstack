import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  distinctUntilChanged,
  map,
  of,
  shareReplay,
  startWith,
  switchMap,
  catchError,
} from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';

import { FeedApiService } from '@features/feed/services/feed-api.service';
import { FeedItem, FeedOrder } from '@features/feed/interfaces/feed.models';
import { parseOrder, formatComments } from '@features/feed/utils/feed.utils';

type FeedVm = {
  loading: boolean;
  error: string | null;
  feed: FeedItem[];
  order: FeedOrder;
};

@Component({
  selector: 'mdd-feed',
  standalone: true,
  imports: [
    AsyncPipe,
    DatePipe,
    MatCardModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatMenuModule,
    MatIconModule,
    RouterLink,
  ],
  templateUrl: './feed.html',
  styleUrl: './feed.scss',
})
export class Feed {
  private readonly api = inject(FeedApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // Expose util en template sans recréer une méthode
  readonly formatComments = formatComments;

  /**
   * Source de vérité: l’URL.
   * order$ réagit automatiquement si l’utilisateur change le tri via le toggle
   * (ou si l’URL est modifiée manuellement).
   */
  private readonly order$ = this.route.queryParamMap.pipe(
    map((pm) => parseOrder(pm.get('order'))),
    distinctUntilChanged(),
    // ShareReplay: plusieurs abonnés -> pas de recalcul inutile
    shareReplay({ bufferSize: 1, refCount: true })
  );

  /**
   * VM unique : évite state doublon + évite double requêtes.
   * switchMap => "dernière intention utilisateur gagne"
   */
  readonly vm$ = this.order$.pipe(
    switchMap((order) =>
      this.api.listFeed(order).pipe(
        map((feed) => ({ loading: false, error: null, feed, order } satisfies FeedVm)),
        startWith({ loading: true, error: null, feed: [], order } satisfies FeedVm),
        catchError(() =>
          of({
            loading: false,
            error: 'Impossible de charger le feed.',
            feed: [],
            order,
          } satisfies FeedVm)
        )
      )
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
    takeUntilDestroyed(this.destroyRef)
  );

  onOrderChange(order: FeedOrder): void {
    this.router
      .navigate([], {
        relativeTo: this.route,
        queryParams: { order },
      })
      .catch(() => undefined);
  }
}
