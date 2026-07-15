import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { ExchangeApiService } from './exchange-api.service';
import { ExchangeResponse } from '../models/exchange.models';

describe('ExchangeApiService', () => {
  let service: ExchangeApiService;
  let httpMock: HttpTestingController;
  const base = environment.apiBaseUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ExchangeApiService],
    });
    service = TestBed.inject(ExchangeApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('requests /exchange with from/to and omits date when not provided', () => {
    const payload: ExchangeResponse = {
      from: 'EUR',
      to: 'USD',
      exchange: 1.0503,
      date: '2024-03-15',
      fromQueryCount: 1,
      toQueryCount: 1,
    };

    let received: ExchangeResponse | undefined;
    service.getExchange('EUR', 'USD').subscribe((r) => (received = r));

    const req = httpMock.expectOne(
      (r) => r.url === `${base}/exchange` && r.params.get('from') === 'EUR',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('to')).toBe('USD');
    expect(req.request.params.has('date')).toBeFalse();
    req.flush(payload);

    expect(received).toEqual(payload);
  });

  it('includes the date param when supplied', () => {
    service.getExchange('EUR', 'PLN', '2024-03-15').subscribe();

    const req = httpMock.expectOne((r) => r.url === `${base}/exchange`);
    expect(req.request.params.get('date')).toBe('2024-03-15');
    req.flush({});
  });

  it('builds the historical request with the full date range', () => {
    service.getHistorical('EUR', 'USD', '2024-03-01', '2024-03-15').subscribe();

    const req = httpMock.expectOne((r) => r.url === `${base}/exchange/historical`);
    expect(req.request.params.get('fromDate')).toBe('2024-03-01');
    expect(req.request.params.get('toDate')).toBe('2024-03-15');
    req.flush({ from: 'EUR', to: 'USD', fromDate: '2024-03-01', toDate: '2024-03-15', points: [] });
  });

  it('calls the analytics endpoint', () => {
    service.getAnalytics().subscribe();
    const req = httpMock.expectOne(`${base}/analytics`);
    expect(req.request.method).toBe('GET');
    req.flush({ topCurrencies: [] });
  });
});
