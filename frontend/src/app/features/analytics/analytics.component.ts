import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { AnalyticsResponse } from '@core/models';
import { ExchangeApiService } from '@core/services';
import { RequestState } from '@core/utils/request-state';
import { BAR_CHART_OPTIONS, toBarChartData } from '@shared/charts/chart-theme';

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

  readonly request = new RequestState<AnalyticsResponse>();

  readonly stats = computed(() => this.request.value()?.topCurrencies ?? []);
  readonly chartOptions = BAR_CHART_OPTIONS;
  readonly chartData = computed(() => toBarChartData(this.stats()));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.request.run(this.api.getAnalytics());
  }
}
