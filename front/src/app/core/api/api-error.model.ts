/**
 * Source: api-contract.md > "Format d’erreur (unique)".
 */
export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'INTERNAL';

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  error: ApiErrorCode;
  message: string;
  fieldErrors?: ApiFieldError[];
}

export function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (value === null || typeof value !== 'object') return false;

  const v = value as Partial<ApiErrorResponse>;
  return (
    typeof v.error === 'string' &&
    typeof v.message === 'string' &&
    (v.fieldErrors === undefined ||
      (Array.isArray(v.fieldErrors) &&
        v.fieldErrors.every(
          (e) =>
            e !== null &&
            typeof e === 'object' &&
            typeof (e as Partial<ApiFieldError>).field === 'string' &&
            typeof (e as Partial<ApiFieldError>).message === 'string'
        )))
  );
}

/**
 * Utility to convert fieldErrors array to a map: field -> message[]
 */
export function toFieldErrorMap(
  fieldErrors: ApiFieldError[] | undefined
): Record<string, string[]> {
  const map: Record<string, string[]> = {};
  for (const fe of fieldErrors ?? []) {
    (map[fe.field] ??= []).push(fe.message);
  }
  return map;
}
