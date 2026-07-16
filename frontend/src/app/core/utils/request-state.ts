import { signal } from '@angular/core';
import { finalize, Observable } from 'rxjs';
import { AppError } from '@core/models';

/**
 * Bundles the loading / error / value signals and the subscribe boilerplate that every data-fetching
 * component otherwise repeats. Components create one instance per independent request and read its
 * signals from the template.
 */
export class RequestState<T> {
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly value = signal<T | null>(null);

  /** Runs the request, toggling {@link loading} and capturing the result or a normalised error. */
  run(source$: Observable<T>, onSuccess?: (value: T) => void): void {
    this.loading.set(true);
    this.error.set(null);
    source$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (value) => {
        this.value.set(value);
        onSuccess?.(value);
      },
      error: (err: AppError) => this.error.set(err?.message ?? 'Request failed.'),
    });
  }

  /** Clears any previous value and error (e.g. before starting a fresh request). */
  clear(): void {
    this.value.set(null);
    this.error.set(null);
  }
}
