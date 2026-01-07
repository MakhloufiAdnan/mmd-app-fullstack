import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateCommentRequest,
  CreatePostRequest,
  CreatedIdResponse,
  PostDetailResponse,
} from '../interfaces/post.models';

@Injectable({ providedIn: 'root' })
export class PostsApiService {
  private readonly http = inject(HttpClient);

  createPost(request: CreatePostRequest): Observable<CreatedIdResponse> {
    return this.http.post<CreatedIdResponse>('/api/posts', request);
  }

  getPost(postId: number): Observable<PostDetailResponse> {
    return this.http.get<PostDetailResponse>(`/api/posts/${postId}`);
  }

  addComment(postId: number, content: string): Observable<CreatedIdResponse> {
    const request: CreateCommentRequest = { content };
    return this.http.post<CreatedIdResponse>(`/api/posts/${postId}/comments`, request);
  }
}
