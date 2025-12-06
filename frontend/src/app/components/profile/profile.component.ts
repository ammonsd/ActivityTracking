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
    MatSnackBarModule,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  loading = false;
  currentUser: User | null = null;
  currentRole = '';

  constructor(
    private readonly userService: UserService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
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
        console.log('Profile loaded successfully:', response);
        this.currentUser = response.data || null;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading profile:', error);
        console.error('Error status:', error.status);
        console.error('Error message:', error.message);
        this.snackBar.open(
          'Failed to load profile: ' + (error.error?.message || error.message),
          'Close',
          {
            duration: 5000,
          }
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

    // Use the dedicated profile update endpoint
    this.userService.updateCurrentUserProfile(this.currentUser).subscribe({
      next: (response) => {
        console.log('Profile updated successfully:', response);
        this.loading = false;
        this.snackBar.open('Profile updated successfully', 'Close', {
          duration: 3000,
        });
      },
      error: (error) => {
        console.error('Error updating profile:', error);
        console.error('Error status:', error.status);
        console.error('Error message:', error.message);
        this.loading = false;
        this.snackBar.open(
          'Failed to update profile: ' +
            (error.error?.message || error.message),
          'Close',
          {
            duration: 5000,
          }
        );
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/dashboard']);
  }
}
