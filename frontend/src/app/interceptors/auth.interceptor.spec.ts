import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);

    // Clear cookies before each test
    for (const c of document.cookie.split(';')) {
      document.cookie = c
        .replace(/^ +/, '')
        .replace(/=.*/, '=;expires=' + new Date().toUTCString() + ';path=/');
    }
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add withCredentials to all requests', () => {
    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.withCredentials).toBe(true);
    req.flush({});
  });

  it('should not add CSRF token to GET requests', () => {
    httpClient.get('/api/data').subscribe();

    const req = httpMock.expectOne('/api/data');
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    req.flush({});
  });

  it('should not add CSRF token to HEAD requests', () => {
    httpClient.head('/api/data').subscribe();

    const req = httpMock.expectOne('/api/data');
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    req.flush({});
  });

  it('should add CSRF token to POST requests when token exists', () => {
    // Set CSRF token in cookie
    document.cookie = 'XSRF-TOKEN=test-csrf-token';

    httpClient.post('/api/data', { test: 'data' }).subscribe();

    const req = httpMock.expectOne('/api/data');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('test-csrf-token');
    req.flush({});
  });

  it('should add CSRF token to PUT requests when token exists', () => {
    document.cookie = 'XSRF-TOKEN=test-put-token';

    httpClient.put('/api/data/1', { test: 'data' }).subscribe();

    const req = httpMock.expectOne('/api/data/1');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('test-put-token');
    req.flush({});
  });

  it('should add CSRF token to DELETE requests when token exists', () => {
    document.cookie = 'XSRF-TOKEN=test-delete-token';

    httpClient.delete('/api/data/1').subscribe();

    const req = httpMock.expectOne('/api/data/1');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('test-delete-token');
    req.flush({});
  });

  it('should add CSRF token to PATCH requests when token exists', () => {
    document.cookie = 'XSRF-TOKEN=test-patch-token';

    httpClient.patch('/api/data/1', { test: 'data' }).subscribe();

    const req = httpMock.expectOne('/api/data/1');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('test-patch-token');
    req.flush({});
  });

  it('should not add CSRF header when token is not in cookie', () => {
    httpClient.post('/api/data', { test: 'data' }).subscribe();

    const req = httpMock.expectOne('/api/data');
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    req.flush({});
  });

  it('should handle multiple cookies correctly', () => {
    document.cookie = 'other-cookie=value1';
    document.cookie = 'XSRF-TOKEN=correct-token';
    document.cookie = 'another-cookie=value2';

    httpClient.post('/api/data', {}).subscribe();

    const req = httpMock.expectOne('/api/data');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('correct-token');
    req.flush({});
  });

  it('should extract CSRF token with special characters', () => {
    document.cookie = 'XSRF-TOKEN=token-with-special_chars.123';

    httpClient.post('/api/data', {}).subscribe();

    const req = httpMock.expectOne('/api/data');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe(
      'token-with-special_chars.123'
    );
    req.flush({});
  });
});
