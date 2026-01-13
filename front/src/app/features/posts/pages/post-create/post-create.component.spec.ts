import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { PostCreateComponent } from './post-create.component';
import { TopicsApiService } from '../../../topics/services/topics-api.service';
import { PostsApiService } from '../../services/posts-api.service';
import { TopicListItem } from '../../../topics/interfaces/topic.models';

describe('PostCreateComponent (shallow)', () => {
  let component: PostCreateComponent;
  let fixture: ComponentFixture<PostCreateComponent>;

  let router: jasmine.SpyObj<Router>;
  let topicsApi: jasmine.SpyObj<TopicsApiService>;
  let postsApi: jasmine.SpyObj<PostsApiService>;

  beforeEach(async () => {
    // Arrange: mocks DI
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.returnValue(Promise.resolve(true) as any);

    topicsApi = jasmine.createSpyObj<TopicsApiService>('TopicsApiService', ['listTopics']);
    postsApi = jasmine.createSpyObj<PostsApiService>('PostsApiService', ['createPost']);

    // Arrange: le constructor() du composant appelle loadTopics()
    // On donne une valeur par défaut pour éviter un crash au montage.
    topicsApi.listTopics.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [PostCreateComponent],
      providers: [
        { provide: Router, useValue: router },
        { provide: TopicsApiService, useValue: topicsApi },
        { provide: PostsApiService, useValue: postsApi },
      ],
    })
      // Arrange: Option A => shallow test, on neutralise le template Material
      .overrideComponent(PostCreateComponent, { set: { template: '' } })
      .compileComponents();

    // Arrange: création du composant
    fixture = TestBed.createComponent(PostCreateComponent);
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

  it('loadTopics() should store topics and stop loading on success', () => {
    // Arrange
    topicsApi.listTopics.calls.reset();
    const topics: TopicListItem[] = [
      { id: 1, name: 'A', subscribed: true } as any,
      { id: 2, name: 'B', subscribed: false } as any,
    ];
    topicsApi.listTopics.and.returnValue(of(topics));

    // Act
    component.loadTopics();

    // Assert: état observable mis à jour
    expect(component.error$.getValue()).toBeNull();
    expect(component.topics$.getValue()).toEqual(topics);
    expect(component.loading$.getValue()).toBeFalse();
  });

  it('loadTopics() should set error and empty topics on failure', () => {
    // Arrange
    topicsApi.listTopics.calls.reset();
    topicsApi.listTopics.and.returnValue(throwError(() => new Error('boom')));

    // Act
    component.loadTopics();

    // Assert: fallback UI prévu par le composant
    expect(component.error$.getValue()).toBe('Impossible de charger les thèmes.');
    expect(component.topics$.getValue()).toEqual([]);
    expect(component.loading$.getValue()).toBeFalse();
  });

  it('hasAnySubscribedTopics() should return true when at least one topic is subscribed', () => {
    // Arrange
    const topics: TopicListItem[] = [
      { id: 1, name: 'A', subscribed: false } as any,
      { id: 2, name: 'B', subscribed: true } as any,
    ];

    // Act
    const result = component.hasAnySubscribedTopics(topics);

    // Assert
    expect(result).toBeTrue();
  });

  it('getFieldErrorFirst() should return first server message for a field', () => {
    // Arrange: fieldErrors = signal<Record<string,string[]>>
    component.fieldErrors.set({ title: ['Too short', 'Other'], topicId: ['Required'] });

    // Act
    const msg = component.getFieldErrorFirst('title');

    // Assert
    expect(msg).toBe('Too short');
  });

  it('onSubmit() should NOT call createPost when form is invalid', () => {
    // Arrange: invalid (Validators.required)
    component.form.setValue({ topicId: null, title: '', content: '' });
    const touchSpy = spyOn(component.form, 'markAllAsTouched').and.callThrough();

    // Act
    component.onSubmit();

    // Assert: aucune requête + feedback UI (touched)
    expect(postsApi.createPost).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(touchSpy).toHaveBeenCalled();
  });

  it('onSubmit() should NOT call createPost when already submitting', () => {
    // Arrange: "double submit" protégé par submitting()
    component.submitting.set(true);
    component.form.setValue({ topicId: 1, title: 't', content: 'c' });

    const touchSpy = spyOn(component.form, 'markAllAsTouched').and.callThrough();

    // Act
    component.onSubmit();

    // Assert: pas de POST
    expect(postsApi.createPost).not.toHaveBeenCalled();
    // Ton code marque touched même si submitting() true (comportement assumé)
    expect(touchSpy).toHaveBeenCalled();
  });

  it('onSubmit() should call createPost then navigate to "/posts/:id" on success', () => {
    // Arrange
    postsApi.createPost.and.returnValue(of({ id: 123 } as any));
    component.form.setValue({ topicId: 10, title: 'Hello', content: 'World' });

    // Act
    component.onSubmit();

    // Assert: POST + navigation
    expect(postsApi.createPost).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['/posts', 123]);

    // Assert: finalize() doit remettre submitting à false (of => synchro)
    expect(component.submitting()).toBeFalse();
  });

  it('onSubmit() should map typed API error to globalError, fieldErrors and set control "server" errors', () => {
    // Arrange: typed API error (ApiErrorResponse + ApiFieldError[])
    postsApi.createPost.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: {
              error: 'VALIDATION_ERROR',
              message: 'Validation failed',
              fieldErrors: [
                { field: 'title', message: 'Title is required' },
                { field: 'content', message: 'Content is required' },
              ],
            },
          })
      )
    );

    // Form valide pour que la requête parte
    component.form.setValue({ topicId: 1, title: 'x', content: 'y' });

    // Act
    component.onSubmit();

    // Assert: globalError
    expect(component.globalError()).toBe('Validation failed');

    // Assert: fieldErrors map (via toFieldErrorMap)
    expect(component.fieldErrors()).toEqual({
      title: ['Title is required'],
      content: ['Content is required'],
    });

    // Assert: projection dans les controls (erreur "server" + touched)
    const titleCtrl = component.form.get('title')!;
    expect(titleCtrl.hasError('server')).toBeTrue();
    expect(titleCtrl.getError('server')).toBe('Title is required');
    expect(titleCtrl.touched).toBeTrue();

    const contentCtrl = component.form.get('content')!;
    expect(contentCtrl.hasError('server')).toBeTrue();
    expect(contentCtrl.touched).toBeTrue();

    // Assert: finalize()
    expect(component.submitting()).toBeFalse();
  });

  it('onSubmit() should set generic message when error is not a typed API payload', () => {
    // Arrange: payload non conforme (string au lieu d’objet ApiErrorResponse)
    postsApi.createPost.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: 'boom' }))
    );

    component.form.setValue({ topicId: 1, title: 'Hello', content: 'World' });

    // Act
    component.onSubmit();

    // Assert: fallback générique
    expect(component.globalError()).toBe('Une erreur est survenue. Réessaie plus tard.');
    expect(component.submitting()).toBeFalse();
  });

  it('onSubmit() should clear only "server" errors on controls (keep other errors)', () => {
    // Arrange: on simule un contrôle avec deux erreurs
    const titleCtrl = component.form.get('title')!;
    titleCtrl.setErrors({ required: true, server: 'Old server error' });

    // Force invalid => le submit return early, mais resetUiErrors() doit enlever "server"
    component.form.setValue({ topicId: null, title: '', content: '' });

    // Act
    component.onSubmit();

    // Assert: server retiré, required conservé
    expect(titleCtrl.hasError('server')).toBeFalse();
    expect(titleCtrl.hasError('required')).toBeTrue();
  });
});
