/**
 * Description: Dashboard Summary report component - displays summary statistics for the dashboard
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import {
  Component,
  OnInit,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
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
export class DashboardSummaryComponent implements OnInit, OnChanges {
  @Input() startDate: Date | null = null;
  @Input() endDate: Date | null = null;

  summary: DashboardSummaryDto | null = null;
  loading = false;

  constructor(private reportsService: ReportsService) {}

  get periodLabel(): string {
    if (this.startDate && this.endDate) {
      return 'Selected Period';
    }
    return 'Current Month';
  }

  ngOnInit(): void {
    this.loadData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['startDate'] || changes['endDate']) {
      this.loadData();
    }
  }

  loadData(): void {
    this.loading = true;
    this.reportsService
      .getDashboardSummary(
        this.startDate ?? undefined,
        this.endDate ?? undefined,
      )
      .subscribe({
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
