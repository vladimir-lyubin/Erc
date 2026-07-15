// DTO interfaces mirroring the backend responses (Appendix A of the brief).

export interface ExchangeResponse {
  from: string;
  to: string;
  exchange: number;
  date: string; // ISO date
  fromQueryCount: number;
  toQueryCount: number;
}

export interface HistoricalPoint {
  date: string; // ISO date
  rate: number;
}

export interface HistoricalRatesResponse {
  from: string;
  to: string;
  fromDate: string;
  toDate: string;
  points: HistoricalPoint[];
}

export interface CurrencyStat {
  currency: string;
  totalCount: number;
  lastQueried: string; // ISO date
}

export interface AnalyticsResponse {
  topCurrencies: CurrencyStat[];
}

export interface InsightResponse {
  from: string;
  to: string;
  fromDate: string;
  toDate: string;
  insight: string;
}
