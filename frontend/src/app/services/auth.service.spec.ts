import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    // Clear session storage before each test
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should successfully login with valid credentials', (done) => {
      const username = 'testuser';
      const password = 'password123';
      const mockResponse = {
        success: true,
        message: 'Login successful',
        data: { username: 'testuser', role: 'USER' },
      };

      service.login(username, password).subscribe({
        next: (response) => {
          expect(response).toEqual(mockResponse);
          expect(service.isAuthenticated()).toBe(true);
          expect(service.getCurrentUsername()).toBe('testuser');
          expect(service.getCurrentRole()).toBe('USER');
          done();
        },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get('Authorization')).toContain('Basic');
      req.flush(mockResponse);
    });

    it('should handle login failure', (done) => {
      const username = 'testuser';
      const password = 'wrongpassword';

      service.login(username, password).subscribe({
        error: (error) => {
          expect(service.isAuthenticated()).toBe(false);
          expect(service.getCurrentUsername()).toBe('');
          done();
        },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(
        { success: false, message: 'Invalid credentials' },
        { status: 401, statusText: 'Unauthorized' }
      );
    });

    it('should update observables on successful login', (done) => {
      const mockResponse = {
        success: true,
        message: 'Login successful',
        data: { username: 'admin', role: 'ADMIN' },
      };

      service.currentUser$.subscribe((username) => {
        if (username === 'admin') {
          expect(username).toBe('admin');
        }
      });

      service.userRole$.subscribe((role) => {
        if (role === 'ADMIN') {
          expect(role).toBe('ADMIN');
          done();
        }
      });

      service.login('admin', 'password').subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockResponse);
    });
  });

  describe('logout', () => {
    it('should clear authentication state', (done) => {
      const mockLoginResponse = {
        success: true,
        message: 'Login successful',
        data: { username: 'testuser', role: 'USER' },
      };

      // First login
      service.login('testuser', 'password').subscribe(() => {
        expect(service.isAuthenticated()).toBe(true);

        // Then logout
        service.logout();

        expect(service.isAuthenticated()).toBe(false);
        expect(service.getCurrentUsername()).toBe('');
        expect(service.getCurrentRole()).toBe('');
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockLoginResponse);
    });

    it('should make HTTP request to logout endpoint', () => {
      service.logout();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/logout`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });

    it('should update observables on logout', (done) => {
      service.isAuthenticated$.subscribe((isAuth) => {
        if (isAuth === false) {
          expect(isAuth).toBe(false);
        }
      });

      service.currentUser$.subscribe((username) => {
        if (username === '') {
          expect(username).toBe('');
        }
      });

      service.userRole$.subscribe((role) => {
        if (role === '') {
          expect(role).toBe('');
          done();
        }
      });

      service.logout();
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/logout`);
      req.flush({});
    });
  });

  describe('getAuthHeader', () => {
    it('should return headers with authorization when authenticated', (done) => {
      const mockResponse = {
        success: true,
        message: 'Login successful',
        data: { username: 'testuser', role: 'USER' },
      };

      service.login('testuser', 'password').subscribe(() => {
        const headers = service.getAuthHeader();
        expect(headers.get('Authorization')).toContain('Basic');
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockResponse);
    });

    it('should return empty headers when not authenticated', () => {
      const headers = service.getAuthHeader();
      expect(headers.get('Authorization')).toBeNull();
    });
  });

  describe('getCurrentUsername', () => {
    it('should return empty string when not authenticated', () => {
      expect(service.getCurrentUsername()).toBe('');
    });

    it('should return username when authenticated', (done) => {
      const mockResponse = {
        success: true,
        message: 'Login successful',
        data: { username: 'testuser', role: 'USER' },
      };

      service.login('testuser', 'password').subscribe(() => {
        expect(service.getCurrentUsername()).toBe('testuser');
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockResponse);
    });
  });

  describe('getCurrentRole', () => {
    it('should return empty string when not authenticated', () => {
      expect(service.getCurrentRole()).toBe('');
    });

    it('should return role when authenticated', (done) => {
      const mockResponse = {
        success: true,
        message: 'Login successful',
        data: { username: 'admin', role: 'ADMIN' },
      };

      service.login('admin', 'password').subscribe(() => {
        expect(service.getCurrentRole()).toBe('ADMIN');
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockResponse);
    });
  });

  describe('isAuthenticated', () => {
    it('should return false initially', () => {
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should return true after successful login', (done) => {
      const mockResponse = {
        success: true,
        message: 'Login successful',
        data: { username: 'testuser', role: 'USER' },
      };

      service.login('testuser', 'password').subscribe(() => {
        expect(service.isAuthenticated()).toBe(true);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockResponse);
    });

    it('should return false after logout', (done) => {
      const mockResponse = {
        success: true,
        message: 'Login successful',
        data: { username: 'testuser', role: 'USER' },
      };

      service.login('testuser', 'password').subscribe(() => {
        service.logout();
        expect(service.isAuthenticated()).toBe(false);

        // Expect logout request
        const logoutReq = httpMock.expectOne(
          `${environment.apiUrl}/auth/logout`
        );
        logoutReq.flush({});
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockResponse);
    });
  });
});
