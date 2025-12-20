import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ExpenseService } from '../../services/expense.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    RouterModule,
  ],
  template: `
    <div class="dashboard-container">
      <!-- Welcome Banner for GUEST users only -->
      <div class="guest-banner" *ngIf="currentRole === 'GUEST'">
        <div class="banner-content">
          <strong>👋 Welcome, Guest!</strong>
          <span>
            You can create and edit your own tasks and view the weekly
            timesheet. Other features (expenses, users, dropdowns) are visible
            but read-only to allow exposure to the full application
            functionality.
          </span>
        </div>
      </div>

      <h2>Admin Dashboard</h2>

      <div class="dashboard-grid">
        <mat-card class="dashboard-card" routerLink="/tasks">
          <mat-card-header>
            <mat-icon>assignment</mat-icon>
            <mat-card-title>Task Activities</mat-card-title>
          </mat-card-header>
        </mat-card>

        <mat-card
          *ngIf="canAccessExpenses || currentRole === 'GUEST'"
          class="dashboard-card"
          [class.disabled]="currentRole === 'GUEST'"
          routerLink="/expenses"
        >
          <mat-card-header>
            <mat-icon>receipt</mat-icon>
            <mat-card-title>Expenses</mat-card-title>
          </mat-card-header>
        </mat-card>

        <mat-card
          *ngIf="currentRole === 'ADMIN' || currentRole === 'GUEST'"
          class="dashboard-card"
          [class.disabled]="currentRole === 'GUEST'"
          routerLink="/users"
        >
          <mat-card-header>
            <mat-icon>people</mat-icon>
            <mat-card-title>User Management</mat-card-title>
          </mat-card-header>
        </mat-card>

        <mat-card
          *ngIf="currentRole !== 'GUEST'"
          class="dashboard-card"
          routerLink="/profile"
        >
          <mat-card-header>
            <mat-icon>account_circle</mat-icon>
            <mat-card-title>My Profile</mat-card-title>
          </mat-card-header>
        </mat-card>

        <mat-card
          *ngIf="currentRole === 'ADMIN' || currentRole === 'GUEST'"
          class="dashboard-card"
          [class.disabled]="currentRole === 'GUEST'"
          routerLink="/dropdowns"
        >
          <mat-card-header>
            <mat-icon>list</mat-icon>
            <mat-card-title>Dropdown Management</mat-card-title>
          </mat-card-header>
        </mat-card>

        <mat-card class="dashboard-card" routerLink="/reports">
          <mat-card-header>
            <mat-icon>analytics</mat-icon>
            <mat-card-title>Analytics & Reports</mat-card-title>
          </mat-card-header>
        </mat-card>
      </div>

      <!-- Welcome Section - Only show for GUEST users -->
      <div class="info-section" *ngIf="currentRole === 'GUEST'">
        <mat-card>
          <mat-card-header>
            <mat-card-title
              >Welcome to the Angular Admin Dashboard</mat-card-title
            >
          </mat-card-header>
          <mat-card-content>
            <p>
              This dashboard demonstrates Angular integration with the Spring
              Boot backend.
            </p>
            <p><strong>Features:</strong></p>
            <ul>
              <li>Angular 19 with standalone components</li>
              <li>Angular Material UI components</li>
              <li>RESTful API integration</li>
              <li>Responsive design</li>
              <li>TypeScript type safety</li>
              <li>Reactive state management with RxJS</li>
              <li>Session-based authentication</li>
              <li>Password expiration warnings</li>
            </ul>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [
    `
      .dashboard-container {
        padding: 20px;
        max-width: 1200px;
        margin: 0 auto;
      }

      .guest-banner {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 15px 20px;
        margin: -20px -20px 20px -20px;
        text-align: center;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }

      .banner-content {
        max-width: 1200px;
        margin: 0 auto;
      }

      .banner-content strong {
        font-size: 16px;
        margin-right: 10px;
      }

      .banner-content span {
        font-size: 14px;
      }

      h2 {
        color: #333;
        margin-bottom: 30px;
      }

      .dashboard-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 20px;
        margin-bottom: 30px;
      }

      .dashboard-card {
        cursor: pointer;
        transition: transform 0.2s, box-shadow 0.2s;
      }

      .dashboard-card:hover {
        transform: translateY(-5px);
        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
      }

      .dashboard-card.disabled {
        opacity: 0.5;
        pointer-events: none;
        cursor: not-allowed;
      }

      .dashboard-card.disabled:hover {
        transform: none;
        box-shadow: none;
      }

      mat-card-header {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 10px;
      }

      mat-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: #1976d2;
      }

      .info-section {
        margin-top: 30px;
      }

      ul {
        margin: 10px 0;
        padding-left: 20px;
      }

      li {
        margin: 5px 0;
      }
    `,
  ],
})
export class DashboardComponent implements OnInit {
  currentRole = '';
  canAccessExpenses = false;

  constructor(
    private readonly authService: AuthService,
    private readonly expenseService: ExpenseService
  ) {}

  ngOnInit(): void {
    console.log('Dashboard component initialized');
    this.currentRole = this.authService.getCurrentRole();
    console.log('Dashboard - Current role:', this.currentRole);
    console.log('Dashboard - Current role type:', typeof this.currentRole);
    console.log('Dashboard - Role === GUEST?', this.currentRole === 'GUEST');
    console.log('Dashboard - Role === USER?', this.currentRole === 'USER');

    // Check if user can access expenses
    this.expenseService.canAccessExpenses().subscribe({
      next: (response) => {
        this.canAccessExpenses = response.data || false;
        console.log('Dashboard - Can access expenses:', this.canAccessExpenses);
      },
      error: (err) => {
        console.error('Dashboard - Error checking expense access:', err);
        this.canAccessExpenses = false;
      },
    });

    // Subscribe to role changes
    this.authService.userRole$.subscribe({
      next: (role: string) => {
        console.log('Dashboard - Role updated:', role);
        this.currentRole = role;
      },
    });
  }
}