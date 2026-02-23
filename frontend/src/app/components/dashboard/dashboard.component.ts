/**
 * Description: Dashboard component - displays main dashboard with task and expense summaries
 *
 * Author: Dean Ammons
 * Date: December 2025
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

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
            timesheet. Other features (expenses) are visible but read-only to
            allow exposure to the full application functionality.
          </span>
        </div>
      </div>

      <h2>Task Activity Tracker - User Dashboard</h2>

      <div class="dashboard-grid">
        <mat-card
          class="dashboard-card"
          *ngIf="hasTaskAccess"
          routerLink="/tasks"
        >
          <mat-card-header>
            <mat-icon>assignment</mat-icon>
            <mat-card-title>Task Activities</mat-card-title>
          </mat-card-header>
        </mat-card>

        <mat-card
          *ngIf="hasExpenseAccess"
          class="dashboard-card"
          routerLink="/expenses"
        >
          <mat-card-header>
            <mat-icon>receipt</mat-icon>
            <mat-card-title>Expenses</mat-card-title>
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
              >Welcome to the Task Activity Tracker</mat-card-title
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
        transition:
          transform 0.2s,
          box-shadow 0.2s;
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
  hasTaskAccess = false;
  hasExpenseAccess = false;

  constructor(private readonly authService: AuthService) {}

  ngOnInit(): void {
    console.log('Dashboard component initialized');
    this.currentRole = this.authService.getCurrentRole();

    // Set permission-based card visibility from cached session data
    this.hasTaskAccess = this.authService.hasPermission('TASK_ACTIVITY:READ');
    this.hasExpenseAccess = this.authService.hasPermission('EXPENSE:READ');
    console.log(
      'Dashboard - hasTaskAccess:',
      this.hasTaskAccess,
      '| hasExpenseAccess:',
      this.hasExpenseAccess,
    );

    // Subscribe to role changes for GUEST banner updates
    this.authService.userRole$.subscribe({
      next: (role: string) => {
        this.currentRole = role;
      },
    });

    // Subscribe to permission changes in case API response arrives after init
    this.authService.userPermissions$.subscribe({
      next: (permissions: string[]) => {
        this.hasTaskAccess = permissions.includes('TASK_ACTIVITY:READ');
        this.hasExpenseAccess = permissions.includes('EXPENSE:READ');
        console.log(
          'Dashboard - Permissions updated — hasTaskAccess:',
          this.hasTaskAccess,
          '| hasExpenseAccess:',
          this.hasExpenseAccess,
        );
      },
    });
  }
}
