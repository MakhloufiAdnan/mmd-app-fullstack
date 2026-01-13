import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Register } from './register';
import { AuthFacade } from '../../state/auth.facade';
import { buildValidSecret } from '@core/testing/test-secrets';

describe('Register (shallow)', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;

  let router: jasmine.SpyObj<Router>;
  let facade: jasmine.SpyObj<AuthFacade>;

  beforeEach(async () => {
    // Arrange: mocks DI
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.returnValue(Promise.resolve(true) as any);

    facade = jasmine.createSpyObj<AuthFacade>('AuthFacade', ['register']);

    // Arrange: TestBed + shallow template (pas de compilation Material)
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthFacade, useValue: facade },
      ],
    })
      .overrideComponent(Register, { set: { template: '' } })
      .compileComponents();

    // Arrange: instanciation composant
    fixture = TestBed.createComponent(Register);
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

  it('submit() should NOT call facade.register when form is invalid', () => {
    // Arrange: formulaire invalide
    component.form.setValue({ username: '', email: 'bad', password: '' });
    const touchSpy = spyOn(component.form, 'markAllAsTouched').and.callThrough();

    // Act
    component.submit();

    // Assert
    expect(facade.register).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(touchSpy).toHaveBeenCalled();
  });

  it('submit() should call facade.register then navigate to "/login" on success', () => {
    // Arrange: succès
    facade.register.and.returnValue(of({ id: 1 }));

    const secret = buildValidSecret();
    component.form.setValue({
      username: 'bob',
      email: 'bob@mail.com',
      password: secret,
    });

    // Act
    component.submit();

    // Assert
    expect(facade.register).toHaveBeenCalledTimes(1);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    expect(component.submitting()).toBeFalse();
  });

  it('submit() should map typed API error payload to globalError and fieldErrors', () => {
    // Arrange: erreur API typée (CONFLICT) + fieldErrors conforme au contrat
    facade.register.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: {
              error: 'CONFLICT',
              message: 'Email déjà utilisé',
              fieldErrors: [{ field: 'email', message: 'Email déjà utilisé' }],
            },
          })
      )
    );

    const secret = buildValidSecret();
    component.form.setValue({
      username: 'bob',
      email: 'bob@mail.com',
      password: secret,
    });

    // Act
    component.submit();

    // Assert
    expect(component.globalError()).toBe('Email déjà utilisé');
    expect(component.fieldErrors()).toEqual({ email: ['Email déjà utilisé'] });
    expect(component.submitting()).toBeFalse();
  });

  it('submit() should set generic message when error is not an API error payload', () => {
    // Arrange: payload non conforme
    facade.register.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: 'boom' }))
    );

    const secret = buildValidSecret();
    component.form.setValue({
      username: 'bob',
      email: 'bob@mail.com',
      password: secret,
    });

    // Act
    component.submit();

    // Assert
    expect(component.globalError()).toBe('Une erreur est survenue. Réessaie plus tard.');
    expect(component.submitting()).toBeFalse();
  });

  it('submit() should reset globalError and fieldErrors at the start of submission', () => {
    // Arrange: état d'erreur existant
    component.globalError.set('old');
    component.fieldErrors.set({ email: ['old'] });

    // Arrange: nouvelle erreur API typée
    facade.register.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: {
              error: 'CONFLICT',
              message: 'Email déjà utilisé',
              fieldErrors: [],
            },
          })
      )
    );

    const secret = buildValidSecret();
    component.form.setValue({
      username: 'bob',
      email: 'bob@mail.com',
      password: secret,
    });

    // Act
    component.submit();

    // Assert: ancien état remplacé (reset + set)
    expect(component.globalError()).toBe('Email déjà utilisé');
    expect(component.submitting()).toBeFalse();
  });
});
