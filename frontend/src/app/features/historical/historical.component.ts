import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { catchError, of } from 'rxjs';
import { HistoricalRatesResponse, InsightResponse } from '@core/models';
import { ExchangeApiService } from '@core/services';
import { isoDaysAgo, isoToday } from '@core/utils/date.util';
import { RequestState } from '@core/utils/request-state';
import { LINE_CHART_OPTIONS, toLineChartData } from '@shared/charts/chart-theme';
import { CurrencySelectComponent } from '@shared/ui/currency-select/currency-select.component';

/**
 * Historical view: pick a pair + date range, then show the raw-rates table and a line chart side by
 * side, plus the AI-generated trend insight (which has its own independent loading state).
 */
@Component({
  selector: 'app-historical',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, BaseChartDirective, CurrencySelectComponent],
  templateUrl: './historical.component.html',
})
export class HistoricalComponent {
  private readonly api = inject(ExchangeApiService);
  private readonly fb = inject(FormBuilder);

  readonly history = new RequestState<HistoricalRatesResponse>();
  readonly insight = new RequestState<InsightResponse>();

  readonly chartOptions = LINE_CHART_OPTIONS;
  readonly chartData = computed(() => toLineChartData(this.history.value()));

  readonly form = this.fb.nonNullable.group({
    from: ['EUR', Validators.required],
    to: ['USD', Validators.required],
    fromDate: [isoDaysAgo(29), Validators.required],
    toDate: [isoToday(), Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { from, to, fromDate, toDate } = this.form.getRawValue();
    if (fromDate > toDate) {
      this.history.error.set('"From" date must not be after "To" date.');
      return;
    }

    this.insight.clear();
    this.history.run(this.api.getHistorical(from, to, fromDate, toDate), () =>
      this.loadInsight(from, to, fromDate, toDate),
    );
  }

  private loadInsight(from: string, to: string, fromDate: string, toDate: string): void {
    const fallback: InsightResponse = {
      from,
      to,
      fromDate,
      toDate,
      insight: 'Trend insight is unavailable right now.',
    };
    this.insight.run(
      this.api.getInsight(from, to, fromDate, toDate).pipe(catchError(() => of(fallback))),
    );
  }
}
