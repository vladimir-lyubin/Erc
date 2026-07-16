import { Component, Input } from '@angular/core';
import { ControlContainer, FormGroupDirective, ReactiveFormsModule } from '@angular/forms';
import { CURRENCIES } from '@core/constants/currencies';

/**
 * Labelled currency dropdown reused by every form. It binds to the enclosing form via the parent
 * {@link FormGroupDirective} (see {@code viewProviders}), so callers only pass the control name:
 * {@code <app-currency-select label="From" controlName="from" />}.
 */
@Component({
  selector: 'app-currency-select',
  standalone: true,
  imports: [ReactiveFormsModule],
  viewProviders: [{ provide: ControlContainer, useExisting: FormGroupDirective }],
  template: `
    <label>
      {{ label }}
      <select [formControlName]="controlName">
        @for (currency of currencies; track currency) {
          <option [value]="currency">{{ currency }}</option>
        }
      </select>
    </label>
  `,
})
export class CurrencySelectComponent {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) controlName!: string;
  readonly currencies = CURRENCIES;
}
