import { Injectable, inject } from '@angular/core';
import { Observable, catchError, finalize, firstValueFrom, map, of, shareReplay, switchMap, tap } from 'rxjs';

import type { LoginRequest, RegisterRequest, RegisterResponse, TokenResponse } from '../interfaces/auth.models';
import { AuthApiService } from '../services/auth-api.service';
import { AuthStore } from './auth.store';

/**
 * Facade d'auth : orchestration du flux SPA.
 *
 * Contrat :
 * - Au démarrage : csrf() puis refresh()
 * - Access token en mémoire
 * - Refresh unique en cas de 401 puis retry côté interceptor
 */
@Injectable({ providedIn: 'root' })
export class AuthFacade {
  private readonly api = inject(AuthApiService);
  private readonly store = inject(AuthStore);

  /**
   * Refresh en cours (déduplication).
   * Objectif : si 5 requêtes tombent en 401 en même temps, ne fait qu'UN refresh.
   */
  private refreshInFlight$?: Observable<string | null>;

  /**
   * Bootstrap SPA : GET csrf puis POST refresh.
   * - Ne doit jamais bloquer l'app : "tente", puis marque initialized.
   */
  bootstrap(): Promise<void> {
    return firstValueFrom(this.bootstrap$());
  }

  bootstrap$(): Observable<void> {
    return this.api.csrf().pipe(
      catchError(() => of(void 0)),
      switchMap(() =>
        this.api.refresh().pipe(
          tap((res) => this.store.setAccessToken(res.accessToken)),
          map(() => void 0),
          // Si refresh échoue (401 cookie absent / 403 csrf absent) => pas connecté
          catchError(() => {
            this.store.setAccessToken(null);
            return of(void 0);
          })
        )
      ),
      finalize(() => this.store.markInitialized())
    );
  }

  /**
   * Login :
   * - csrf() best-effort (sécurise si le cookie CSRF n'est pas encore présent)
   * - login() => stocke access token en mémoire
   */
  login(payload: LoginRequest): Observable<TokenResponse> {
    return this.api.csrf().pipe(
      catchError(() => of(void 0)),
      switchMap(() => this.api.login(payload)),
      tap((res) => this.store.setAccessToken(res.accessToken))
    );
  }

  /**
   * Register : simple proxy vers l'API.
   * (Ne connecte pas automatiquement : redirection vers /login ensuite.)
   */
  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.api.register(payload);
  }

  /**
   * Logout :
   * - appelle /logout (cookie + CSRF requis côté back)
   * - purge aussi l'access token en mémoire (quoi qu'il arrive)
   */
  logout(): Observable<void> {
    return this.api.logout().pipe(
      catchError(() => of(void 0)),
      finalize(() => this.store.clear())
    );
  }

  /**
   * Refresh dédupliqué :
   * - Renvoie le nouveau token si succès, sinon null.
   * - Utilisé par l'interceptor sur 401.
   */
  refreshAccessTokenOnce(): Observable<string | null> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    const refresh$ = this.api.refresh().pipe(
      tap((res) => this.store.setAccessToken(res.accessToken)),
      map((res) => res.accessToken),
      catchError(() => {
        this.store.setAccessToken(null);
        return of(null);
      }),
      finalize(() => {
        this.refreshInFlight$ = undefined;
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.refreshInFlight$ = refresh$;
    return refresh$;
  }
}
