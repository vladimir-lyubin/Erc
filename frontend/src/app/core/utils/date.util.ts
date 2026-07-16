const MS_PER_DAY = 86_400_000;

/** Formats a Date as an ISO `yyyy-MM-dd` string (the shape the backend expects for date params). */
export function isoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/** Today as `yyyy-MM-dd`. */
export function isoToday(): string {
  return isoDate(new Date());
}

/** The date `days` days before today as `yyyy-MM-dd`. */
export function isoDaysAgo(days: number): string {
  return isoDate(new Date(Date.now() - days * MS_PER_DAY));
}
