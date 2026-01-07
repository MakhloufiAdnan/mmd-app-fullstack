import { TestBed } from '@angular/core/testing';
import { AuthLayout } from './auth-layout';

describe('AuthLayout', () => {
  it('should create', () => {
    const fixture = TestBed.configureTestingModule({
      imports: [AuthLayout],
    }).createComponent(AuthLayout);

    fixture.componentInstance.title = 'Se connecter';
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
  });
});
