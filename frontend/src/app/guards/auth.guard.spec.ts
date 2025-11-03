import { TestBed } from '@angular/core/testing';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let authService: AuthService;
  let originalLocation: Location;

  beforeEach(() => {
    const authServiceMock = {
      isAuthenticated: jasmine.createSpy('isAuthenticated'),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authServiceMock }],
    });

    authService = TestBed.inject(AuthService);

    // Mock globalThis.location
    originalLocation = globalThis.location;
    delete (globalThis as any).location;
    (globalThis as any).location = { href: '' };
  });

  afterEach(() => {
    // Restore original location
    (globalThis as any).location = originalLocation;
  });

  it('should allow access when user is authenticated', () => {
    (authService.isAuthenticated as jasmine.Spy).and.returnValue(true);

    const result = TestBed.runInInjectionContext(() => authGuard());

    expect(result).toBe(true);
    expect(globalThis.location.href).toBe('');
  });

  it('should deny access and redirect to login when user is not authenticated', () => {
    (authService.isAuthenticated as jasmine.Spy).and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard());

    expect(result).toBe(false);
    expect(globalThis.location.href).toBe('/login');
  });

  it('should check authentication status', () => {
    (authService.isAuthenticated as jasmine.Spy).and.returnValue(true);

    TestBed.runInInjectionContext(() => authGuard());

    expect(authService.isAuthenticated).toHaveBeenCalled();
  });
});
