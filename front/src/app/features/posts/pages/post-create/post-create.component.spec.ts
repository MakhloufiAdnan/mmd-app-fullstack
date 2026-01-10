import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { PostCreateComponent } from './post-create.component';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('PostCreateComponent', () => {
  let component: PostCreateComponent;
  let fixture: ComponentFixture<PostCreateComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PostCreateComponent],
      providers: [...DEFAULT_COMPONENT_TEST_PROVIDERS],
    }).compileComponents();

    fixture = TestBed.createComponent(PostCreateComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    // Assert : aucune requête HTTP non flush
    httpMock.verify();
  });

  it('should create', () => {
    // Arrange: le constructor appelle loadTopics() -> GET /api/topics
    const req = httpMock.expectOne('/api/topics');

    // Act: on répond au backend mock
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush([]);

    // Assert
    expect(component).toBeTruthy();
  });
});
