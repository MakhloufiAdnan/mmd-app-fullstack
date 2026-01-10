import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { PostCreateComponent } from './post-create.component';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('PostCreateComponent', () => {
  let component: PostCreateComponent;
  let fixture: ComponentFixture<PostCreateComponent>;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PostCreateComponent],
      providers: [...DEFAULT_COMPONENT_TEST_PROVIDERS],
    }).compileComponents();

    // Arrange
    fixture = TestBed.createComponent(PostCreateComponent);
    component = fixture.componentInstance;

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);

    spyOn(router, 'navigate').and.resolveTo(true);

    // Act : constructor() => loadTopics() => GET /api/topics
    fixture.detectChanges();

    // IMPORTANT : flush du GET /api/topics sinon requête "pending"
    const topicsReq = httpMock.expectOne('/api/topics');
    expect(topicsReq.request.method).toBe('GET');
    topicsReq.flush([]);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('onSubmit() should NOT POST when form is invalid', () => {
    // Arrange : required vides => invalid
    component.form.reset();
    component.form.updateValueAndValidity();
    expect(component.form.valid).toBeFalse();

    // Act
    component.onSubmit();

    // Assert
    httpMock.expectNone('/api/posts');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('onSubmit() should project VALIDATION_ERROR into fieldErrors + form control server error', () => {
    // Arrange : rendre le form valide
    component.form.patchValue({
      topicId: 1,
      title: 't',
      content: 'c',
    });
    component.form.updateValueAndValidity();
    expect(component.form.valid).toBeTrue();

    // Act
    component.onSubmit();

    // Assert : POST émis
    const createReq = httpMock.expectOne('/api/posts');
    expect(createReq.request.method).toBe('POST');

    // Act : réponse 400 contractuelle
    // ✅ On met field + fieldName pour matcher ton mapping quelle que soit l'implémentation exacte du core
    createReq.flush(
      {
        error: 'VALIDATION_ERROR',
        message: 'Validation error',
        fieldErrors: [
          { field: 'title', fieldName: 'title', message: 'Title is required' },
        ],
      },
      { status: 400, statusText: 'Bad Request' }
    );

    // Assert 1 : preuve que handleApiError a reconnu le contrat
    expect(component.globalError()).toBe('Validation error');

    const map = component.fieldErrors();
    expect(map['title']?.[0]).toBe('Title is required');

    // Assert 2 : projection dans le control via applyServerErrorsToControls()
    const titleCtrl = component.form.get('title');
    expect(titleCtrl?.errors?.['server']).toBe('Title is required');

    // Et pas de navigation en erreur
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
