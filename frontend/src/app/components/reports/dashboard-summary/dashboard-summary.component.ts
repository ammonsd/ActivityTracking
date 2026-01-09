/**
 * Description: Dashboard Summary report component - displays summary statistics for the dashboard
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReportsService } from '../../../services/reports.service';
import { DashboardSummaryDto } from '../../../models/report.model';

@Component({
  selector: 'app-dashboard-summary',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './dashboard-summary.component.html',
  styleUrl: './dashboard-summary.component.scss',
})
export class DashboardSummaryComponent implements OnInit {
  summary: DashboardSummaryDto | null = null;
  loading = false;

  constructor(private reportsService: ReportsService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reportsService.getDashboardSummary().subscribe({
      next: (data) => {
        this.summary = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading dashboard summary:', err);
        this.loading = false;
      },
    });
  }
}
