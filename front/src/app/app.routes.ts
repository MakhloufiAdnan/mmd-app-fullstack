import { Routes } from '@angular/router';
import { authGuard, publicOnlyGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    canActivate: [publicOnlyGuard],
    loadComponent: () => import('./features/auth/pages/welcome/welcome').then((m) => m.Welcome),
  },
  {
    path: 'register',
    canActivate: [publicOnlyGuard],
    loadComponent: () => import('./features/auth/pages/register/register').then((m) => m.Register),
  },
  {
    path: 'login',
    canActivate: [publicOnlyGuard],
    loadComponent: () => import('./features/auth/pages/login/login').then((m) => m.Login),
  },

  // Route protégée 
  {
    path: 'feed',
    canActivate: [authGuard],
    loadComponent: () => import('./features/feed/pages/feed/feed').then((m) => m.Feed),
  },

  { path: '**', redirectTo: '' },
];
