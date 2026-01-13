import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Profile } from './profile';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

/**
 * Helpers en "highest possible scope" (Sonar S7721)
 * => évite des fonctions déclarées à l'intérieur des tests.
 */

type MeDto = {
  id: number;
  email: string;
  username: string;
  subscriptions: Array<{ id: number; name: string }>;
};

type TopicDto = {
  id: number;
  name: string;
  description: string;
  subscribed: boolean;
};

function flushInitRequests(httpMock: HttpTestingController, me: MeDto, topics: TopicDto[]): void {
  const meReq = httpMock.expectOne('/api/users/me');
  expect(meReq.request.method).toBe('GET');
  meReq.flush(me);

  const topicsReq = httpMock.expectOne('/api/topics');
  expect(topicsReq.request.method).toBe('GET');
  topicsReq.flush(topics);
}

function flushReloadMe(httpMock: HttpTestingController, me: MeDto): void {
  const meReq = httpMock.expectOne('/api/users/me');
  expect(meReq.request.method).toBe('GET');
  meReq.flush(me);
}

function flushReloadTopics(httpMock: HttpTestingController, topics: TopicDto[]): void {
  const topicsReq = httpMock.expectOne('/api/topics');
  expect(topicsReq.request.method).toBe('GET');
  topicsReq.flush(topics);
}

function getComponentSnackBar(component: Profile): MatSnackBar {
  // Le champ est "private readonly snackBar" mais reste accessible en runtime (pas de #private)
  return (component as unknown as { snackBar: MatSnackBar }).snackBar;
}

