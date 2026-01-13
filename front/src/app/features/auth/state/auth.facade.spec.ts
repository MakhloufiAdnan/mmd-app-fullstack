import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, Subject, throwError } from 'rxjs';

import { AuthFacade } from './auth.facade';
import { AuthStore } from './auth.store';
import { AuthApiService } from '../services/auth-api.service';
import type { TokenResponse } from '../interfaces/auth.models';
import { buildValidSecret } from '@core/testing/test-secrets';

/**
 * Helper top-level pour créer une réponse TokenResponse.
 */
function tokenResponse(accessToken: string): TokenResponse {
  return { accessToken, tokenType: 'Bearer', expiresInSeconds: 900 };
}

describe('AuthFacade', () => {
  let facade: AuthFacade;
  let store: AuthStore;
  let api: jasmine.SpyObj<AuthApiService>;

  beforeEach(() => {
    // Arrange
    api = jasmine.createSpyObj<AuthApiService>('AuthApiService', [
      'csrf',
      'login',
      'register',
      'refresh',
      'logout',
    ]);

    TestBed.configureTestingModule({
      providers: [AuthFacade, AuthStore, { provide: AuthApiService, useValue: api }],
    });

    facade = TestBed.inject(AuthFacade);
    store = TestBed.inject(AuthStore);
  });

  it('login() should call csrf best-effort then set access token in store', (done) => {
    // Arrange
    api.csrf.and.returnValue(of(void 0));
    api.login.and.returnValue(of(tokenResponse('jwt')));

    const secret = buildValidSecret();

    // Act
    facade.login({ identifier: 'bob', password: secret }).subscribe({
      next: (res) => {
        // Assert
        expect(res.accessToken).toBe('jwt');
        expect(api.csrf).toHaveBeenCalled();
        expect(api.login).toHaveBeenCalled();
        expect(store.accessToken()).toBe('jwt');
        done();
      },
      error: done.fail,
    });
  });

  it('logout() should clear token even when API fails (finalize)', () => {
    // Arrange
    store.setAccessToken('jwt');
    api.logout.and.returnValue(throwError(() => new Error('boom')));

    // Act
    facade.logout().subscribe();

    // Assert
    expect(store.accessToken()).toBeNull();
  });

  it('refreshAccessTokenOnce() should deduplicate concurrent refresh calls', (done) => {
    // Arrange
    const refresh$ = new Subject<TokenResponse>();
    api.refresh.and.returnValue(refresh$.asObservable());

    const results: Array<string | null> = [];

    // Act
    facade.refreshAccessTokenOnce().subscribe((v) => results.push(v));
    facade.refreshAccessTokenOnce().subscribe((v) => results.push(v));

    // Assert (1 seul refresh)
    expect(api.refresh).toHaveBeenCalledTimes(1);

    // Act (résolution)
    refresh$.next(tokenResponse('new-jwt'));
    refresh$.complete();

    setTimeout(() => {
      // Assert
      expect(results).toEqual(['new-jwt', 'new-jwt']);
      expect(store.accessToken()).toBe('new-jwt');
      done();
    }, 0);
  });

  it('bootstrap$() should call csrf then refresh, set token, and mark initialized', async () => {
    // Arrange
    api.csrf.and.returnValue(of(void 0));
    api.refresh.and.returnValue(of(tokenResponse('boot-jwt')));

    spyOn(store, 'markInitialized').and.callThrough();

    // Act
    await firstValueFrom(facade.bootstrap$());

    // Assert
    expect(api.csrf).toHaveBeenCalled();
    expect(api.refresh).toHaveBeenCalled();
    expect(store.accessToken()).toBe('boot-jwt');
    expect(store.markInitialized).toHaveBeenCalled();
  });
});
