import { HttpErrorResponse } from '@angular/common/http';
import { computed, Signal, WritableSignal } from '@angular/core';
import { AbstractControl, FormGroup } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs';

import { isApiErrorResponse, toFieldErrorMap } from '@core/api/api-error.model';

export type FieldErrors = Record<string, string[]> | null;

export function createFormSubmitSignals(
  form: AbstractControl,
  submitting: Signal<boolean>,
) {
  const formStatus$ = form.statusChanges.pipe(startWith(form.status));
  const formStatus = toSignal(formStatus$, { initialValue: form.status });

  const canSubmit = computed(() => formStatus() === 'VALID' && !submitting());

  return { formStatus, canSubmit };
}

/**
 * - reset erreurs
 * - si invalid -> touch + return null
 * - sinon : passe submitting à true + retourne le payload typé
 */
export function prepareSubmit<T>(
  form: FormGroup,
  submitting: WritableSignal<boolean>,
  globalError: WritableSignal<string | null>,
  fieldErrors: WritableSignal<FieldErrors>,
): T | null {
  globalError.set(null);
  fieldErrors.set(null);

  if (form.invalid) {
    form.markAllAsTouched();
    return null;
  }

  submitting.set(true);
  return form.getRawValue() as T;
}

/** Normalise l’erreur API en global + fieldErrors, sinon message générique. */
export function setApiErrors(
  err: unknown,
  globalError: WritableSignal<string | null>,
  fieldErrors: WritableSignal<FieldErrors>,
  fallbackMessage = 'Une erreur est survenue. Réessaie plus tard.',
): void {
  if (err instanceof HttpErrorResponse && isApiErrorResponse(err.error)) {
    globalError.set(err.error.message);
    fieldErrors.set(toFieldErrorMap(err.error.fieldErrors));
    return;
  }
  globalError.set(fallbackMessage);
}
