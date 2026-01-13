import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthStore } from '../state/auth.store';
import { AuthFacade } from '../state/auth.facade';
import { AUTH_REFRESH_ATTEMPTED } from './auth.http-context';

/**
 * Interceptor Auth :
 * - Ajoute `Authorization: Bearer <token>` sur endpoints (sauf csrf/login/register/refresh)
 * - Sur 401 : tente 1 refresh puis rejoue la requête
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(AuthStore);
  const facade = inject(AuthFacade);
  const router = inject(Router);

  const token = store.accessToken();

  // On évite d'ajouter Authorization sur les endpoints publics d'auth (sauf logout qui est vérouillé côté back).
  const isCsrf = req.url.endsWith('/api/auth/csrf');
  const isLogin = req.url.endsWith('/api/auth/login');
  const isRegister = req.url.endsWith('/api/auth/register');
  const isRefresh = req.url.endsWith('/api/auth/refresh');

  const skipAuthHeader = isCsrf || isLogin || isRegister || isRefresh;

  const authReq =
    token && !skipAuthHeader
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authReq).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
        return throwError(() => err);
      }

      // pas de refresh sur refresh, sinon boucle
      if (isRefresh) {
        return throwError(() => err);
      }

      // une seule tentative refresh->retry
      if (req.context.get(AUTH_REFRESH_ATTEMPTED)) {
        return throwError(() => err);
      }

      return facade.refreshAccessTokenOnce().pipe(
        switchMap((newToken) => {
          if (!newToken) {
            router.navigateByUrl('/login').catch(() => undefined);
            return throwError(() => err);
          }

          const retryReq = req.clone({
            setHeaders: { Authorization: `Bearer ${newToken}` },
            context: req.context.set(AUTH_REFRESH_ATTEMPTED, true),
          });

          return next(retryReq);
        })
      );
    })
  );
};
