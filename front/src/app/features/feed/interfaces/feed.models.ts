export type FeedOrder = 'asc' | 'desc';

export interface FeedItem {
  id: number;
  topic: { id: number; name: string };
  title: string;
  content: string;
  author: { id: number; username: string };
  createdAt: string;        // ISO string
  commentsCount: number;
}
