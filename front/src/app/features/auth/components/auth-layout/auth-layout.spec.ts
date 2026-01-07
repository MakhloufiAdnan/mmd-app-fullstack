import { TestBed } from '@angular/core/testing';
import { AuthLayout } from './auth-layout';
import {
  ROUTER_TEST_PROVIDERS,
} from '@core/testing/test.providers';

describe('AuthLayout', () => {
  it('should create', () => {
    const fixture = TestBed.configureTestingModule({
      imports: [AuthLayout],
      // AuthLayout injects Router and renders Angular Material buttons/icons.
      providers: [...ROUTER_TEST_PROVIDERS],
    }).createComponent(AuthLayout);

    fixture.componentInstance.title = 'Se connecter';
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
  });
});
