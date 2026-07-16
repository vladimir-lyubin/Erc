import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env';
import {
  AnalyticsResponse,
  ExchangeResponse,
  HistoricalRatesResponse,
  InsightResponse,
} from '@core/models';

/**
 * Single owner of all HTTP calls to the backend. Components consume this service and never
 * touch HttpClient directly. Base URL comes from the environment (configurable, no hardcoding).
 */
@Injectable({ providedIn: 'root' })
export class ExchangeApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getExchange(from: string, to: string, date?: string): Observable<ExchangeResponse> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<ExchangeResponse>(`${this.baseUrl}/exchange`, { params });
  }

  getHistorical(
    from: string,
    to: string,
    fromDate: string,
    toDate: string,
  ): Observable<HistoricalRatesResponse> {
    return this.http.get<HistoricalRatesResponse>(`${this.baseUrl}/exchange/historical`, {
      params: this.rangeParams(from, to, fromDate, toDate),
    });
  }

  getInsight(
    from: string,
    to: string,
    fromDate: string,
    toDate: string,
  ): Observable<InsightResponse> {
    return this.http.get<InsightResponse>(`${this.baseUrl}/exchange/insight`, {
      params: this.rangeParams(from, to, fromDate, toDate),
    });
  }

  getAnalytics(): Observable<AnalyticsResponse> {
    return this.http.get<AnalyticsResponse>(`${this.baseUrl}/analytics`);
  }

  /** Shared query params for the pair + date-range endpoints (historical and insight). */
  private rangeParams(from: string, to: string, fromDate: string, toDate: string): HttpParams {
    return new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('fromDate', fromDate)
      .set('toDate', toDate);
  }
}
