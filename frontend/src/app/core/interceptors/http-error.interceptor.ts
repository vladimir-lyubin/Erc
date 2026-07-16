import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AppError } from '@core/models';

/**
 * Normalises backend errors into a single-string message on `error.message`, so components can
 * display a consistent message regardless of the ProblemDetail shape. Re-throws a typed {@link AppError}
 * so callers still handle the error branch (and toggle their own loading state).
 */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message =
        error.error?.detail ??
        error.error?.message ??
        (error.status === 0 ? 'Cannot reach the backend.' : `Request failed (${error.status}).`);
      const appError: AppError = { message, status: error.status };
      return throwError(() => appError);
    }),
  );
