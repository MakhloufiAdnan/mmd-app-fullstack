import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { AuthShell } from './auth-shell';
import { AuthFacade } from '@features/auth/state/auth.facade';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('AuthShell', () => {
  let fixture: ComponentFixture<AuthShell>;
  let component: AuthShell;

  const authMock = {
    logout: jasmine.createSpy('logout').and.returnValue(of(void 0)),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthShell],
      providers: [
        ...DEFAULT_COMPONENT_TEST_PROVIDERS,
        { provide: AuthFacade, useValue: authMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AuthShell);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    // Arrange / Act / Assert
    expect(component).toBeTruthy();
  });

  it('logout(drawer) should call AuthFacade.logout, close drawer if opened, then navigate to "/"', async () => {
    // Arrange
    const router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);

    const drawer = {
      opened: true,
      close: jasmine.createSpy('close').and.resolveTo(void 0),
    };

    // Act
    await component.logout(drawer as any);

    // Assert
    expect(authMock.logout).toHaveBeenCalled();
    expect(drawer.close).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('logout(drawer) should NOT close drawer if already closed, but still navigate to "/"', async () => {
    // Arrange
    const router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);

    const drawer = {
      opened: false,
      close: jasmine.createSpy('close').and.resolveTo(void 0),
    };

    // Act
    await component.logout(drawer as any);

    // Assert
    expect(authMock.logout).toHaveBeenCalled();
    expect(drawer.close).not.toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('closeDrawer(drawer) should close drawer only when opened', async () => {
    // Arrange
    const drawerOpened = {
      opened: true,
      close: jasmine.createSpy('close').and.resolveTo(void 0),
    };

    const drawerClosed = {
      opened: false,
      close: jasmine.createSpy('close').and.resolveTo(void 0),
    };

    // Act
    await component.closeDrawer(drawerOpened as any);
    await component.closeDrawer(drawerClosed as any);

    // Assert
    expect(drawerOpened.close).toHaveBeenCalled();
    expect(drawerClosed.close).not.toHaveBeenCalled();
  });
});
