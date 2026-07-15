import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { CURRENCIES } from '../../core/currencies';
import {
  HistoricalRatesResponse,
  InsightResponse,
} from '../../core/models/exchange.models';
import { ExchangeApiService } from '../../core/services/exchange-api.service';

/**
 * Historical view: pick a pair + date range, then show the raw-rates table and a line chart side by
 * side, plus the AI-generated trend insight (which has its own independent loading state).
 */
@Component({
  selector: 'app-historical',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, BaseChartDirective],
  templateUrl: './historical.component.html',
})
export class HistoricalComponent {
  private readonly api = inject(ExchangeApiService);
  private readonly fb = inject(FormBuilder);

  readonly currencies = CURRENCIES;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly history = signal<HistoricalRatesResponse | null>(null);

  readonly insightLoading = signal(false);
  readonly insight = signal<InsightResponse | null>(null);

  readonly chartData = signal<ChartConfiguration<'line'>['data']>({ labels: [], datasets: [] });
  readonly chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: true } },
    scales: { y: { beginAtZero: false } },
  };

  private readonly today = new Date().toISOString().slice(0, 10);
  private readonly monthAgo = new Date(Date.now() - 29 * 86_400_000).toISOString().slice(0, 10);

  readonly form = this.fb.nonNullable.group({
    from: ['EUR', Validators.required],
    to: ['PLN', Validators.required],
    fromDate: [this.monthAgo, Validators.required],
    toDate: [this.today, Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { from, to, fromDate, toDate } = this.form.getRawValue();
    if (fromDate > toDate) {
      this.error.set('"From" date must not be after "To" date.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.history.set(null);
    this.insight.set(null);

    this.api.getHistorical(from, to, fromDate, toDate).subscribe({
      next: (res) => {
        this.history.set(res);
        this.chartData.set({
          labels: res.points.map((p) => p.date),
          datasets: [
            {
              data: res.points.map((p) => p.rate),
              label: `${from}/${to}`,
              borderColor: '#2e86de',
              backgroundColor: 'rgba(46, 134, 222, 0.15)',
              fill: true,
              tension: 0.25,
              pointRadius: 2,
            },
          ],
        });
        this.loading.set(false);
        this.loadInsight(from, to, fromDate, toDate);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Failed to load historical rates.');
        this.loading.set(false);
      },
    });
  }

  private loadInsight(from: string, to: string, fromDate: string, toDate: string): void {
    this.insightLoading.set(true);
    this.api.getInsight(from, to, fromDate, toDate).subscribe({
      next: (res) => {
        this.insight.set(res);
        this.insightLoading.set(false);
      },
      error: () => {
        this.insight.set({ from, to, fromDate, toDate, insight: 'Trend insight is unavailable right now.' });
        this.insightLoading.set(false);
      },
    });
  }
}
