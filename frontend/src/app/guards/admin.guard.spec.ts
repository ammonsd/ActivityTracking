import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { adminGuard } from './admin.guard';

describe('adminGuard', () => {
  let authService: AuthService;
  let router: Router;

  beforeEach(() => {
    const authServiceMock = {
      getCurrentRole: jasmine.createSpy('getCurrentRole'),
    };

    const routerMock = {
      navigate: jasmine.createSpy('navigate'),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
      ],
    });

    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  it('should allow access when user has ADMIN role', () => {
    (authService.getCurrentRole as jasmine.Spy).and.returnValue('ADMIN');

    const result = TestBed.runInInjectionContext(() => adminGuard());

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should deny access and redirect to dashboard when user is not ADMIN', () => {
    (authService.getCurrentRole as jasmine.Spy).and.returnValue('USER');

    const result = TestBed.runInInjectionContext(() => adminGuard());

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should deny access when user has GUEST role', () => {
    (authService.getCurrentRole as jasmine.Spy).and.returnValue('GUEST');

    const result = TestBed.runInInjectionContext(() => adminGuard());

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should deny access when role is empty', () => {
    (authService.getCurrentRole as jasmine.Spy).and.returnValue('');

    const result = TestBed.runInInjectionContext(() => adminGuard());

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should check current role', () => {
    (authService.getCurrentRole as jasmine.Spy).and.returnValue('ADMIN');

    TestBed.runInInjectionContext(() => adminGuard());

    expect(authService.getCurrentRole).toHaveBeenCalled();
  });
});
