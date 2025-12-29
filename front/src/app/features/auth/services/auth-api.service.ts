import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import type {
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  TokenResponse,
} from '../interfaces/auth.models';

/**
 * Thin API client for /api/auth endpoints.
 *
 * Notes:
 * - withCredentials=true garantit que le refresh cookie sera bien envoyé
 *   même si on n'utilise plus le proxy (CORS).
 * - CSRF: Angular attachera automatiquement X-XSRF-TOKEN sur POST/PUT/DELETE
 *   via withXsrfConfiguration(...)
 */
@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);

  csrf(): Observable<void> {
    return this.http.get<void>('/api/auth/csrf', { withCredentials: true });
  }

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>('/api/auth/register', payload, {
      withCredentials: true,
    });
  }

  login(payload: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/login', payload, {
      withCredentials: true,
    });
  }

  refresh(): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/refresh', {}, { withCredentials: true });
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}, { withCredentials: true });
  }
}
