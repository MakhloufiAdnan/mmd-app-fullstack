import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthStore } from '../state/auth.store';
import { ROUTER_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('Auth Interceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let store: AuthStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        // Some interceptor code may use Router for redirects; keep it available in tests.
        ...ROUTER_TEST_PROVIDERS,
        // HttpClient configured with the interceptor under test.
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(AuthStore);

    // Guards/interceptors in the app assume auth store has been initialized.
    store.markInitialized();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adds Authorization header on protected calls', () => {
    store.setAccessToken('token123');

    http.get('/api/feed').subscribe();
    const req = httpMock.expectOne('/api/feed');

    expect(req.request.headers.get('Authorization')).toBe('Bearer token123');
    req.flush([]);
  });

  it('on 401: refresh once then retry request', () => {
    store.setAccessToken('expired');

    http.get('/api/feed').subscribe({ next: () => {} });

    // First call -> 401
    const first = httpMock.expectOne('/api/feed');
    first.flush({}, { status: 401, statusText: 'Unauthorized' });

    // Refresh should be triggered once.
    const refresh = httpMock.expectOne('/api/auth/refresh');
    expect(refresh.request.method).toBe('POST');
    expect(refresh.request.withCredentials).toBeTrue();
    refresh.flush({ accessToken: 'newToken', tokenType: 'Bearer', expiresInSeconds: 900 });

    // Original request should be retried with the new token.
    const retry = httpMock.expectOne('/api/feed');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer newToken');
    retry.flush([]);
  });
});
