import { Location } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthLayout } from './auth-layout';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

/**
 * Objectif couverture :
 * - Couvrir les 3 chemins de `onBack()` :
 *   1) non-browser => navigateByUrl('/')
 *   2) browser + referrer same-origin => location.back()
 *   3) browser + referrer empty / cross-origin => navigateByUrl('/')
 * - Exécuter aussi les callbacks `.catch(() => undefined)` (fonctions comptées par lcov)
 */
describe('AuthLayout', () => {
  let fixture: ComponentFixture<AuthLayout>;
  let component: AuthLayout;

  let router: jasmine.SpyObj<Router>;
  let location: jasmine.SpyObj<Location>;

  beforeEach(async () => {
    // Arrange (commun) : autorise re-spy si besoin dans ce fichier
    jasmine.getEnv().allowRespy(true);

    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    location = jasmine.createSpyObj<Location>('Location', ['back']);

    await TestBed.configureTestingModule({
      imports: [AuthLayout],
      providers: [
        ...DEFAULT_COMPONENT_TEST_PROVIDERS,

        // Arrange : on injecte des doubles pour observer les effets
        { provide: Router, useValue: router },
        { provide: Location, useValue: location },

        // Par défaut : environnement browser
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AuthLayout);
    component = fixture.componentInstance;

    // Arrange : input requis par le composant
    component.title = 'Se connecter';

    fixture.detectChanges();
  });

  it('should create', () => {
    // Assert
    expect(component).toBeTruthy();
  });

  it('onBack() should navigate to "/" when platform is NOT browser (SSR/Server)', fakeAsync(() => {
    // Arrange : recrée le composant avec PLATFORM_ID = "server"
    TestBed.resetTestingModule();

    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    location = jasmine.createSpyObj<Location>('Location', ['back']);

    TestBed.configureTestingModule({
      imports: [AuthLayout],
      providers: [
        ...DEFAULT_COMPONENT_TEST_PROVIDERS,
        { provide: Router, useValue: router },
        { provide: Location, useValue: location },
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    });

    const localFixture = TestBed.createComponent(AuthLayout);
    const localComponent = localFixture.componentInstance;

    localComponent.title = 'Se connecter';
    localFixture.detectChanges();

    // Arrange : on force navigateByUrl à rejeter pour exécuter le `.catch(() => undefined)`
    router.navigateByUrl.and.returnValue(Promise.reject(new Error('nav failed')));

    // Act
    localComponent.onBack();
    flushMicrotasks();

    // Assert : fallback SSR => navigation vers "/"
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
    expect(location.back).not.toHaveBeenCalled();
  }));

  it('onBack() should call location.back() when referrer is same-origin', () => {
    // Arrange
    const origin = globalThis.location.origin;
    spyOnProperty(document, 'referrer', 'get').and.returnValue(`${origin}/feed`);

    // Act
    component.onBack();

    // Assert
    expect(location.back).toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('onBack() should navigate to "/" when referrer is empty or NOT same-origin (safe fallback)', fakeAsync(() => {
    // Arrange : referrer cross-origin
    spyOnProperty(document, 'referrer', 'get').and.returnValue('https://evil.example/anything');

    // Arrange : on force navigateByUrl à rejeter pour exécuter le `.catch(() => undefined)`
    router.navigateByUrl.and.returnValue(Promise.reject(new Error('nav failed')));

    // Act
    component.onBack();
    flushMicrotasks();

    // Assert
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
    expect(location.back).not.toHaveBeenCalled();
  }));
});
