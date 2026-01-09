/**
 * Description: User Summary report component - displays summary of hours and activities by user
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { ReportsService } from '../../../services/reports.service';
import { UserSummaryDto } from '../../../models/report.model';

@Component({
  selector: 'app-user-summary',
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './user-summary.component.html',
  styleUrl: './user-summary.component.scss',
})
export class UserSummaryComponent implements OnInit {
  userSummaries: UserSummaryDto[] = [];
  loading = false;
  displayedColumns: string[] = [
    'rank',
    'username',
    'totalHours',
    'billableHours',
    'nonBillableHours',
    'taskCount',
    'avgHoursPerDay',
    'topClient',
    'topProject',
    'lastActivityDate',
  ];

  constructor(private readonly reportsService: ReportsService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reportsService.getUserSummaries().subscribe({
      next: (data) => {
        this.userSummaries = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading user summaries:', error);
        this.loading = false;
      },
    });
  }

  getRankIcon(index: number): string {
    if (index === 0) return 'emoji_events'; // Gold trophy
    if (index === 1) return 'workspace_premium'; // Silver medal
    if (index === 2) return 'military_tech'; // Bronze medal
    return 'person';
  }

  getRankClass(index: number): string {
    if (index === 0) return 'rank-1';
    if (index === 1) return 'rank-2';
    if (index === 2) return 'rank-3';
    return '';
  }
}
