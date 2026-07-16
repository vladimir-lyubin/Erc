import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AnalyticsResponse } from '@core/models';
import { ExchangeApiService } from '@core/services';
import { AnalyticsComponent } from './analytics.component';

describe('AnalyticsComponent', () => {
  let api: jasmine.SpyObj<ExchangeApiService>;
  let component: AnalyticsComponent;

  beforeEach(() => {
    api = jasmine.createSpyObj<ExchangeApiService>('ExchangeApiService', ['getAnalytics']);
    TestBed.configureTestingModule({
      imports: [AnalyticsComponent],
      providers: [{ provide: ExchangeApiService, useValue: api }],
    });
    component = TestBed.createComponent(AnalyticsComponent).componentInstance;
  });

  it('exposes stats and bar-chart data on success', () => {
    const response: AnalyticsResponse = {
      topCurrencies: [
        { currency: 'EUR', totalCount: 5, lastQueried: '2024-03-15' },
        { currency: 'USD', totalCount: 2, lastQueried: null },
      ],
    };
    api.getAnalytics.and.returnValue(of(response));

    component.load();

    expect(component.stats().length).toBe(2);
    expect(component.chartData().labels).toEqual(['EUR', 'USD']);
    expect(component.chartData().datasets[0].data).toEqual([5, 2]);
  });

  it('yields empty stats when nothing is recorded', () => {
    api.getAnalytics.and.returnValue(of({ topCurrencies: [] }));
    component.load();
    expect(component.stats()).toEqual([]);
    expect(component.chartData().labels).toEqual([]);
  });

  it('surfaces the error message on failure', () => {
    api.getAnalytics.and.returnValue(throwError(() => ({ message: 'boom' })));
    component.load();
    expect(component.request.error()).toBe('boom');
  });
});
