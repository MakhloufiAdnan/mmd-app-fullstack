import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';
import { AuthFacade } from './features/auth/state/auth.facade';
import { authInterceptor } from './features/auth/infra/auth.interceptor';

/**
 * Initialise l'app : bootstrap SPA auth (csrf -> refresh).
 * Important : on "tente" toujours puis on laisse l'app démarrer.
 */
export function authAppInitializer(authFacade: AuthFacade): () => Promise<void> {
  return () => authFacade.bootstrap();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimations(),

    // Bootstrap auth
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: authAppInitializer,
      deps: [AuthFacade],
    },

    provideHttpClient(
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN',
      }),
      withInterceptors([authInterceptor])
    ),
  ],
};
