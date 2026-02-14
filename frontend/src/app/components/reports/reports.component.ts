/**
 * Description: Reports component - displays various analytics and reports for task activities
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { DashboardSummaryComponent } from './dashboard-summary/dashboard-summary.component';
import { TimeByClientComponent } from './time-by-client/time-by-client.component';
import { TimeByProjectComponent } from './time-by-project/time-by-project.component';
import { DailyTrackingComponent } from './daily-tracking/daily-tracking.component';
import { PhaseDistributionComponent } from './phase-distribution/phase-distribution.component';
import { WeeklySummaryComponent } from './weekly-summary/weekly-summary.component';
import { TopActivitiesComponent } from './top-activities/top-activities.component';
import { MonthlyComparisonComponent } from './monthly-comparison/monthly-comparison.component';
import { UserSummaryComponent } from './user-summary/user-summary.component';
import { HoursByUserComponent } from './hours-by-user/hours-by-user.component';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTabsModule,
    MatIconModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
    DashboardSummaryComponent,
    TimeByClientComponent,
    TimeByProjectComponent,
    DailyTrackingComponent,
    PhaseDistributionComponent,
    WeeklySummaryComponent,
    TopActivitiesComponent,
    MonthlyComparisonComponent,
    UserSummaryComponent,
    HoursByUserComponent,
  ],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss',
})
export class ReportsComponent implements OnInit {
  isAdmin = false;
  startDate: Date | null = null;
  endDate: Date | null = null;

  constructor(private readonly authService: AuthService) {}

  ngOnInit(): void {
    const role = this.authService.getCurrentRole();
    this.isAdmin = role === 'ADMIN';
    this.setDateRange('currentMonth');
  }

  setDateRange(range: string): void {
    const today = new Date();

    switch (range) {
      case 'currentWeek': {
        // Calculate Monday of current week (week starts on Monday)
        const dayOfWeek = today.getDay();
        const diff = dayOfWeek === 0 ? -6 : 1 - dayOfWeek; // If Sunday (0), go back 6 days; otherwise go back to Monday
        this.startDate = new Date(
          today.getFullYear(),
          today.getMonth(),
          today.getDate() + diff,
        );
        // Calculate Sunday of current week
        this.endDate = new Date(
          this.startDate.getFullYear(),
          this.startDate.getMonth(),
          this.startDate.getDate() + 6,
        );
        break;
      }
      case 'currentMonth':
        this.startDate = new Date(today.getFullYear(), today.getMonth(), 1);
        this.endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
        break;
      case 'lastMonth':
        this.startDate = new Date(today.getFullYear(), today.getMonth() - 1, 1);
        this.endDate = new Date(today.getFullYear(), today.getMonth(), 0);
        break;
      case 'last3Months':
        this.startDate = new Date(today.getFullYear(), today.getMonth() - 2, 1);
        this.endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
        break;
      case 'currentYear':
        this.startDate = new Date(today.getFullYear(), 0, 1);
        this.endDate = new Date(today.getFullYear(), 11, 31);
        break;
      case 'allTime':
        this.startDate = null;
        this.endDate = null;
        break;
    }
  }

  clearDateRange(): void {
    this.startDate = null;
    this.endDate = null;
  }
}
