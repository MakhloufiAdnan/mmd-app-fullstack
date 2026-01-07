import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Validateur UX du mot de passe.
 *
 * Exigence du MVP :
 * - 8 caractères minimum
 * - au moins 1 chiffre
 * - au moins 1 minuscule
 * - au moins 1 majuscule
 * - au moins 1 caractère spécial
 *
 * Sécurité :
 * - Ce validateur améliore l'UX.
 * - Le backend reste la source de vérité et doit revalider.
 *
 * @returns ValidatorFn retournant `{ passwordPolicy: true }` si invalide, sinon `null`.
 */
export function passwordPolicyValidator(): ValidatorFn {
  const digit = /\d/;
  const lower = /[a-z]/;
  const upper = /[A-Z]/;
  const special = /[^A-Za-z0-9]/;

  return (control: AbstractControl): ValidationErrors | null => {
    const v = String(control.value ?? '');

    // Le "required" est géré par Validators.required : ne force pas une erreur supplémentaire.
    if (!v) return null;

    const ok = v.length >= 8 && digit.test(v) && lower.test(v) && upper.test(v) && special.test(v);

    return ok ? null : { passwordPolicy: true };
  };
}
