import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { Welcome } from './welcome';

describe('Welcome', () => {
  let component: Welcome;
  let fixture: ComponentFixture<Welcome>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    // Arrange
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.returnValue(Promise.resolve(true) as any);

    await TestBed.configureTestingModule({
      imports: [Welcome],
      providers: [{ provide: Router, useValue: router }],
    }).compileComponents();

    fixture = TestBed.createComponent(Welcome);
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

  it('goLogin() should navigate to "/login"', () => {
    // Arrange
    // (router mock already set)

    // Act
    component.goLogin();

    // Assert
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('goRegister() should navigate to "/register"', () => {
    // Arrange

    // Act
    component.goRegister();

    // Assert
    expect(router.navigateByUrl).toHaveBeenCalledWith('/register');
  });
});