describe('Profile', () => {
  let fixture: ComponentFixture<Profile>;
  let component: Profile;
  let httpMock: HttpTestingController;

  // Spy sur l'instance réellement utilisée dans Profile
  let snackBar: MatSnackBar;
  let openSpy: jasmine.Spy;

  beforeEach(async () => {
    // Arrange : module test
    await TestBed.configureTestingModule({
      imports: [Profile],
      providers: [...DEFAULT_COMPONENT_TEST_PROVIDERS],
    }).compileComponents();

    // Arrange : instanciation
    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    // Arrange : spy sur l'instance snackBar du composant (et pas une autre instance)
    snackBar = getComponentSnackBar(component);
    openSpy = spyOn(snackBar, 'open').and.stub();
  });

  afterEach(() => {
    // Assert : aucune requête HTTP en attente
    httpMock.verify();
  });

  it('should load me + topics on init (success)', () => {
    // Arrange
    const me: MeDto = {
      id: 1,
      email: 'user@mail.com',
      username: 'devUser',
      subscriptions: [{ id: 10, name: 'Java' }],
    };
    const topics: TopicDto[] = [
      { id: 10, name: 'Java', description: 'desc', subscribed: true },
      { id: 11, name: 'Angular', description: 'desc', subscribed: false },
    ];

    // Act : ngOnInit() déclenche loadMe + loadTopics
    fixture.detectChanges();
    flushInitRequests(httpMock, me, topics);
    fixture.detectChanges();

    // Assert
    expect(component.me()?.email).toBe('user@mail.com');
    expect(component.initialMe()?.username).toBe('devUser');
    expect(component.subscribedTopics().map((t) => t.id)).toEqual([10]);

    // Aucun snack attendu sur le happy-path init
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('submit() should PUT update and then reload me (success path)', () => {
    // Arrange : init OK
    fixture.detectChanges();
    flushInitRequests(
      httpMock,
      { id: 1, email: 'user@mail.com', username: 'devUser', subscriptions: [] },
      []
    );
    fixture.detectChanges();

    // Arrange : form valide + modification username
    component.form.patchValue({
      email: 'user@mail.com',
      username: 'newUser',
      password: '', // => payload.password doit être null
    });
    component.form.updateValueAndValidity();
    expect(component.form.valid).toBeTrue();

    // Act : submit
    component.submit();

    // Assert : PUT émis avec payload conforme
    const putReq = httpMock.expectOne('/api/users/me');
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body).toEqual({
      email: 'user@mail.com',
      username: 'newUser',
      password: null,
    });

    // Act : réponse OK du PUT => doit ouvrir snack + relancer loadMe()
    putReq.flush({ updated: true });
    fixture.detectChanges();

    // Assert : snack succès (c'est ce qui échouait chez toi)
    expect(openSpy).toHaveBeenCalledWith('Profil mis à jour.', 'OK', { duration: 2000 });

    // Assert : reload me (GET)
    flushReloadMe(httpMock, {
      id: 1,
      email: 'user@mail.com',
      username: 'newUser',
      subscriptions: [],
    });
    fixture.detectChanges();

    expect(component.me()?.username).toBe('newUser');
  });

  it('unsubscribe() should optimistically update then call DELETE and reload topics on success', () => {
    // Arrange : init OK avec topic subscribed=true
    fixture.detectChanges();
    flushInitRequests(
      httpMock,
      { id: 1, email: 'user@mail.com', username: 'devUser', subscriptions: [] },
      [{ id: 10, name: 'Java', description: 'desc', subscribed: true }]
    );
    fixture.detectChanges();

    // Pré-assert : topic bien abonné
    expect(component.topics()?.[0].subscribed).toBeTrue();

    // Act : unsubscribe
    component.unsubscribe(10);

    // Assert : optimistic update immédiat
    expect(component.topics()?.[0].subscribed).toBeFalse();

    // Assert : DELETE émis
    const delReq = httpMock.expectOne('/api/users/me/subscriptions/10');
    expect(delReq.request.method).toBe('DELETE');

    // Act : succès backend
    delReq.flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();

    // Assert : snack succès (c'est ce qui échouait chez toi)
    expect(openSpy).toHaveBeenCalledWith('Abonnement supprimé.', 'OK', { duration: 2000 });

    // Assert : reload topics (GET) déclenché par onUnsubscribeSuccess()
    flushReloadTopics(httpMock, [{ id: 10, name: 'Java', description: 'desc', subscribed: false }]);
    fixture.detectChanges();

    expect(component.topics()?.[0].subscribed).toBeFalse();
  });

  it('unsubscribe() should rollback optimistic update on ApiErrorResponse', () => {
    // Arrange : init OK avec topic subscribed=true
    fixture.detectChanges();
    flushInitRequests(
      httpMock,
      { id: 1, email: 'user@mail.com', username: 'devUser', subscriptions: [] },
      [{ id: 10, name: 'Java', description: 'desc', subscribed: true }]
    );
    fixture.detectChanges();

    // Act : unsubscribe
    component.unsubscribe(10);

    // Assert : optimistic false
    expect(component.topics()?.[0].subscribed).toBeFalse();

    // Act : backend renvoie erreur contractuelle (ex: déjà désabonné)
    const delReq = httpMock.expectOne('/api/users/me/subscriptions/10');
    delReq.flush(
      { error: 'CONFLICT', message: 'Déjà désabonné', fieldErrors: [] },
      { status: 409, statusText: 'Conflict' }
    );
    fixture.detectChanges();

    // Assert : rollback => repasse à true
    expect(component.topics()?.[0].subscribed).toBeTrue();

    // Assert : snack message serveur (c'est ce qui échouait chez toi)
    expect(openSpy).toHaveBeenCalledWith('Déjà désabonné', 'OK', { duration: 3000 });

    // Assert : pas de reload topics en cas d'erreur
    httpMock.expectNone('/api/topics');
  });

  it('unsubscribe() should ignore call when topicId is already pending (double-click guard)', () => {
    // Arrange : init OK avec topic subscribed=true
    fixture.detectChanges();
    flushInitRequests(
      httpMock,
      { id: 1, email: 'user@mail.com', username: 'devUser', subscriptions: [] },
      [{ id: 10, name: 'Java', description: 'desc', subscribed: true }]
    );
    fixture.detectChanges();

    // Arrange : simule "pending" (double click)
    component.unsubPendingIds.set(new Set([10]));
    expect(component.unsubPendingIds().has(10)).toBeTrue(); // ✅ expectation explicite

    // Act : doit être ignoré
    component.unsubscribe(10);

    // Assert : aucun DELETE
    httpMock.expectNone('/api/users/me/subscriptions/10');

    // Assert : aucun snack
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('loadTopics() should set topicsError and open snackBar on error', () => {
    // Arrange
    fixture.detectChanges();

    // Act : /users/me OK (pour laisser le composant stable)
    const meReq = httpMock.expectOne('/api/users/me');
    meReq.flush({ id: 1, email: 'user@mail.com', username: 'devUser', subscriptions: [] });

    // Act : /topics KO
    const topicsReq = httpMock.expectOne('/api/topics');
    topicsReq.flush({}, { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();

    // Assert : topicsError + snack
    expect(component.topicsError()).toBe('Impossible de charger vos abonnements.');
    expect(openSpy).toHaveBeenCalledWith('Impossible de charger vos abonnements.', 'OK', {
      duration: 3000,
    });
  });
});
