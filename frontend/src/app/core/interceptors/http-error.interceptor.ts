import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/**
 * Normalises backend errors into a single-string message on `error.message`, so components can
 * display a consistent message regardless of the ProblemDetail shape. Re-throws so callers still
 * handle the error branch (and toggle their own loading state).
 */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message =
        error.error?.detail ??
        error.error?.message ??
        (error.status === 0 ? 'Cannot reach the backend.' : `Request failed (${error.status}).`);
      return throwError(() => ({ ...error, message }));
    }),
  );
