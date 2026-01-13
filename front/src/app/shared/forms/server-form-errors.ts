import { HttpErrorResponse } from '@angular/common/http';
import { WritableSignal } from '@angular/core';
import { AbstractControl, FormGroup } from '@angular/forms';

import { isApiErrorResponse, toFieldErrorMap } from '@core/api/api-error.model';

/**
 * Map standard côté UI :
 * - clé = nom du champ (string)
 * - valeur = liste de messages d’erreur
 */
export type FieldErrorMap = Record<string, string[]>;

export interface HandleApiErrorOptions {
  /** Message fallback si l’erreur n’est pas un payload API typé. */
  genericMessage?: string;
  /** Clé d’erreur injectée dans ctrl.errors (par défaut "server"). */
  errorKey?: string;
}

/**
 * Renvoie le premier message serveur d’un champ, ou null.
 * Utile pour afficher "le message principal" sous un input.
 */
export function firstServerError(map: FieldErrorMap, field: string): string | null {
  return map[field]?.[0] ?? null;
}

/**
 * Applique les erreurs serveur aux contrôles du form :
 * - ctrl.setErrors({ ...existingErrors, [errorKey]: firstMessage })
 * - ctrl.markAsTouched() pour afficher directement l’erreur UI
 */
export function applyServerErrorsToForm(
  form: FormGroup,
  map: FieldErrorMap,
  errorKey: string = 'server'
): void {
  for (const [field, messages] of Object.entries(map)) {
    const ctrl = form.get(field);
    if (!ctrl) continue;

    const msg = messages?.[0] ?? 'Erreur';
    const current = (ctrl.errors ?? {}) as Record<string, unknown>;

    ctrl.setErrors({ ...current, [errorKey]: msg });
    ctrl.markAsTouched();
  }
}

/**
 * Nettoie uniquement l’erreur `errorKey` (ex: "server") sur tous les contrôles du form,
 * sans écraser les autres erreurs (required, minlength, etc.).
 */
export function clearServerErrorsFromForm(form: FormGroup, errorKey: string = 'server'): void {
  const controls = (form.controls ?? {}) as Record<string, AbstractControl>;

  for (const ctrl of Object.values(controls)) {
    if (!ctrl?.errors) continue;

    const errors = ctrl.errors as Record<string, unknown>;
    if (!(errorKey in errors)) continue;

    // Retire uniquement errorKey et conserve le reste
    const { [errorKey]: _removed, ...rest } = errors;
    ctrl.setErrors(Object.keys(rest).length ? rest : null);
  }
}

/**
 * Reset "UI errors":
 * - globalError = null
 * - fieldErrors = {}
 * - supprime les erreurs "server" des contrôles (si form fourni)
 *
 * Objectif : éviter qu’un submit "propre" ré-affiche des erreurs du submit précédent.
 */
export function resetUiErrors(
  globalError: WritableSignal<string | null>,
  fieldErrors: WritableSignal<FieldErrorMap>,
  form?: FormGroup,
  errorKey: string = 'server'
): void {
  globalError.set(null);
  fieldErrors.set({});
  if (form) clearServerErrorsFromForm(form, errorKey);
}

/**
 * Handler générique d’erreur API :
 * - Si HttpErrorResponse + payload conforme ApiErrorResponse => globalError + fieldErrors + projection dans form
 * - Sinon => message générique
 *
 * Le handler ne "throw" jamais : l’objectif est de protéger l’UI.
 */
export function handleApiErrorToUi(
  err: unknown,
  globalError: WritableSignal<string | null>,
  fieldErrors: WritableSignal<FieldErrorMap>,
  form?: FormGroup,
  options: HandleApiErrorOptions = {}
): void {
  const genericMessage = options.genericMessage ?? 'Une erreur est survenue. Réessaie plus tard.';
  const errorKey = options.errorKey ?? 'server';

  if (err instanceof HttpErrorResponse && isApiErrorResponse(err.error)) {
    globalError.set(err.error.message);

    // fieldErrors côté API est un tableau ApiFieldError[]
    // => toFieldErrorMap produit Record<string, string[]>
    const map = toFieldErrorMap(err.error.fieldErrors);
    fieldErrors.set(map);

    if (form) applyServerErrorsToForm(form, map, errorKey);
    return;
  }

  globalError.set(genericMessage);
}
