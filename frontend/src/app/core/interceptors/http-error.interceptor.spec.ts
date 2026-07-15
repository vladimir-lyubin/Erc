import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { httpErrorInterceptor } from './http-error.interceptor';

describe('httpErrorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('prefers ProblemDetail.detail as the normalised message', () => {
    let message: string | undefined;
    http.get('/x').subscribe({ error: (e) => (message = e.message) });

    httpMock
      .expectOne('/x')
      .flush({ detail: 'No rate for XYZ on 2024-03-15' }, { status: 404, statusText: 'Not Found' });

    expect(message).toBe('No rate for XYZ on 2024-03-15');
  });

  it('reports a connectivity message on status 0', () => {
    let message: string | undefined;
    http.get('/x').subscribe({ error: (e) => (message = e.message) });

    httpMock
      .expectOne('/x')
      .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    expect(message).toBe('Cannot reach the backend.');
  });

  it('falls back to a status-coded message when no body detail is present', () => {
    let message: string | undefined;
    http.get('/x').subscribe({ error: (e) => (message = e.message) });

    httpMock.expectOne('/x').flush(null, { status: 500, statusText: 'Server Error' });

    expect(message).toBe('Request failed (500).');
  });
});
