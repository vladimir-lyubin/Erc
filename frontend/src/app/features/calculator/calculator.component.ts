import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CURRENCIES } from '../../core/currencies';
import { ExchangeResponse } from '../../core/models/exchange.models';
import { ExchangeApiService } from '../../core/services/exchange-api.service';

/**
 * Calculator view: pick two currencies + optional date, show the spread-adjusted rate.
 * Validated inputs, clear error messages on API error, and a visible loading state.
 */
@Component({
  selector: 'app-calculator',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './calculator.component.html',
})
export class CalculatorComponent {
  private readonly api = inject(ExchangeApiService);
  private readonly fb = inject(FormBuilder);

  readonly currencies = CURRENCIES;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<ExchangeResponse | null>(null);

  readonly form = this.fb.nonNullable.group({
    from: ['EUR', Validators.required],
    to: ['USD', Validators.required],
    date: [''],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { from, to, date } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);
    this.api.getExchange(from, to, date || undefined).subscribe({
      next: (res) => {
        this.result.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Failed to fetch rate.');
        this.loading.set(false);
      },
    });
  }
}
