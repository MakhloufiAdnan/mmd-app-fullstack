import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthApiService } from './auth-api.service';
import type { LoginRequest, RegisterRequest } from '../interfaces/auth.models';

describe('AuthApiService', () => {
  let service: AuthApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('csrf() should GET /api/auth/csrf withCredentials=true', () => {
    // Arrange
    let completed = false;

    // Act
    service.csrf().subscribe({ complete: () => (completed = true) });

    // Assert
    const req = httpMock.expectOne('/api/auth/csrf');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(null);
    expect(completed).toBeTrue();
  });

  it('register() should POST /api/auth/register withCredentials=true', () => {
    // Arrange
    const payload: RegisterRequest = { email: 'a@b.com', username: 'bob', password: 'Aa1!aaaa' };

    // Act
    service.register(payload).subscribe((res) => {
      // Assert (response)
      expect(res.id).toBe(1);
    });

    // Assert (request)
    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    expect(req.request.withCredentials).toBeTrue();
    req.flush({ id: 1 });
  });

  it('login() should POST /api/auth/login withCredentials=true', () => {
    // Arrange
    const payload: LoginRequest = { identifier: 'bob', password: 'Aa1!aaaa' };

    // Act
    service.login(payload).subscribe((res) => {
      // Assert (response)
      expect(res.accessToken).toBe('token');
    });

    // Assert (request)
    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    expect(req.request.withCredentials).toBeTrue();
    req.flush({ accessToken: 'token' });
  });

  it('refresh() should POST /api/auth/refresh withCredentials=true and empty body', () => {
    // Arrange
    // Act
    service.refresh().subscribe((res) => {
      // Assert
      expect(res.accessToken).toBe('new-token');
    });

    // Assert
    const req = httpMock.expectOne('/api/auth/refresh');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    expect(req.request.withCredentials).toBeTrue();
    req.flush({ accessToken: 'new-token' });
  });

  it('logout() should POST /api/auth/logout withCredentials=true and empty body', () => {
    // Arrange
    let completed = false;

    // Act
    service.logout().subscribe({ complete: () => (completed = true) });

    // Assert
    const req = httpMock.expectOne('/api/auth/logout');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    expect(req.request.withCredentials).toBeTrue();
    req.flush(null);
    expect(completed).toBeTrue();
  });
});
