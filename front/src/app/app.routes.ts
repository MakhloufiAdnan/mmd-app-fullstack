import { Routes } from '@angular/router';
import { authChildGuard, publicOnlyGuard } from './features/auth/infra/auth.guards';

/**
 * Routing global.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
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

  /**
   * Layout authentifié (header + drawer mobile).
   * Protège les enfants via canActivateChild.
   */
  {
    path: '',
    canActivateChild: [authChildGuard],
    loadComponent: () => import('./shell/auth-shell/auth-shell').then((m) => m.AuthShell),
    children: [
      {
        path: 'feed',
        loadComponent: () => import('./features/feed/pages/feed/feed').then((m) => m.Feed),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/pages/profile/profile').then((m) => m.Profile),
      },

      {
        path: 'topics',
        loadComponent: () =>
          import('./features/topics/pages/topics/topics').then((m) => m.TopicsComponent),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
