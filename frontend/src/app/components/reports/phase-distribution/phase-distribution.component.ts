import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartData, registerables } from 'chart.js';
import { ReportsService } from '../../../services/reports.service';
import { ChartConfigService } from '../../../services/chart-config.service';
import { TimeByPhaseDto } from '../../../models/report.model';

Chart.register(...registerables);

@Component({
  selector: 'app-phase-distribution',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    BaseChartDirective,
  ],
  templateUrl: './phase-distribution.component.html',
  styleUrl: './phase-distribution.component.scss',
})
export class PhaseDistributionComponent implements OnInit {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  loading = false;
  chartData: ChartData<'doughnut'> = {
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
    this.chartOptions =
      this.chartConfig.getDonutChartOptions('Phase Distribution');
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reportsService.getTimeByPhase().subscribe({
      next: (data: TimeByPhaseDto[]) => {
        this.updateChart(data);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading phase data:', err);
        this.loading = false;
      },
    });
  }

  updateChart(data: TimeByPhaseDto[]): void {
    const colors = this.chartConfig.getColorPalette();

    this.chartData = {
      labels: data.map((d) => d.phase),
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
