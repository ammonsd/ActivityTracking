/**
 * Description: Reports component - displays various analytics and reports for task activities
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
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
    MatCardModule,
    MatTabsModule,
    MatIconModule,
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

  constructor(private readonly authService: AuthService) {}

  ngOnInit(): void {
    const role = this.authService.getCurrentRole();
    this.isAdmin = role === 'ADMIN';
  }
}
