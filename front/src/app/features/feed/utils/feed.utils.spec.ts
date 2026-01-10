import { formatComments, parseOrder } from './feed.utils';

describe('feed.utils', () => {
  it('parseOrder should accept asc/desc and fallback to desc', () => {
    // Arrange / Act / Assert
    expect(parseOrder('asc')).toBe('asc');
    expect(parseOrder('desc')).toBe('desc');
    expect(parseOrder(null)).toBe('desc');
    expect(parseOrder(undefined)).toBe('desc');
    expect(parseOrder('nope')).toBe('desc');
  });

  it('formatComments should format 0/1/n', () => {
    // Arrange / Act / Assert
    expect(formatComments(0)).toBe('Aucun commentaire');
    expect(formatComments(1)).toBe('1 commentaire');
    expect(formatComments(2)).toBe('2 commentaires');
  });
});
