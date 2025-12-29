import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { authInterceptor } from './auth.interceptor';
import { AuthStore } from './auth.store';

describe('Auth Interceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let store: AuthStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(AuthStore);

    // Assure un état stable en tests
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

    http.get('/api/feed').subscribe({
      next: () => {},
    });

    // 1ère tentative => 401
    const first = httpMock.expectOne('/api/feed');
    first.flush({}, { status: 401, statusText: 'Unauthorized' });

    // Refresh
    const refresh = httpMock.expectOne('/api/auth/refresh');
    expect(refresh.request.method).toBe('POST');
    expect(refresh.request.withCredentials).toBeTrue();
    refresh.flush({ accessToken: 'newToken', tokenType: 'Bearer', expiresInSeconds: 900 });

    // Retry => OK avec nouveau Bearer
    const retry = httpMock.expectOne('/api/feed');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer newToken');
    retry.flush([]);
  });
});
