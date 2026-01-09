/**
 * Description: Admin guard - protects routes by checking if user has admin role
 *
 * Author: Dean Ammons
 * Date: October 2025
 */

import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const role = authService.getCurrentRole();

  console.log('adminGuard - Current role:', role);

  if (role === 'ADMIN') {
    console.log('adminGuard - ADMIN access granted');
    return true;
  }

  console.log('adminGuard - Access denied, redirecting to dashboard');
  // Redirect non-admin users to dashboard
  router.navigate(['/dashboard']);
  return false;
};
