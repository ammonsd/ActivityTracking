import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard = () => {
  const authService = inject(AuthService);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Redirect to Spring Boot login page
  globalThis.location.href = '/login';
  return false;
};
