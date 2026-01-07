import { FeedOrder } from '../interfaces/feed.models';

export function parseOrder(value: string | null | undefined): FeedOrder {
  return value === 'asc' || value === 'desc' ? value : 'desc';
}

export function formatComments(count: number): string {
  if (count === 0) return 'Aucun commentaire';
  if (count === 1) return '1 commentaire';
  return `${count} commentaires`;
}
