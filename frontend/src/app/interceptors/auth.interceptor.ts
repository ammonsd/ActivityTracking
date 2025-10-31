import { HttpInterceptorFn } from '@angular/common/http';

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

  return next(authReq);
};

/**
 * Extract CSRF token from cookie
 * Spring Security stores it as XSRF-TOKEN cookie
 */
function getCsrfTokenFromCookie(): string | null {
  const name = 'XSRF-TOKEN=';
  const decodedCookie = decodeURIComponent(document.cookie);
  const cookies = decodedCookie.split(';');

  for (let cookie of cookies) {
    cookie = cookie.trim();
    if (cookie.indexOf(name) === 0) {
      return cookie.substring(name.length);
    }
  }

  return null;
}
