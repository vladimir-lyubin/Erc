import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { CurrencyStat } from '../../core/models/exchange.models';
import { ExchangeApiService } from '../../core/services/exchange-api.service';

/**
 * Analytics dashboard: surfaces which currencies are queried most often and when they were last
 * queried, as a bar chart plus a supporting table.
 */
@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './analytics.component.html',
})
export class AnalyticsComponent implements OnInit {
  private readonly api = inject(ExchangeApiService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly stats = signal<CurrencyStat[]>([]);

  readonly chartData = signal<ChartConfiguration<'bar'>['data']>({ labels: [], datasets: [] });
  readonly chartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getAnalytics().subscribe({
      next: (res) => {
        this.stats.set(res.topCurrencies);
        this.chartData.set({
          labels: res.topCurrencies.map((s) => s.currency),
          datasets: [
            {
              data: res.topCurrencies.map((s) => s.totalCount),
              label: 'Query count',
              backgroundColor: '#1e3d6b',
            },
          ],
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Failed to load analytics.');
        this.loading.set(false);
      },
    });
  }
}
