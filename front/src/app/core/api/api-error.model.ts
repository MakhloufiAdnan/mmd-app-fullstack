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
    const field = fe.field;
    const messages = map[field] ?? [];
    messages.push(fe.message);
    map[field] = messages;
  }
  return map;
}
