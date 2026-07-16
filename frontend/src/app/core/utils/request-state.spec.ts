import { of, Subject, throwError } from 'rxjs';
import { RequestState } from './request-state';

describe('RequestState', () => {
  it('captures the value and clears loading on success', () => {
    const state = new RequestState<number>();
    state.run(of(42));
    expect(state.value()).toBe(42);
    expect(state.loading()).toBeFalse();
    expect(state.error()).toBeNull();
  });

  it('captures a normalised error message and clears loading on failure', () => {
    const state = new RequestState<number>();
    state.run(throwError(() => ({ message: 'nope' })));
    expect(state.error()).toBe('nope');
    expect(state.value()).toBeNull();
    expect(state.loading()).toBeFalse();
  });

  it('falls back to a generic message when the error carries none', () => {
    const state = new RequestState<number>();
    state.run(throwError(() => ({})));
    expect(state.error()).toBe('Request failed.');
  });

  it('is loading until the source emits', () => {
    const state = new RequestState<number>();
    const source = new Subject<number>();
    state.run(source.asObservable());
    expect(state.loading()).toBeTrue();
    source.next(1);
    source.complete();
    expect(state.loading()).toBeFalse();
  });

  it('invokes the onSuccess callback with the value', () => {
    const state = new RequestState<number>();
    const onSuccess = jasmine.createSpy('onSuccess');
    state.run(of(7), onSuccess);
    expect(onSuccess).toHaveBeenCalledWith(7);
  });

  it('clear() resets value and error', () => {
    const state = new RequestState<number>();
    state.run(of(1));
    state.clear();
    expect(state.value()).toBeNull();
    expect(state.error()).toBeNull();
  });
});
