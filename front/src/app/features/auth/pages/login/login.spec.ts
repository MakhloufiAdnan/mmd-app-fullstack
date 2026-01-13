import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Login } from './login';
import { AuthFacade } from '../../state/auth.facade';
import { buildValidSecret } from '@core/testing/test-secrets';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;

  let router: jasmine.SpyObj<Router>;
  let facade: jasmine.SpyObj<AuthFacade>;

  beforeEach(async () => {
    // Arrange (DI mocks)
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.returnValue(Promise.resolve(true) as any);

    facade = jasmine.createSpyObj<AuthFacade>('AuthFacade', ['login']);

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthFacade, useValue: facade },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    // Arrange done in beforeEach

    // Act
    const instance = component;

    // Assert
    expect(instance).toBeTruthy();
  });

  it('submit() should NOT call facade.login when form is invalid', () => {
    // Arrange
    component.form.setValue({ identifier: '', password: '' });

    // Act
    component.submit();

    // Assert
    expect(facade.login).not.toHaveBeenCalled();
  });

  it('submit() should call facade.login then navigate to "/feed" on success', () => {
    // Arrange
    facade.login.and.returnValue(
      of({ accessToken: 'jwt', tokenType: 'Bearer', expiresInSeconds: 900 })
    );

    const secret = buildValidSecret();
    component.form.setValue({ identifier: 'bob', password: secret });

    // Act
    component.submit();

    // Assert
    expect(facade.login).toHaveBeenCalledTimes(1);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/feed');
  });

  it('submit() should map API payload error to globalError (typed API error)', () => {
    // Arrange
    facade.login.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: { error: 'UNAUTHORIZED', message: 'Identifiants invalides' },
          })
      )
    );

    const secret = buildValidSecret();
    component.form.setValue({ identifier: 'bob', password: secret });

    // Act
    component.submit();

    // Assert
    expect(component.globalError()).toBe('Identifiants invalides');
  });

  it('submit() should set generic message when error is not an API error payload', () => {
    // Arrange
    facade.login.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: 'boom' }))
    );

    const secret = buildValidSecret();
    component.form.setValue({ identifier: 'bob', password: secret });

    // Act
    component.submit();

    // Assert
    expect(component.globalError()).toBe('Une erreur est survenue. Réessaie plus tard.');
  });
});
