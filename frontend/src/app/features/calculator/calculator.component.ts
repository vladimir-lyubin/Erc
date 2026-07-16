import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ExchangeResponse } from '@core/models';
import { ExchangeApiService } from '@core/services';
import { RequestState } from '@core/utils/request-state';
import { CurrencySelectComponent } from '@shared/ui/currency-select/currency-select.component';

/**
 * Calculator view: pick two currencies + optional date, show the spread-adjusted rate.
 * Validated inputs, clear error messages on API error, and a visible loading state.
 */
@Component({
  selector: 'app-calculator',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencySelectComponent],
  templateUrl: './calculator.component.html',
})
export class CalculatorComponent {
  private readonly api = inject(ExchangeApiService);
  private readonly fb = inject(FormBuilder);

  readonly request = new RequestState<ExchangeResponse>();

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
    this.request.run(this.api.getExchange(from, to, date || undefined));
  }
}
