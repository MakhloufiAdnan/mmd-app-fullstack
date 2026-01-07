import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { Feed } from './feed';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('Feed', () => {
  let component: Feed;
  let fixture: ComponentFixture<Feed>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Feed],
      // Feed injects Router/ActivatedRoute and loads data via FeedApiService (HttpClient).
      providers: [...DEFAULT_COMPONENT_TEST_PROVIDERS],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(Feed);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    // Ensures the component doesn't leave pending HTTP requests between tests.
    httpMock.verify();
  });

  it('should create', () => {
    // Feed loads the list on init (default order = "desc").
    const req = httpMock.expectOne(
      (r) => r.url === '/api/feed' && r.params.get('order') === 'desc'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush([]);

    expect(component).toBeTruthy();
  });
});
