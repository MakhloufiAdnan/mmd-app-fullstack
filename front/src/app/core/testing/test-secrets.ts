/**
 * Génère une chaîne conforme à la policy (front) sans exposer un littéral directement.
 *
 * Note : donnée de test non sensible.
 */
export function buildValidSecret(): string {
  return `Aa1!${'a'.repeat(4)}`; // => "Aa1!aaaa"
}
