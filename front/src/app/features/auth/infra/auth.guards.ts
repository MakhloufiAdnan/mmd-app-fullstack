import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';
import { filter, map, take } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { AuthStore } from '../state/auth.store';

/**
 * Attend la fin de l'initialisation auth (csrf -> refresh) pour éviter un redirect trop tôt au chargement.
 */
function waitForInit$(store: AuthStore) {
  return toObservable(store.initialized).pipe(
    filter((v) => v),
    take(1)
  );
}

/**
 * Vérifie l'auth :
 * - si authentifié => true
 * - sinon => redirection /login
 */
function checkAuth$() {
  const store = inject(AuthStore);
  const router = inject(Router);

  return waitForInit$(store).pipe(
    map(() => (store.isAuthenticated() ? true : router.parseUrl('/login')))
  );
}

/** Guard route protégée. */
export const authGuard: CanActivateFn = () => checkAuth$();

/** Guard layout wrapper (enfants). */
export const authChildGuard: CanActivateChildFn = () => checkAuth$();

/**
 * Guard routes publiques :
 * - si connecté => redirect /feed
 * - sinon => OK
 */
export const publicOnlyGuard: CanActivateFn = () => {
  const store = inject(AuthStore);
  const router = inject(Router);

  return waitForInit$(store).pipe(
    map(() => (store.isAuthenticated() ? router.parseUrl('/feed') : true))
  );
};
