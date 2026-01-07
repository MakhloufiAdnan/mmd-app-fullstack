import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { UserMeApiService } from './user-me-api.service';

describe('UserMeApiService', () => {
  let api: UserMeApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    api = TestBed.inject(UserMeApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should call GET /api/users/me with credentials', () => {
    const me$ = api.me();
    me$.subscribe();

    const req = httpMock.expectOne('/api/users/me');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush({ id: 1, email: 'a@b.com', username: 'u', subscriptions: [] });
  });

  it('should call PUT /api/users/me with credentials', () => {
    api.updateMe({ username: 'new' }).subscribe();

    const req = httpMock.expectOne('/api/users/me');
    expect(req.request.method).toBe('PUT');
    expect(req.request.withCredentials).toBeTrue();
    expect(req.request.body).toEqual({ username: 'new' });
    req.flush({ updated: true });
  });
});
