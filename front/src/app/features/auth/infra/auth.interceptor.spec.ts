import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { AuthStore } from '../state/auth.store';
import { AuthFacade } from '../state/auth.facade';
import { AUTH_REFRESH_ATTEMPTED } from './auth.http-context';

/**
 * Factory top-level :
 * - HttpClient avec interceptor
 * - Router spy
 * - AuthFacade spy pour contrôler refreshAccessTokenOnce() (déterministe)
 */
function setupInterceptorTestBed(refreshResult: string | null) {
  const router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
  router.navigateByUrl.and.resolveTo(true);

  const facade = jasmine.createSpyObj<AuthFacade>('AuthFacade', ['refreshAccessTokenOnce']);
  facade.refreshAccessTokenOnce.and.returnValue(of(refreshResult));

  TestBed.configureTestingModule({
    providers: [
      { provide: Router, useValue: router },
      { provide: AuthFacade, useValue: facade },
      AuthStore,
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
    ],
  });

  const http = TestBed.inject(HttpClient);
  const httpMock = TestBed.inject(HttpTestingController);
  const store = TestBed.inject(AuthStore);

  store.markInitialized();

  return { http, httpMock, store, router, facade };
}

describe('authInterceptor', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
  });

  it('should add Authorization header on protected calls when token exists', () => {
    // Arrange
    const { http, httpMock, store } = setupInterceptorTestBed('newToken');
    store.setAccessToken('token123');

    // Act
    http.get('/api/feed').subscribe();
    const req = httpMock.expectOne('/api/feed');

    // Assert
    expect(req.request.headers.get('Authorization')).toBe('Bearer token123');
    req.flush([]);
  });

  it('should NOT add Authorization header on auth public endpoints (csrf/login/register/refresh)', () => {
    // Arrange
    const { http, httpMock, store } = setupInterceptorTestBed('newToken');
    store.setAccessToken('token123');

    // Act + Assert : csrf
    http.get('/api/auth/csrf').subscribe();
    const csrf = httpMock.expectOne('/api/auth/csrf');
    expect(csrf.request.headers.has('Authorization')).toBeFalse();
    csrf.flush(null);

    // Act + Assert : login
    http.post('/api/auth/login', { identifier: 'x', password: 'y' }).subscribe();
    const login = httpMock.expectOne('/api/auth/login');
    expect(login.request.headers.has('Authorization')).toBeFalse();
    login.flush({ accessToken: 'a', tokenType: 'Bearer', expiresInSeconds: 900 });

    // Act + Assert : register
    http.post('/api/auth/register', { email: 'a@b.com', username: 'u', password: 'p' }).subscribe();
    const reg = httpMock.expectOne('/api/auth/register');
    expect(reg.request.headers.has('Authorization')).toBeFalse();
    reg.flush({ id: 1 });

    // Act + Assert : refresh
    http.post('/api/auth/refresh', {}).subscribe();
    const refresh = httpMock.expectOne('/api/auth/refresh');
    expect(refresh.request.headers.has('Authorization')).toBeFalse();
    refresh.flush({ accessToken: 'a', tokenType: 'Bearer', expiresInSeconds: 900 });
  });

  it('on 401, should refresh once then retry request with new token and mark AUTH_REFRESH_ATTEMPTED', (done) => {
    // Arrange
    const { http, httpMock, store, facade } = setupInterceptorTestBed('newToken');
    store.setAccessToken('expired');

    // Act
    http.get('/api/feed').subscribe({
      next: () => done(),
      error: done.fail,
    });

    // Assert : first request -> 401
    const first = httpMock.expectOne('/api/feed');
    expect(first.request.headers.get('Authorization')).toBe('Bearer expired');
    first.flush({}, { status: 401, statusText: 'Unauthorized' });

    // Assert : refresh appelé
    expect(facade.refreshAccessTokenOnce).toHaveBeenCalledTimes(1);

    // Assert : retry request
    const retry = httpMock.expectOne('/api/feed');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer newToken');
    expect(retry.request.context.get(AUTH_REFRESH_ATTEMPTED)).toBeTrue();

    retry.flush([]);
  });

  it('should NOT refresh if AUTH_REFRESH_ATTEMPTED is already true', (done) => {
    // Arrange
    const { http, httpMock, store, facade } = setupInterceptorTestBed('newToken');
    store.setAccessToken('token');

    const ctx = new HttpContext().set(AUTH_REFRESH_ATTEMPTED, true);

    // Act
    http.get('/api/feed', { context: ctx }).subscribe({
      next: () => done.fail('Expected an error'),
      error: (err) => {
        // Assert
        expect(err.status).toBe(401);
        expect(facade.refreshAccessTokenOnce).not.toHaveBeenCalled();
        done();
      },
    });

    // Assert : single request, no refresh
    const req = httpMock.expectOne('/api/feed');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
  });

  it('should NOT attempt refresh on /api/auth/refresh itself (avoid loop)', (done) => {
    // Arrange
    const { http, httpMock, facade } = setupInterceptorTestBed('newToken');

    // Act
    http.post('/api/auth/refresh', {}).subscribe({
      next: () => done.fail('Expected an error'),
      error: (err) => {
        // Assert
        expect(err.status).toBe(401);
        expect(facade.refreshAccessTokenOnce).not.toHaveBeenCalled();
        done();
      },
    });

    // Assert
    const req = httpMock.expectOne('/api/auth/refresh');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
  });

  it('when refresh returns null, should redirect to /login and propagate error', (done) => {
    // Arrange
    const { http, httpMock, store, router } = setupInterceptorTestBed(null);
    store.setAccessToken('expired');

    // Act
    http.get('/api/feed').subscribe({
      next: () => done.fail('Expected an error'),
      error: (err) => {
        // Assert
        expect(err.status).toBe(401);
        expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
        done();
      },
    });

    // Assert : first request -> 401 triggers refresh (null) then redirect
    const first = httpMock.expectOne('/api/feed');
    first.flush({}, { status: 401, statusText: 'Unauthorized' });
  });
});
