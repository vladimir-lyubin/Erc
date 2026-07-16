import { ChartConfiguration } from 'chart.js';
import { CurrencyStat, HistoricalRatesResponse } from '@core/models';

/** Shared palette so chart colours are defined once, not hardcoded per component. */
export const CHART_COLORS = {
  line: '#2e86de',
  lineFill: 'rgba(46, 134, 222, 0.15)',
  bar: '#1e3d6b',
} as const;

export const LINE_CHART_OPTIONS: ChartConfiguration<'line'>['options'] = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: true } },
  scales: { y: { beginAtZero: false } },
};

export const BAR_CHART_OPTIONS: ChartConfiguration<'bar'>['options'] = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
};

const EMPTY_CHART = { labels: [], datasets: [] };

/** Line-chart dataset for a historical rate series. */
export function toLineChartData(
  history: HistoricalRatesResponse | null,
): ChartConfiguration<'line'>['data'] {
  if (!history) {
    return EMPTY_CHART;
  }
  return {
    labels: history.points.map((p) => p.date),
    datasets: [
      {
        data: history.points.map((p) => p.rate),
        label: `${history.from}/${history.to}`,
        borderColor: CHART_COLORS.line,
        backgroundColor: CHART_COLORS.lineFill,
        fill: true,
        tension: 0.25,
        pointRadius: 2,
      },
    ],
  };
}

/** Bar-chart dataset for currency usage counts. */
export function toBarChartData(stats: CurrencyStat[]): ChartConfiguration<'bar'>['data'] {
  return {
    labels: stats.map((s) => s.currency),
    datasets: [
      {
        data: stats.map((s) => s.totalCount),
        label: 'Query count',
        backgroundColor: CHART_COLORS.bar,
      },
    ],
  };
}
