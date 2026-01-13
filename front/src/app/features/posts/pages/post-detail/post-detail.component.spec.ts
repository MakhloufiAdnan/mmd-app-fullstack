import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Location } from '@angular/common';
import { BehaviorSubject, firstValueFrom, of, throwError } from 'rxjs';
import { filter, take } from 'rxjs/operators';

import { PostDetailComponent } from './post-detail.component';
import { PostsApiService } from '@features/posts/services/posts-api.service';
import { CreatedIdResponse, PostDetailResponse } from '@features/posts/interfaces/post.models';

describe('PostDetailComponent (shallow)', () => {
  let component: PostDetailComponent;
  let fixture: ComponentFixture<PostDetailComponent>;

  let postsApi: jasmine.SpyObj<PostsApiService>;
  let router: jasmine.SpyObj<Router>;
  let location: jasmine.SpyObj<Location>;

  // Permet de piloter le paramMap (postId) dans les tests
  let paramMap$: BehaviorSubject<any>;

  beforeEach(async () => {
    // Arrange: mocks DI
    postsApi = jasmine.createSpyObj<PostsApiService>('PostsApiService', ['getPost', 'addComment']);

    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.returnValue(Promise.resolve(true) as any);

    location = jasmine.createSpyObj<Location>('Location', ['back']);

    // Arrange: ActivatedRoute paramMap observable pilotable
    paramMap$ = new BehaviorSubject(convertToParamMap({ postId: '42' }));

    // Arrange: réponse par défaut pour éviter que vm$ plante au montage
    const basePost: PostDetailResponse = {
      id: 42,
      title: 'T',
      content: 'C',
      createdAt: '2026-01-01T10:00:00.000Z',
      author: { id: 1, username: 'bob' } as any,
      topic: { id: 1, name: 'topic' } as any,
      comments: [],
      likesCount: 0,
      liked: false,
    } as any;

    postsApi.getPost.and.returnValue(of(basePost));

    await TestBed.configureTestingModule({
      imports: [PostDetailComponent],
      providers: [
        { provide: PostsApiService, useValue: postsApi },
        { provide: Router, useValue: router },
        { provide: Location, useValue: location },
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
      ],
    })
      // Arrange: Option A => template neutralisé (pas de Material)
      .overrideComponent(PostDetailComponent, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(PostDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    // Arrange: done in beforeEach

    // Act
    const instance = component;

    // Assert
    expect(instance).toBeTruthy();
  });

  it('vm$ should emit loaded post with comments sorted desc by createdAt', async () => {
    // Arrange: commentaires non triés
    const post: PostDetailResponse = {
      id: 42,
      title: 'T',
      content: 'C',
      createdAt: '2026-01-01T10:00:00.000Z',
      author: { id: 1, username: 'bob' } as any,
      topic: { id: 1, name: 'topic' } as any,
      comments: [
        { id: 1, content: 'old', createdAt: '2026-01-01T10:00:00.000Z' } as any,
        { id: 2, content: 'new', createdAt: '2026-01-02T10:00:00.000Z' } as any,
      ],
      likesCount: 0,
      liked: false,
    } as any;

    postsApi.getPost.and.returnValue(of(post));

    // Act: on récupère le 1er VM "loaded" (loading === false)
    const loadedVm = await firstValueFrom(
      component.vm$.pipe(
        filter((vm) => vm.loading === false),
        take(1)
      )
    );

    // Assert: tri décroissant
    expect(loadedVm.post?.comments[0].id).toBe(2);
    expect(loadedVm.post?.comments[1].id).toBe(1);
  });

  it('commentErrorMessage() should return required message when touched + required', () => {
    // Arrange: champ vide + touched
    component.commentForm.setValue({ content: '' });
    component.commentForm.get('content')!.markAsTouched();

    // Act
    const msg = component.commentErrorMessage();

    // Assert
    expect(msg).toBe('Le commentaire est requis.');
  });

  it('commentErrorMessage() should return server message first when present', () => {
    // Arrange: message serveur déjà mappé
    component.fieldErrors.set({ content: ['Server says no'] });

    // Act
    const msg = component.commentErrorMessage();

    // Assert
    expect(msg).toBe('Server says no');
  });

  it('onSubmitComment() should NOT call addComment when form is invalid', () => {
    // Arrange: invalid (required)
    component.commentForm.setValue({ content: '' });

    // Act
    component.onSubmitComment(42);

    // Assert
    expect(postsApi.addComment).not.toHaveBeenCalled();
  });

  it('onSubmitComment() should call addComment, reset form and trigger refresh on success', () => {
    // Arrange:
    // - vm$ n’est pas auto-subscribed (template vide), donc on s’abonne pour activer le pipeline de refresh
    // - getPost doit répondre 2 fois : initial + refresh
    postsApi.getPost.calls.reset();
    const post1 = { id: 42, comments: [], createdAt: '2026-01-01T00:00:00Z' } as any;
    const post2 = { id: 42, comments: [], createdAt: '2026-01-01T00:00:00Z' } as any;
    postsApi.getPost.and.returnValues(of(post1), of(post2));

    const sub = component.vm$.subscribe(); // active le stream

    // Arrange: addComment() est typé Observable<CreatedIdResponse> => on retourne un objet conforme
    postsApi.addComment.and.returnValue(of({ id: 1 } satisfies CreatedIdResponse));

    component.commentForm.setValue({ content: 'Hello' });

    const before = postsApi.getPost.calls.count();

    // Act
    component.onSubmitComment(42);

    // Assert: POST appelé avec le bon payload
    expect(postsApi.addComment).toHaveBeenCalledWith(42, 'Hello');

    // Assert: reset du form
    expect(component.commentForm.getRawValue().content).toBe('');

    // Assert: refresh => nouveau GET
    expect(postsApi.getPost.calls.count()).toBe(before + 1);

    // Assert: finalize
    expect(component.submitting()).toBeFalse();

    sub.unsubscribe();
  });

  it('onSubmitComment() should map typed API error to globalError, fieldErrors and set control "server" error', () => {
    // Arrange: typed API error
    postsApi.addComment.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: {
              error: 'VALIDATION_ERROR',
              message: 'Validation failed',
              fieldErrors: [{ field: 'content', message: 'Too short' }],
            },
          })
      )
    );

    component.commentForm.setValue({ content: 'Hello' });

    // Act
    component.onSubmitComment(42);

    // Assert: global + map
    expect(component.globalError()).toBe('Validation failed');
    expect(component.fieldErrors()).toEqual({ content: ['Too short'] });

    // Assert: projection dans le contrôle
    const ctrl = component.commentForm.get('content')!;
    expect(ctrl.hasError('server')).toBeTrue();
    expect(ctrl.getError('server')).toBe('Too short');
    expect(ctrl.touched).toBeTrue();

    expect(component.submitting()).toBeFalse();
  });

  it('onSubmitComment() should set generic message when error is not a typed API payload', () => {
    // Arrange: payload non conforme
    postsApi.addComment.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: 'boom' }))
    );

    component.commentForm.setValue({ content: 'Hello' });

    // Act
    component.onSubmitComment(42);

    // Assert
    expect(component.globalError()).toBe('Une erreur est survenue. Réessaie plus tard.');
    expect(component.submitting()).toBeFalse();
  });

  it('onSubmitComment() should clear only "server" error on control (keep other errors)', () => {
    // Arrange: ctrl avec required + server
    const ctrl = component.commentForm.get('content')!;
    ctrl.setErrors({ required: true, server: 'Old server error' });

    // Form invalide => return early, mais resetUiErrors() doit enlever "server"
    component.commentForm.setValue({ content: '' });

    // Act
    component.onSubmitComment(42);

    // Assert: server retiré, required conservé
    expect(ctrl.hasError('server')).toBeFalse();
    expect(ctrl.hasError('required')).toBeTrue();
  });
});
