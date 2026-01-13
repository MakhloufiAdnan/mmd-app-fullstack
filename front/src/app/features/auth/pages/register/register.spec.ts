import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Register } from './register';
import { AuthFacade } from '../../state/auth.facade';
import { buildValidSecret } from '@core/testing/test-secrets';

describe('Register', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;

  let router: jasmine.SpyObj<Router>;
  let facade: jasmine.SpyObj<AuthFacade>;

  beforeEach(async () => {
    // Arrange
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.returnValue(Promise.resolve(true) as any);

    facade = jasmine.createSpyObj<AuthFacade>('AuthFacade', ['register']);

    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthFacade, useValue: facade },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
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

  it('submit() should NOT call facade.register when form is invalid', () => {
    // Arrange
    component.form.setValue({ username: '', email: 'bad', password: '' });

    // Act
    component.submit();

    // Assert
    expect(facade.register).not.toHaveBeenCalled();
  });

  it('submit() should call facade.register then navigate to "/login" on success', () => {
    // Arrange
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
  });

  it('submit() should map API error payload to globalError (typed API error)', () => {
    // Arrange
    facade.register.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { error: 'CONFLICT', message: 'Email déjà utilisé' },
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
  });

  it('submit() should set generic message when error is not an API error payload', () => {
    // Arrange
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
  });
});
