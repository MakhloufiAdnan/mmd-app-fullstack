import { Injectable, computed, signal } from '@angular/core';

/**
 * Store minimal d'authentification (en mémoire).
 *
 * Règles :
 * - L'access token (JWT) est stocké uniquement en mémoire (pas de localStorage).
 * - `initialized` indique que le bootstrap (csrf -> refresh) a été tenté.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  /** Access token JWT en mémoire. */
  private readonly _accessToken = signal<string | null>(null);
  readonly accessToken = this._accessToken.asReadonly();

  /** Indique si le bootstrap SPA a été exécuté (réussi ou non). */
  private readonly _initialized = signal(false);
  readonly initialized = this._initialized.asReadonly();

  /** Utilisateur considéré "auth" si un access token est présent. */
  readonly isAuthenticated = computed(() => !!this._accessToken());

  /** Définit/écrase le token en mémoire. */
  setAccessToken(token: string | null): void {
    this._accessToken.set(token);
  }

  /** Purge le token. */
  clear(): void {
    this._accessToken.set(null);
  }

  /** Marque la fin du bootstrap (même si refresh échoue). */
  markInitialized(): void {
    this._initialized.set(true);
  }
}
