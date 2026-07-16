import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HistoricalRatesResponse, InsightResponse } from '@core/models';
import { ExchangeApiService } from '@core/services';
import { HistoricalComponent } from './historical.component';

describe('HistoricalComponent', () => {
  let api: jasmine.SpyObj<ExchangeApiService>;
  let component: HistoricalComponent;

  const history: HistoricalRatesResponse = {
    from: 'EUR',
    to: 'USD',
    fromDate: '2024-03-01',
    toDate: '2024-03-03',
    points: [
      { date: '2024-03-01', rate: 1.01 },
      { date: '2024-03-02', rate: 1.02 },
    ],
  };
  const insight: InsightResponse = {
    from: 'EUR',
    to: 'USD',
    fromDate: '2024-03-01',
    toDate: '2024-03-03',
    insight: 'Slight upward trend.',
  };

  beforeEach(() => {
    api = jasmine.createSpyObj<ExchangeApiService>('ExchangeApiService', ['getHistorical', 'getInsight']);
    TestBed.configureTestingModule({
      imports: [HistoricalComponent],
      providers: [{ provide: ExchangeApiService, useValue: api }],
    });
    component = TestBed.createComponent(HistoricalComponent).componentInstance;
  });

  it('rejects an inverted date range without calling the API', () => {
    component.form.controls.fromDate.setValue('2024-03-10');
    component.form.controls.toDate.setValue('2024-03-01');
    component.submit();
    expect(api.getHistorical).not.toHaveBeenCalled();
    expect(component.history.error()).toContain('From');
  });

  it('loads history, builds chart data and fetches the insight on success', () => {
    api.getHistorical.and.returnValue(of(history));
    api.getInsight.and.returnValue(of(insight));

    component.submit();

    expect(component.history.value()).toEqual(history);
    const data = component.chartData();
    expect(data.datasets.length).toBe(1);
    expect(data.datasets[0].data).toEqual([1.01, 1.02]);
    expect(data.labels).toEqual(['2024-03-01', '2024-03-02']);
    expect(component.insight.value()).toEqual(insight);
  });

  it('falls back to a placeholder insight when the insight call fails', () => {
    api.getHistorical.and.returnValue(of(history));
    api.getInsight.and.returnValue(throwError(() => ({ message: 'LLM down' })));

    component.submit();

    expect(component.history.value()).toEqual(history);
    expect(component.insight.value()?.insight).toContain('unavailable');
  });

  it('empty chart data before any request', () => {
    const data = component.chartData();
    expect(data.datasets.length).toBe(0);
    expect(data.labels).toEqual([]);
  });
});
