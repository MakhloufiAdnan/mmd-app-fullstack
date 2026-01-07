import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';

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

    // Guards depend on the "initialized" signal for deterministic behavior.
    store.markInitialized();
  });

  it('authGuard: allow when authenticated', async () => {
    store.setAccessToken('t');

    const result = await TestBed.runInInjectionContext(async () => {
      const out = authGuard({} as any, {} as any);
      return firstValueFrom(out as any);
    });

    expect(result).toBeTrue();
  });

  it('authGuard: redirect /login when NOT authenticated', async () => {
    store.clear();

    const result = await TestBed.runInInjectionContext(async () => {
      const out = authGuard({} as any, {} as any);
      return firstValueFrom(out as any);
    });

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });

  it('publicOnlyGuard: allow when NOT authenticated', async () => {
    store.clear();

    const result = await TestBed.runInInjectionContext(async () => {
      const out = publicOnlyGuard({} as any, {} as any);
      return firstValueFrom(out as any);
    });

    expect(result).toBeTrue();
  });

  it('publicOnlyGuard: redirect /feed when authenticated', async () => {
    store.setAccessToken('t');

    const result = await TestBed.runInInjectionContext(async () => {
      const out = publicOnlyGuard({} as any, {} as any);
      return firstValueFrom(out as any);
    });

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/feed');
  });
});
