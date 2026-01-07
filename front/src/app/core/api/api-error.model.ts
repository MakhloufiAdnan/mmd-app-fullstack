/**
 * Modèle d'erreur API côté front.
 *
 * Objectif :
 * - Centraliser le parsing/typage des erreurs renvoyées par le back
 * - Permettre une gestion cohérente des messages globaux et des erreurs de champs
 *
 * Note :
 * - Les valeurs exactes de `ApiErrorCode` doivent rester alignées avec le contrat backend.
 */

/** Codes d'erreur applicatifs attendus. */
export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'INTERNAL';

/** Erreur portée par un champ (utile pour les formulaires). */
export interface ApiFieldError {
  field: string;
  message: string;
}

/** Payload d'erreur standard renvoyé par l'API. */
export interface ApiErrorResponse {
  error: ApiErrorCode;
  message: string;
  fieldErrors?: ApiFieldError[];
}

/**
 * Type guard : vérifie que la valeur ressemble à une erreur API standard.
 *
 * @param v Valeur inconnue (souvent `HttpErrorResponse.error`)
 * @returns true si le payload est compatible avec ApiErrorResponse
 */
export function isApiErrorResponse(v: unknown): v is ApiErrorResponse {
  if (!v || typeof v !== 'object') return false;
  const o = v as Record<string, unknown>;
  return typeof o['error'] === 'string' && typeof o['message'] === 'string';
}

/**
 * Transforme `fieldErrors` en map `{ [field]: messages[] }`.
 * Utile pour afficher facilement les erreurs sous les inputs correspondants.
 *
 * @param fieldErrors Liste optionnelle d'erreurs de champs
 */
export function toFieldErrorMap(fieldErrors?: ApiFieldError[]): Record<string, string[]> {
  const map: Record<string, string[]> = {};
  for (const fe of fieldErrors ?? []) {
    (map[fe.field] ??= []).push(fe.message);
  }
  return map;
}
