import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';
import { ReportsService } from '../../../services/reports.service';
import { ChartConfigService } from '../../../services/chart-config.service';
import { TimeByClientDto } from '../../../models/report.model';

Chart.register(...registerables);

@Component({
  selector: 'app-time-by-client',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    BaseChartDirective,
  ],
  templateUrl: './time-by-client.component.html',
  styleUrl: './time-by-client.component.scss',
})
export class TimeByClientComponent implements OnInit {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  loading = false;
  chartData: ChartData<'pie'> = {
    labels: [],
    datasets: [
      {
        data: [],
        backgroundColor: [],
      },
    ],
  };
  chartOptions: any;

  constructor(
    private readonly reportsService: ReportsService,
    private readonly chartConfig: ChartConfigService
  ) {}

  ngOnInit(): void {
    this.chartOptions = this.chartConfig.getPieChartOptions(
      'Time Distribution by Client'
    );
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reportsService.getTimeByClient().subscribe({
      next: (data: TimeByClientDto[]) => {
        this.updateChart(data);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading client data:', err);
        this.loading = false;
      },
    });
  }

  updateChart(data: TimeByClientDto[]): void {
    const colors = this.chartConfig.getColorPalette();

    this.chartData = {
      labels: data.map((d) => d.client),
      datasets: [
        {
          data: data.map((d) => d.hours),
          backgroundColor: colors.slice(0, data.length),
        },
      ],
    };

    this.chart?.update();
  }
}
