import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CURRENCIES } from '@core/constants/currencies';
import { CurrencySelectComponent } from './currency-select.component';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, CurrencySelectComponent],
  template: `<form [formGroup]="form"><app-currency-select label="From" controlName="from" /></form>`,
})
class HostComponent {
  form = new FormGroup({ from: new FormControl('USD') });
}

describe('CurrencySelectComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  it('renders the label and one option per currency', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('From');
    expect(el.querySelectorAll('option').length).toBe(CURRENCIES.length);
  });

  it('binds the select to the parent form control', () => {
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    expect(select.value).toBe('USD');

    fixture.componentInstance.form.controls.from.setValue('EUR');
    fixture.detectChanges();
    expect(select.value).toBe('EUR');
  });
});
