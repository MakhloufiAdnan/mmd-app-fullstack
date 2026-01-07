import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { HttpTestingController } from '@angular/common/http/testing';

import { PostDetailComponent } from './post-detail.component';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('PostDetailComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PostDetailComponent],
      providers: [
        ...DEFAULT_COMPONENT_TEST_PROVIDERS,
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ postId: '10' })) },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should load post detail on init', () => {
    const fixture = TestBed.createComponent(PostDetailComponent);
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/posts/10');
    expect(req.request.method).toBe('GET');

    req.flush({
      id: 10,
      topic: { id: 1, name: 'Java' },
      title: 'Mon titre',
      content: 'Mon contenu',
      author: { id: 1, username: 'devUser' },
      createdAt: '2025-12-22T12:00:00Z',
      comments: [],
    });

    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });
});
