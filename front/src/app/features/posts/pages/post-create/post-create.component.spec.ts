import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { PostCreateComponent } from './post-create.component';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

type FlushBody = Parameters<TestRequest['flush']>[0];

/**
 * Helper top-level :
 * flush la requête GET /api/topics issue du constructor().
 */
function flushTopics(
  httpMock: HttpTestingController,
  body: FlushBody,
  status?: { code: number; text: string }
): void {
  const req = httpMock.expectOne('/api/topics');
  expect(req.request.method).toBe('GET');

  if (status) {
    req.flush(body, { status: status.code, statusText: status.text });
  } else {
    req.flush(body);
  }
}

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

    // Assert : flush obligatoire sinon requête pending
    flushTopics(httpMock, []);
  });

  afterEach(() => {
    // Assert global : aucune requête HTTP ne doit rester pending
    httpMock.verify();
  });

  it('should create', () => {
    // Arrange done in beforeEach

    // Act
    const instance = component;

    // Assert
    expect(instance).toBeTruthy();
  });

  it('onSubmit() should NOT POST when form is invalid (guard + markAllAsTouched)', () => {
    // Arrange : form invalide (required vides)
    component.form.reset();
    component.form.updateValueAndValidity();
    expect(component.form.valid).toBeFalse();

    // Act
    component.onSubmit();

    // Assert : aucun POST + aucune navigation
    httpMock.expectNone('/api/posts');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('onSubmit() should POST then navigate to /posts/:id on success', () => {
    // Arrange : rendre le form valide
    component.form.patchValue({ topicId: 1, title: 'Titre', content: 'Contenu' });
    component.form.updateValueAndValidity();
    expect(component.form.valid).toBeTrue();

    // Act
    component.onSubmit();

    // Assert : POST émis
    const req = httpMock.expectOne('/api/posts');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ topicId: 1, title: 'Titre', content: 'Contenu' });

    // Act : réponse OK
    req.flush({ id: 42 });

    // Assert : navigation vers le détail
    expect(router.navigate).toHaveBeenCalledWith(['/posts', 42]);
  });

  it('onSubmit() should project VALIDATION_ERROR into fieldErrors + control server error', () => {
    // Arrange : form valide
    component.form.patchValue({ topicId: 1, title: 't', content: 'c' });
    component.form.updateValueAndValidity();
    expect(component.form.valid).toBeTrue();

    // Act
    component.onSubmit();

    // Assert : POST émis
    const req = httpMock.expectOne('/api/posts');
    expect(req.request.method).toBe('POST');

    // Act : réponse 400 contractuelle
    req.flush(
      {
        error: 'VALIDATION_ERROR',
        message: 'Validation error',
        fieldErrors: [{ field: 'title', message: 'Title is required' }],
      },
      { status: 400, statusText: 'Bad Request' }
    );

    // Assert : mapping UI
    expect(component.globalError()).toBe('Validation error');
    expect(component.fieldErrors()['title']?.[0]).toBe('Title is required');

    const titleCtrl = component.form.get('title');
    expect(titleCtrl?.errors?.['server']).toBe('Title is required');

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('onSubmit() should set generic error when API payload is not standard', () => {
    // Arrange : form valide
    component.form.patchValue({ topicId: 1, title: 'Titre', content: 'Contenu' });
    component.form.updateValueAndValidity();
    expect(component.form.valid).toBeTrue();

    // Act
    component.onSubmit();

    // Assert : POST émis
    const req = httpMock.expectOne('/api/posts');
    expect(req.request.method).toBe('POST');

    // Act : réponse 500 non contractuelle (payload non ApiErrorResponse)
    req.flush('boom', { status: 500, statusText: 'Server Error' });

    // Assert : message générique (pas de fuite technique)
    expect(component.globalError()).toBe('Une erreur est survenue. Réessaie plus tard.');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});

describe('PostCreateComponent (loadTopics error branch)', () => {
  it('loadTopics() should set error$ and topics$ to [] when /api/topics fails', async () => {
    // Arrange
    await TestBed.configureTestingModule({
      imports: [PostCreateComponent],
      providers: [...DEFAULT_COMPONENT_TEST_PROVIDERS],
    }).compileComponents();

    const fixture = TestBed.createComponent(PostCreateComponent);
    const component = fixture.componentInstance;
    const httpMock = TestBed.inject(HttpTestingController);

    // Act : constructor() -> loadTopics() -> simulate error
    fixture.detectChanges();
    flushTopics(httpMock, {}, { code: 500, text: 'Server Error' });

    // Assert : fallback UX attendu
    expect(component.error$.value).toBe('Impossible de charger les thèmes.');
    expect(component.topics$.value).toEqual([]);
    expect(component.loading$.value).toBeFalse();

    httpMock.verify();
  });
});
