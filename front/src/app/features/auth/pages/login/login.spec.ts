import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Login } from './login';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      // Login depends on Router + AuthFacade (which uses HttpClient) + Material.
      providers: [...DEFAULT_COMPONENT_TEST_PROVIDERS],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
