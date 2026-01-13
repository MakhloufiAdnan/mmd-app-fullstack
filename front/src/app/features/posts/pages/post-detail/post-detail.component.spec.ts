import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { HttpTestingController } from '@angular/common/http/testing';
import { Location } from '@angular/common';

import { PostDetailComponent } from './post-detail.component';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('PostDetailComponent', () => {
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;
  let location: jasmine.SpyObj<Location>;

  beforeEach(async () => {
    // Arrange (DI mocks)
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.returnValue(Promise.resolve(true) as any);

    location = jasmine.createSpyObj<Location>('Location', ['back']);

    await TestBed.configureTestingModule({
      imports: [PostDetailComponent],
      providers: [
        ...DEFAULT_COMPONENT_TEST_PROVIDERS,
        { provide: Router, useValue: router },
        { provide: Location, useValue: location },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ postId: '10' })) },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Assert global : aucune requête HTTP “en attente”
    httpMock.verify();
  });

  /**
   * Helper : répond au GET initial /api/posts/10
   * (évite de dupliquer le flush dans chaque test)
   */
  function flushInitialGet() {
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
  }

  it('should load post detail on init', () => {
    // Arrange
    const fixture = TestBed.createComponent(PostDetailComponent);

    // Act
    fixture.detectChanges();
    flushInitialGet();
    fixture.detectChanges();

    // Assert
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('onSubmitComment() should POST comment then refresh post detail', fakeAsync(() => {
    // Arrange
    const fixture = TestBed.createComponent(PostDetailComponent);
    fixture.detectChanges();
    flushInitialGet();
    fixture.detectChanges();

    // Form valide
    fixture.componentInstance.commentForm.setValue({ content: 'Hello' });

    // Act (submit)
    fixture.componentInstance.onSubmitComment(10);

    // Assert (POST envoyé)
    const postReq = httpMock.expectOne('/api/posts/10/comments');
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body).toEqual({ content: 'Hello' });

    // Act (réponse OK)
    postReq.flush({ id: 200 });

    // Assert (refresh GET déclenché)
    const refreshGet = httpMock.expectOne('/api/posts/10');
    expect(refreshGet.request.method).toBe('GET');

    // Act (retour détaillé avec commentaire)
    refreshGet.flush({
      id: 10,
      topic: { id: 1, name: 'Java' },
      title: 'Mon titre',
      content: 'Mon contenu',
      author: { id: 1, username: 'devUser' },
      createdAt: '2025-12-22T12:00:00Z',
      comments: [
        {
          id: 200,
          content: 'Hello',
          author: { id: 1, username: 'devUser' },
          createdAt: '2025-12-22T13:00:00Z',
        },
      ],
    });

    tick();

    // Assert (form reset)
    expect(fixture.componentInstance.commentForm.getRawValue().content).toBe('');
  }));

  it('onSubmitComment() should map API validation error to fieldErrors and set control "server" error', () => {
    // Arrange
    const fixture = TestBed.createComponent(PostDetailComponent);
    fixture.detectChanges();
    flushInitialGet();
    fixture.detectChanges();

    fixture.componentInstance.commentForm.setValue({ content: 'x' });

    // Act
    fixture.componentInstance.onSubmitComment(10);

    // Assert (POST envoyé)
    const postReq = httpMock.expectOne('/api/posts/10/comments');
    expect(postReq.request.method).toBe('POST');

    // Act (réponse 400 validation)
    postReq.flush(
      {
        error: 'VALIDATION_ERROR',
        message: 'Erreur de validation',
        fieldErrors: [{ field: 'content', message: 'Trop court' }],
      },
      { status: 400, statusText: 'Bad Request' }
    );

    // Assert (mapping UI)
    expect(fixture.componentInstance.globalError()).toBe('Erreur de validation');
    expect(fixture.componentInstance.fieldErrors()['content'][0]).toBe('Trop court');

    const ctrl = fixture.componentInstance.commentForm.get('content');
    expect(ctrl?.hasError('server')).toBeTrue();
    expect(ctrl?.touched).toBeTrue();
  });

  it('goBack() should navigate to "/feed" when history length <= 1', () => {
    // Arrange
    spyOnProperty(globalThis.history, 'length', 'get').and.returnValue(0);

    const fixture = TestBed.createComponent(PostDetailComponent);
    fixture.detectChanges();
    flushInitialGet();
    fixture.detectChanges();

    // Act
    fixture.componentInstance.goBack();

    // Assert
    expect(router.navigateByUrl).toHaveBeenCalledWith('/feed');
    expect(location.back).not.toHaveBeenCalled();
  });

  it('goBack() should call location.back() when history length > 1', () => {
    // Arrange
    spyOnProperty(globalThis.history, 'length', 'get').and.returnValue(2);

    const fixture = TestBed.createComponent(PostDetailComponent);
    fixture.detectChanges();
    flushInitialGet();
    fixture.detectChanges();

    // Act
    fixture.componentInstance.goBack();

    // Assert
    expect(location.back).toHaveBeenCalled();
  });
});
