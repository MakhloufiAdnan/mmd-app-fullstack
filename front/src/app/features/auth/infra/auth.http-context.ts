import { HttpContextToken } from '@angular/common/http';

/**
 * Marque une requête comme "déjà retentée après refresh".
 * Objectif : éviter une boucle infinie (401 -> refresh -> retry -> 401 -> refresh...).
 */
export const AUTH_REFRESH_ATTEMPTED = new HttpContextToken<boolean>(() => false);
