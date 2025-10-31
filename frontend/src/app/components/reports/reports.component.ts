import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  template: `
    <div class="reports-container">
      <mat-card>
        <mat-card-header>
          <mat-icon>analytics</mat-icon>
          <mat-card-title>Reports - Coming Soon</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>
            The Reports section is currently under development. Future reports
            will include:
          </p>
          <ul>
            <li>Hours by Client (pie chart)</li>
            <li>Hours by Project (bar chart)</li>
            <li>Hours by Phase (breakdown)</li>
            <li>Daily/Weekly/Monthly Summary</li>
            <li>Time Trends (line chart)</li>
            <li>User Activity Reports</li>
            <li>Project Status Dashboard</li>
          </ul>
          <p>Check back soon for these analytics features!</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .reports-container {
        padding: 20px;
        max-width: 1200px;
        margin: 0 auto;
      }

      mat-card-header {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 20px;
      }

      mat-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: #1976d2;
      }

      ul {
        line-height: 1.8;
      }

      li {
        margin-bottom: 8px;
      }
    `,
  ],
})
export class ReportsComponent {}
