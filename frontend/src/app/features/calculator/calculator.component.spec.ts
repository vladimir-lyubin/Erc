import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ExchangeResponse } from '@core/models';
import { ExchangeApiService } from '@core/services';
import { CalculatorComponent } from './calculator.component';

describe('CalculatorComponent', () => {
  let api: jasmine.SpyObj<ExchangeApiService>;
  let component: CalculatorComponent;

  const sample: ExchangeResponse = {
    from: 'EUR',
    to: 'USD',
    exchange: 1.05,
    date: '2024-03-15',
    fromQueryCount: 1,
    toQueryCount: 1,
  };

  beforeEach(() => {
    api = jasmine.createSpyObj<ExchangeApiService>('ExchangeApiService', ['getExchange']);
    TestBed.configureTestingModule({
      imports: [CalculatorComponent],
      providers: [{ provide: ExchangeApiService, useValue: api }],
    });
    component = TestBed.createComponent(CalculatorComponent).componentInstance;
  });

  it('does not call the API when the form is invalid', () => {
    component.form.controls.from.setValue('');
    component.submit();
    expect(api.getExchange).not.toHaveBeenCalled();
  });

  it('omits the date param when it is blank', () => {
    api.getExchange.and.returnValue(of(sample));
    component.submit();
    expect(api.getExchange).toHaveBeenCalledWith('EUR', 'USD', undefined);
  });

  it('passes the date param when provided', () => {
    api.getExchange.and.returnValue(of(sample));
    component.form.controls.date.setValue('2024-03-15');
    component.submit();
    expect(api.getExchange).toHaveBeenCalledWith('EUR', 'USD', '2024-03-15');
  });

  it('stores the result on success', () => {
    api.getExchange.and.returnValue(of(sample));
    component.submit();
    expect(component.request.value()).toEqual(sample);
    expect(component.request.loading()).toBeFalse();
    expect(component.request.error()).toBeNull();
  });

  it('surfaces the error message on failure', () => {
    api.getExchange.and.returnValue(throwError(() => ({ message: 'No rate' })));
    component.submit();
    expect(component.request.error()).toBe('No rate');
    expect(component.request.value()).toBeNull();
  });
});
