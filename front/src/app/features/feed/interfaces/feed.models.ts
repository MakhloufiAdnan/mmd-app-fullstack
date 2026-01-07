export type FeedOrder = 'asc' | 'desc';

export interface FeedItem {
  id: number;
  topic: { id: number; name: string };
  title: string;
  author: { id: number; username: string };
  createdAt: string;        // ISO string
  commentsCount: number;
}
