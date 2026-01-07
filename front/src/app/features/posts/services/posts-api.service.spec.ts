import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { PostsApiService } from './posts-api.service';
import { CreatePostRequest } from '../interfaces/post.models';

describe('PostsApiService', () => {
  let service: PostsApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(PostsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('createPost should POST /api/posts', () => {
    const body: CreatePostRequest = { topicId: 1, title: 'T', content: 'C' };

    service.createPost(body).subscribe((res) => {
      expect(res.id).toBe(10);
    });

    const req = httpMock.expectOne('/api/posts');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 10 });
  });

  it('getPost should GET /api/posts/{postId}', () => {
    service.getPost(10).subscribe((res) => {
      expect(res.id).toBe(10);
      expect(res.comments).toEqual([]);
    });

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
  });

  it('addComment should POST /api/posts/{postId}/comments', () => {
    service.addComment(10, 'Mon commentaire').subscribe((res) => {
      expect(res.id).toBe(200);
    });

    const req = httpMock.expectOne('/api/posts/10/comments');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ content: 'Mon commentaire' });
    req.flush({ id: 200 });
  });
});
