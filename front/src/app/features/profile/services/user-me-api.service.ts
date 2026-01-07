import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import type {
  UpdateMeRequest,
  UpdatedResponse,
  UserMeResponse,
} from '../interfaces/user-me.models';

/**
 * Service API "profil" : /api/users/me.
 */
@Injectable({ providedIn: 'root' })
export class UserMeApiService {
  private readonly http = inject(HttpClient);

  /**
   * Récupère le profil courant.
   * Endpoint : GET /api/users/me
   */
  me(): Observable<UserMeResponse> {
    return this.http.get<UserMeResponse>('/api/users/me', { withCredentials: true });
  }

  /**
   * Met à jour le profil courant.
   * Endpoint : PUT /api/users/me
   */
  updateMe(payload: UpdateMeRequest): Observable<UpdatedResponse> {
    return this.http.put<UpdatedResponse>('/api/users/me', payload, { withCredentials: true });
  }

  /**
   * Se désabonner d'un thème.
   * Endpoint : DELETE /api/users/me/subscriptions/{topicId}
   */
  unsubscribeFromTopic(topicId: number): Observable<void> {
    return this.http.delete<void>(`/api/users/me/subscriptions/${topicId}`, {
      withCredentials: true,
    });
  }
}
