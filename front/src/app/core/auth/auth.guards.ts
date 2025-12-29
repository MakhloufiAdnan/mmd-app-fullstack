import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { filter, map, take } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { AuthStore } from './auth.store';

/**
 * Attend la fin du bootstrap SPA (csrf -> refresh) pour éviter
 * des redirections "faux négatif" au chargement.
 */
function waitForInit$(store: AuthStore) {
  return toObservable(store.initialized).pipe(
    filter((v) => v),
    take(1)
  );
}

/**
 * Guard des routes protégées.
 * - Si connecté => OK
 * - Sinon => redirect /login
 */
export const authGuard: CanActivateFn = () => {
  const store = inject(AuthStore);
  const router = inject(Router);

  return waitForInit$(store).pipe(
    map(() => (store.isAuthenticated() ? true : router.parseUrl('/login')))
  );
};

/**
 * Guard des routes publiques (welcome/login/register).
 * - Si connecté => redirect /feed
 * - Sinon => OK
 */
export const publicOnlyGuard: CanActivateFn = () => {
  const store = inject(AuthStore);
  const router = inject(Router);

  return waitForInit$(store).pipe(
    map(() => (store.isAuthenticated() ? router.parseUrl('/feed') : true))
  );
};
