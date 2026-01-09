/**
 * Description: Monthly Comparison report component - compares hours across multiple months
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartData, registerables } from 'chart.js';
import { ReportsService } from '../../../services/reports.service';
import { ChartConfigService } from '../../../services/chart-config.service';
import { MonthlyComparisonDto } from '../../../models/report.model';

Chart.register(...registerables);

@Component({
  selector: 'app-monthly-comparison',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    BaseChartDirective,
  ],
  templateUrl: './monthly-comparison.component.html',
  styleUrl: './monthly-comparison.component.scss',
})
export class MonthlyComparisonComponent implements OnInit {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  loading = false;
  chartData: ChartData<'bar'> = {
    labels: [],
    datasets: [],
  };
  chartOptions: any;

  constructor(
    private readonly reportsService: ReportsService,
    private readonly chartConfig: ChartConfigService
  ) {}

  ngOnInit(): void {
    this.chartOptions = this.chartConfig.getGroupedBarChartOptions(
      'Monthly Comparison by Client'
    );
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reportsService.getMonthlyComparison().subscribe({
      next: (data: MonthlyComparisonDto[]) => {
        this.updateChart(data);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading monthly comparison:', err);
        this.loading = false;
      },
    });
  }

  updateChart(data: MonthlyComparisonDto[]): void {
    const colors = this.chartConfig.getColorPalette();
    const clients = Array.from(
      new Set(data.flatMap((m) => m.clients.map((c) => c.client)))
    );

    this.chartData = {
      labels: data.map((d) => d.month),
      datasets: clients.map((client, index) => ({
        label: client,
        data: data.map((month) => {
          const clientData = month.clients.find((c) => c.client === client);
          return clientData ? clientData.hours : 0;
        }),
        backgroundColor: colors[index % colors.length],
      })),
    };

    this.chart?.update();
  }
}
