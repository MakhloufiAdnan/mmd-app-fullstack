import { AbstractControl, FormControl, ValidationErrors, ValidatorFn } from '@angular/forms';

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
 * Le back reste la source de vérité.
 */
export function passwordPolicyValidator(): ValidatorFn {
  const digit = /\d/;
  const lower = /[a-z]/;
  const upper = /[A-Z]/;
  const special = /[^A-Za-z0-9]/;

  return (control: AbstractControl): ValidationErrors | null => {
    // IMPORTANT : trim pour que "   " soit traité comme vide (cohérent avec submit())
    const v = String(control.value ?? '').trim();

    // required est géré ailleurs (Validators.required)
    if (!v) return null;

    const ok =
      v.length >= 8 &&
      digit.test(v) &&
      lower.test(v) &&
      upper.test(v) &&
      special.test(v);

    return ok ? null : { passwordPolicy: true };
  };
}

it('should return null when only spaces (trim)', () => {
  const control = new FormControl('   ');
  const result = passwordPolicyValidator()(control);
  expect(result).toBeNull();
});