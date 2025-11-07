import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // For Spring Security session-based auth, we need to:
  // 1. Send credentials (cookies) - includes JSESSIONID
  // 2. Add CSRF token from cookie to header for non-GET requests

  let authReq = req.clone({
    withCredentials: true,
  });

  // Add CSRF token for state-changing requests (POST, PUT, DELETE, PATCH)
  if (
    req.method !== 'GET' &&
    req.method !== 'HEAD' &&
    req.method !== 'OPTIONS'
  ) {
    const csrfToken = getCsrfTokenFromCookie();
    if (csrfToken) {
      authReq = authReq.clone({
        setHeaders: {
          'X-XSRF-TOKEN': csrfToken,
        },
      });
    }
  }

  // Handle session timeout and authentication errors
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Session timeout or authentication failure
      if (error.status === 401) {
        console.log('Session expired or unauthorized - redirecting to login');

        // Clear sessionStorage directly (avoid circular dependency with AuthService)
        sessionStorage.removeItem('auth');
        sessionStorage.removeItem('username');
        sessionStorage.removeItem('userRole');

        // Redirect to login page with session timeout message
        globalThis.location.href = '/login?timeout=true';

        // Return empty error to prevent further error handling
        return throwError(() => new Error('Session expired'));
      }

      // For other errors, pass them through
      return throwError(() => error);
    })
  );
};

/**
 * Extract CSRF token from cookie
 * Spring Security stores it as XSRF-TOKEN cookie
 */
function getCsrfTokenFromCookie(): string | null {
  const name = 'XSRF-TOKEN=';
  const decodedCookie = decodeURIComponent(document.cookie);
  const cookies = decodedCookie.split(';');

  for (const cookieStr of cookies) {
    const trimmedCookie = cookieStr.trim();
    if (trimmedCookie.startsWith(name)) {
      return trimmedCookie.substring(name.length);
    }
  }

  return null;
}
