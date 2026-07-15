import { Routes } from '@angular/router';

/**
 * Three required views (lazy-loaded standalone components):
 *  - calculator: spread-adjusted rate for a pair
 *  - historical: table + line chart + AI trend insight
 *  - analytics:  usage dashboard
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'calculator' },
  {
    path: 'calculator',
    loadComponent: () =>
      import('./features/calculator/calculator.component').then((m) => m.CalculatorComponent),
  },
  {
    path: 'historical',
    loadComponent: () =>
      import('./features/historical/historical.component').then((m) => m.HistoricalComponent),
  },
  {
    path: 'analytics',
    loadComponent: () =>
      import('./features/analytics/analytics.component').then((m) => m.AnalyticsComponent),
  },
  { path: '**', redirectTo: 'calculator' },
];
