import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/auth/pages/welcome/welcome').then((m) => m.Welcome),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/pages/register/register').then((m) => m.Register),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login/login').then((m) => m.Login),
  },

  /**
   * Écran "connecté" temporaire pour valider le parcours UI après login.
   * Todo: La route deviendra protégée (authGuard).
   */
  {
    path: 'feed',
    loadComponent: () => import('./features/feed/pages/feed/feed').then((m) => m.Feed),
  },

  { path: '**', redirectTo: '' },
];
