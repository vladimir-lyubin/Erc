import { isoDate, isoDaysAgo, isoToday } from './date.util';

describe('date.util', () => {
  it('isoDate formats a Date as yyyy-MM-dd', () => {
    expect(isoDate(new Date(Date.UTC(2024, 2, 15, 10, 30)))).toBe('2024-03-15');
  });

  it('isoToday equals the UTC date slice of now', () => {
    expect(isoToday()).toBe(new Date().toISOString().slice(0, 10));
  });

  it('isoDaysAgo(0) equals today', () => {
    expect(isoDaysAgo(0)).toBe(isoToday());
  });

  it('isoDaysAgo(n) goes back exactly n days', () => {
    const expected = new Date(Date.now() - 5 * 86_400_000).toISOString().slice(0, 10);
    expect(isoDaysAgo(5)).toBe(expected);
  });
});
