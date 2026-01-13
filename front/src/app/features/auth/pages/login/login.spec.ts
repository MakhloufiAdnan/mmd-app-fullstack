import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Login } from './login';
import { AuthFacade } from '../../state/auth.facade';
import { buildValidSecret } from '@core/testing/test-secrets';

describe('Login (shallow)', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;

  let router: jasmine.SpyObj<Router>;
  let facade: jasmine.SpyObj<AuthFacade>;

  beforeEach(async () => {
    // Arrange: mocks DI (Router + Facade)
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.returnValue(Promise.resolve(true) as any);

    facade = jasmine.createSpyObj<AuthFacade>('AuthFacade', ['login']);

    // Arrange: TestBed + shallow template (évite les dépendances Angular Material)
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthFacade, useValue: facade },
      ],
    })
      .overrideComponent(Login, { set: { template: '' } })
      .compileComponents();

    // Arrange: instanciation composant
    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    // Arrange: done in beforeEach

    // Act
    const instance = component;

    // Assert
    expect(instance).toBeTruthy();
  });

  it('submit() should NOT call facade.login when form is invalid', () => {
    // Arrange: formulaire invalide
    component.form.setValue({ identifier: '', password: '' });
    const touchSpy = spyOn(component.form, 'markAllAsTouched').and.callThrough();

    // Act
    component.submit();

    // Assert: pas d'appel vers la façade ni de navigation
    expect(facade.login).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();

    // Assert: le formulaire est marqué touched pour afficher les erreurs UI
    expect(touchSpy).toHaveBeenCalled();
  });

  it('submit() should call facade.login then navigate to "/feed" on success', () => {
    // Arrange: succès renvoyé par la façade (émission synchrone)
    facade.login.and.returnValue(
      of({ accessToken: 'jwt', tokenType: 'Bearer', expiresInSeconds: 900 })
    );

    // Arrange: formulaire valide
    const secret = buildValidSecret();
    component.form.setValue({ identifier: 'bob', password: secret });

    // Act
    component.submit();

    // Assert: appel + navigation
    expect(facade.login).toHaveBeenCalledTimes(1);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/feed');

    // Assert: submitting revient à false via finalize()
    expect(component.submitting()).toBeFalse();
  });

  it('submit() should map typed API error payload to globalError and fieldErrors', () => {
    // Arrange: HttpErrorResponse avec payload conforme ApiErrorResponse
    // fieldErrors DOIT être un tableau de {field, message}
    facade.login.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: {
              error: 'UNAUTHORIZED',
              message: 'Identifiants invalides',
              fieldErrors: [{ field: 'identifier', message: 'Bad credentials' }],
            },
          })
      )
    );

    const secret = buildValidSecret();
    component.form.setValue({ identifier: 'bob', password: secret });

    // Act
    component.submit();

    // Assert: globalError mappé
    expect(component.globalError()).toBe('Identifiants invalides');

    // Assert: fieldErrors mappé via toFieldErrorMap()
    expect(component.fieldErrors()).toEqual({ identifier: ['Bad credentials'] });

    // Assert: finalize()
    expect(component.submitting()).toBeFalse();
  });

  it('submit() should set generic message when error is not an API error payload', () => {
    // Arrange: HttpErrorResponse, mais payload non conforme à ApiErrorResponse
    facade.login.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: 'boom' }))
    );

    const secret = buildValidSecret();
    component.form.setValue({ identifier: 'bob', password: secret });

    // Act
    component.submit();

    // Assert: fallback générique (pas de fuite technique)
    expect(component.globalError()).toBe('Une erreur est survenue. Réessaie plus tard.');
    expect(component.submitting()).toBeFalse();
  });

  it('submit() should reset globalError and fieldErrors at the start of submission', () => {
    // Arrange: état d'erreurs existant (ex: submit précédent)
    component.globalError.set('old');
    component.fieldErrors.set({ identifier: ['old'] });

    // Arrange: nouvelle erreur API typée
    facade.login.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: {
              error: 'UNAUTHORIZED',
              message: 'Identifiants invalides',
              fieldErrors: [],
            },
          })
      )
    );

    const secret = buildValidSecret();
    component.form.setValue({ identifier: 'bob', password: secret });

    // Act
    component.submit();

    // Assert: l'ancien message a été remplacé (donc reset + set)
    expect(component.globalError()).toBe('Identifiants invalides');
    expect(component.submitting()).toBeFalse();
  });
});
