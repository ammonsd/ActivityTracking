/**
 * Description: Reports component - displays various analytics and reports for task activities.
 * Note: The admin-only "User Analysis" report has been moved to the React Admin Dashboard
 * (Analytics & Reports page) to keep all admin-facing features in one place.
 *
 * Modified by: Dean Ammons - March 2026
 * Change: "Current Week" date range now respects the user's weekStartDay preference
 * (MONDAY = Mon-Sun, SATURDAY = Sat-Fri), matching the Spring Boot weekly timesheet range.
 *
 * Modified by: Dean Ammons - February 2026
 * Change: Removed User Analysis tab (moved to React Admin Dashboard)
 * Reason: Admin should not need to flip between dashboards for admin-specific functionality
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
import { UserService } from '../../services/user.service';

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
  ],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss',
})
export class ReportsComponent implements OnInit {
  startDate: Date | null = null;
  endDate: Date | null = null;

  /** Mirrors the user's weekStartDay preference: JS getDay() value (1 = Monday, 6 = Saturday). */
  private weekStartDayValue = 1; // default: Monday

  constructor(private readonly userService: UserService) {}

  ngOnInit(): void {
    // Load the user's week-start preference before defaulting to currentMonth so that
    // any subsequent click on "Current Week" uses the correct range immediately.
    this.userService.getCurrentUserProfile().subscribe({
      next: (response) => {
        if (response?.data?.weekStartDay === 'SATURDAY') {
          this.weekStartDayValue = 6;
        } else {
          this.weekStartDayValue = 1; // MONDAY or absent → Monday
        }
      },
      error: () => {
        this.weekStartDayValue = 1; // Fall back to Monday on any error
      },
    });
    this.setDateRange('currentMonth');
  }

  setDateRange(range: string): void {
    const today = new Date();

    switch (range) {
      case 'currentWeek': {
        // Calculate the start of the current week using the user's weekStartDay preference.
        // weekStartDayValue: 1 = Monday (Mon-Sun), 6 = Saturday (Sat-Fri).
        const dayOfWeek = today.getDay(); // 0 = Sun, 1 = Mon, ..., 6 = Sat
        const diff = (dayOfWeek - this.weekStartDayValue + 7) % 7;
        this.startDate = new Date(
          today.getFullYear(),
          today.getMonth(),
          today.getDate() - diff,
        );
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
