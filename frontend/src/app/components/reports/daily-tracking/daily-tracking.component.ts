/**
 * Description: Daily Tracking report component - displays daily task activity summary
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
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartData, registerables } from 'chart.js';
import { ReportsService } from '../../../services/reports.service';
import { ChartConfigService } from '../../../services/chart-config.service';
import { DailyHoursDto } from '../../../models/report.model';

Chart.register(...registerables);

@Component({
  selector: 'app-daily-tracking',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    BaseChartDirective,
  ],
  templateUrl: './daily-tracking.component.html',
  styleUrl: './daily-tracking.component.scss',
})
export class DailyTrackingComponent implements OnInit, OnChanges {
  @Input() startDate: Date | null = null;
  @Input() endDate: Date | null = null;
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  loading = false;
  chartData: ChartData<'line'> = {
    labels: [],
    datasets: [],
  };
  chartOptions: any;

  constructor(
    private readonly reportsService: ReportsService,
    private readonly chartConfig: ChartConfigService,
  ) {}

  ngOnInit(): void {
    this.chartOptions = this.chartConfig.getLineChartOptions(
      'Daily Time Tracking',
    );
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
      .getDailyHours(this.startDate ?? undefined, this.endDate ?? undefined)
      .subscribe({
        next: (data: DailyHoursDto[]) => {
          this.updateChart(data);
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading daily hours:', err);
          this.loading = false;
        },
      });
  }

  updateChart(data: DailyHoursDto[]): void {
    this.chartData = {
      labels: data.map((d) =>
        new Date(d.date).toLocaleDateString('en-US', {
          month: 'short',
          day: 'numeric',
        }),
      ),
      datasets: [
        {
          label: 'Hours Worked',
          data: data.map((d) => d.hours),
          borderColor: '#1976d2',
          backgroundColor: 'rgba(25, 118, 210, 0.1)',
          tension: 0.4,
          fill: true,
        },
        {
          label: 'Target (8 hrs)',
          data: data.map(() => 8),
          borderColor: '#dc004e',
          borderDash: [5, 5],
          pointRadius: 0,
          fill: false,
        },
      ],
    };

    this.chart?.update();
  }
}
