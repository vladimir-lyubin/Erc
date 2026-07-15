import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AnalyticsResponse,
  ExchangeResponse,
  HistoricalRatesResponse,
  InsightResponse,
} from '../models/exchange.models';

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
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('fromDate', fromDate)
      .set('toDate', toDate);
    return this.http.get<HistoricalRatesResponse>(`${this.baseUrl}/exchange/historical`, { params });
  }

  getInsight(
    from: string,
    to: string,
    fromDate: string,
    toDate: string,
  ): Observable<InsightResponse> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('fromDate', fromDate)
      .set('toDate', toDate);
    return this.http.get<InsightResponse>(`${this.baseUrl}/exchange/insight`, { params });
  }

  getAnalytics(): Observable<AnalyticsResponse> {
    return this.http.get<AnalyticsResponse>(`${this.baseUrl}/analytics`);
  }
}
