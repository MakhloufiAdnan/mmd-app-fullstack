/**
 * Modèles alignés sur le contrat API (MVP)
 */

export interface TopicDto {
  id: number;
  name: string;
}

/** Réponse de GET /api/users/me */
export interface UserMeResponse {
  id: number;
  email: string;
  username: string;
  subscriptions: TopicDto[];
}

/**
 * Requête de mise à jour (PUT /api/users/me).
 * n'envoie que ce qui a changé.
 */
export interface UpdateMeRequest {
  email?: string | null;
  username?: string | null;
  password?: string | null;
}

/** Réponse de PUT /api/users/me */
export interface UpdatedResponse {
  updated: boolean;
}
