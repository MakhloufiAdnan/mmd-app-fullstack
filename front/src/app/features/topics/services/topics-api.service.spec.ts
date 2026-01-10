import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TopicsApiService } from './topics-api.service';

describe('TopicsApiService', () => {
  let service: TopicsApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(TopicsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listTopics() should GET /api/topics withCredentials=true', () => {
    // Arrange
    // Act
    service.listTopics().subscribe((topics) => {
      // Assert (response)
      expect(topics.length).toBe(1);
      expect(topics[0].id).toBe(10);
    });

    // Assert (request)
    const req = httpMock.expectOne('/api/topics');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();

    req.flush([{ id: 10, name: 'Angular', subscribed: false }]);
  });

  it('subscribeToTopic(topicId) should POST /api/users/me/subscriptions withCredentials=true', () => {
    // Arrange
    const topicId = 42;

    // Act
    service.subscribeToTopic(topicId).subscribe((res) => {
      // Assert (response)
      expect(res.id).toBe(999);
    });

    // Assert (request)
    const req = httpMock.expectOne('/api/users/me/subscriptions');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBeTrue();
    expect(req.request.body).toEqual({ topicId });

    req.flush({ id: 999 });
  });
});
