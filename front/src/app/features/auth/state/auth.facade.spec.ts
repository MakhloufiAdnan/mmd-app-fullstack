import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import { AuthFacade } from './auth.facade';
import { AuthStore } from './auth.store';
import { AuthApiService } from '../services/auth-api.service';
import type { TokenResponse } from '../interfaces/auth.models';

describe('AuthFacade', () => {
  let facade: AuthFacade;
  let store: AuthStore;

  let api: jasmine.SpyObj<AuthApiService>;

  // Helper : réponse token conforme au contrat TypeScript
  const tokenResponse = (accessToken: string): TokenResponse => ({
    accessToken,
    tokenType: 'Bearer',
    expiresInSeconds: 900, // valeur arbitraire ok pour un test
  });

  beforeEach(() => {
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

    // Act
    facade.login({ identifier: 'bob', password: 'Aa1!aaaa' }).subscribe({
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

    // Act (throwError + catchError(of()) => completions sync)
    facade.logout().subscribe();

    // Assert (finalize a déjà tourné après la fin de subscribe)
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

    // Assert (only 1 refresh call)
    expect(api.refresh).toHaveBeenCalledTimes(1);

    // Finish refresh
    refresh$.next(tokenResponse('new-jwt'));
    refresh$.complete();

    setTimeout(() => {
      // Assert
      expect(results).toEqual(['new-jwt', 'new-jwt']);
      expect(store.accessToken()).toBe('new-jwt');
      done();
    }, 0);
  });
});
