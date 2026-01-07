import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { ROUTER_TEST_PROVIDERS } from './core/testing/test.providers';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      // App template contains <router-outlet>, so Router must be provided.
      providers: [...ROUTER_TEST_PROVIDERS],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });
});
