import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { provideRouter, Router, UrlTree } from '@angular/router';

import { AuthStore } from '../state/auth.store';
import { authGuard, publicOnlyGuard } from './auth.guards';

@Component({ standalone: true, template: '' })
class Dummy {}

describe('Auth Guards', () => {
  let store: AuthStore;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'login', component: Dummy },
          { path: 'feed', component: Dummy },
        ]),
      ],
    });

    store = TestBed.inject(AuthStore);
    router = TestBed.inject(Router);
  });

  it('authGuard: allow when authenticated', async () => {
    store.setAccessToken('t');
    store.markInitialized();

    const result = await TestBed.runInInjectionContext(async () => {
      const out = authGuard({} as any, {} as any);
      // Guard retourne Observable => on l'attend via router + microtask
      return await new Promise<boolean | UrlTree>((resolve) => (out as any).subscribe(resolve));
    });

    expect(result).toBe(true);
  });

  it('authGuard: redirect /login when not authenticated', async () => {
    store.clear();
    store.markInitialized();

    const result = await TestBed.runInInjectionContext(async () => {
      const out = authGuard({} as any, {} as any);
      return await new Promise<boolean | UrlTree>((resolve) => (out as any).subscribe(resolve));
    });

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });

  it('publicOnlyGuard: redirect /feed when authenticated', async () => {
    store.setAccessToken('t');
    store.markInitialized();

    const result = await TestBed.runInInjectionContext(async () => {
      const out = publicOnlyGuard({} as any, {} as any);
      return await new Promise<boolean | UrlTree>((resolve) => (out as any).subscribe(resolve));
    });

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/feed');
  });
});
