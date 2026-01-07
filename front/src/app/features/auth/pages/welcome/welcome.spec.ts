import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Welcome } from './welcome';
import { ROUTER_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('Welcome', () => {
  let component: Welcome;
  let fixture: ComponentFixture<Welcome>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Welcome],
      // Welcome injects Router to navigate on button clicks.
      providers: [...ROUTER_TEST_PROVIDERS],
    }).compileComponents();

    fixture = TestBed.createComponent(Welcome);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
