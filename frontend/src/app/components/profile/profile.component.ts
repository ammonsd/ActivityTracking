/**
 * Description: Profile component - displays and allows editing of user profile information
 *
 * Author: Dean Ammons
 * Date: December 2025
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/task-activity.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatCheckboxModule,
    MatSnackBarModule,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  loading = false;
  currentUser: User | null = null;
  currentRole = '';
  showPasswordDialog = false;
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  showPasswords = false;
  passwordError = '';

  constructor(
    private readonly userService: UserService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.currentRole = this.authService.getCurrentRole();
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;

    // Use the dedicated profile endpoint
    this.userService.getCurrentUserProfile().subscribe({
      next: (response) => {
        this.currentUser = response.data || null;
        this.loading = false;
      },
      error: (error) => {
        this.snackBar.open(
          'Failed to load profile: ' + (error.error?.message || error.message),
          'Close',
          {
            duration: 5000,
          },
        );
        this.loading = false;
      },
    });
  }

  updateProfile(): void {
    if (!this.currentUser) {
      return;
    }

    this.loading = true;

    // Create a DTO with only the fields that UserEditDto expects
    // Extract role name as string (backend returns role as object but expects string)
    const profileUpdate = {
      id: this.currentUser.id,
      username: this.currentUser.username,
      firstname: this.currentUser.firstname,
      lastname: this.currentUser.lastname,
      company: this.currentUser.company,
      email: this.currentUser.email,
      role:
        typeof this.currentUser.role === 'string'
          ? this.currentUser.role
          : (this.currentUser.role as any)?.name ||
            (this.currentUser.role as any)?.roleName ||
            'ROLE_USER',
      enabled: this.currentUser.enabled,
      forcePasswordUpdate: this.currentUser.forcePasswordUpdate || false,
      accountLocked: this.currentUser.accountLocked || false,
      failedLoginAttempts: this.currentUser.failedLoginAttempts || 0,
    };

    // Use the dedicated profile update endpoint
    this.userService.updateCurrentUserProfile(profileUpdate as User).subscribe({
      next: (response) => {
        this.loading = false;
        this.snackBar.open('Profile updated successfully', 'Close', {
          duration: 3000,
        });
      },
      error: (error) => {
        this.loading = false;
        this.snackBar.open(
          'Failed to update profile: ' +
            (error.error?.message || error.message),
          'Close',
          {
            duration: 5000,
          },
        );
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/dashboard']);
  }

  openPasswordDialog(): void {
    this.showPasswordDialog = true;
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.showPasswords = false;
    this.passwordError = '';
  }

  closePasswordDialog(): void {
    this.showPasswordDialog = false;
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.showPasswords = false;
    this.passwordError = '';
  }

  updatePassword(): void {
    this.passwordError = '';

    if (!this.newPassword || !this.confirmPassword) {
      this.passwordError = 'Both fields are required';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.passwordError = 'Passwords do not match';
      return;
    }

    if (this.newPassword.length < 8) {
      this.passwordError = 'Password must be at least 8 characters';
      return;
    }

    if (!this.currentUser?.id) {
      this.passwordError = 'User ID not found';
      return;
    }

    this.loading = true;

    this.userService
      .updatePassword(
        this.currentUser.id,
        this.currentPassword,
        this.newPassword,
      )
      .subscribe({
        next: () => {
          this.loading = false;
          this.closePasswordDialog();
          this.snackBar.open('Password updated successfully', 'Close', {
            duration: 3000,
          });
        },
        error: (error) => {
          this.loading = false;

          // Handle validation errors with field-specific messages
          if (error.error?.data && typeof error.error.data === 'object') {
            // Extract field error message (newPassword field)
            const fieldError =
              error.error.data.newPassword || error.error.data.currentPassword;
            this.passwordError =
              fieldError || error.error?.message || 'Failed to update password';
          } else {
            this.passwordError =
              error.error?.message || 'Failed to update password';
          }
        },
      });
  }

  get roleDisplay(): string {
    if (!this.currentUser?.role) return '';

    // Handle role as object (with name or roleName property)
    if (typeof this.currentUser.role === 'object') {
      return (
        (this.currentUser.role as any)?.name ||
        (this.currentUser.role as any)?.roleName ||
        ''
      );
    }

    // Handle role as string
    return this.currentUser.role;
  }
}
