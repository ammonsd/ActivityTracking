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
            The Analytics & Reports section is currently under development.
            Future reports will include:
          </p>
          <ul>
            <li>
              <strong>Time Distribution by Client</strong> - Pie chart showing
              percentage of time per client
            </li>
            <li>
              <strong>Time Distribution by Project</strong> - Bar chart with
              hours by project and phase breakdown
            </li>
            <li>
              <strong>Daily Time Tracking</strong> - Line chart showing daily
              hours worked with trends
            </li>
            <li>
              <strong>Phase Distribution</strong> - Donut chart of time spent in
              different project phases
            </li>
            <li>
              <strong>Weekly Summary</strong> - Weekly breakdown with
              client/project details and trends
            </li>
            <li>
              <strong>Top Activities</strong> - Most time-consuming tasks with
              progress indicators
            </li>
            <li>
              <strong>Monthly Comparison</strong> - Compare hours across
              multiple months by client
            </li>
            <li>
              <strong>Personal Dashboard</strong> - KPI cards with key metrics
              (month/week hours, top clients, averages)
            </li>
          </ul>
          <p>Check back soon for these developer productivity analytics!</p>
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
