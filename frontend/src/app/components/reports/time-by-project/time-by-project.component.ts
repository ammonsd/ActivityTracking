import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartData, registerables } from 'chart.js';
import { ReportsService } from '../../../services/reports.service';
import { ChartConfigService } from '../../../services/chart-config.service';
import { TimeByProjectDto } from '../../../models/report.model';

Chart.register(...registerables);

@Component({
  selector: 'app-time-by-project',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    BaseChartDirective,
  ],
  templateUrl: './time-by-project.component.html',
  styleUrl: './time-by-project.component.scss',
})
export class TimeByProjectComponent implements OnInit {
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
    this.chartOptions = this.chartConfig.getBarChartOptions(
      'Time Distribution by Project',
      true
    );
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reportsService.getTimeByProject().subscribe({
      next: (data: TimeByProjectDto[]) => {
        this.updateChart(data);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading project data:', err);
        this.loading = false;
      },
    });
  }

  updateChart(data: TimeByProjectDto[]): void {
    const colors = this.chartConfig.getColorPalette();
    const phases = Array.from(
      new Set(data.flatMap((p) => p.phases.map((ph) => ph.phase)))
    );

    this.chartData = {
      labels: data.map((d) => d.project),
      datasets: phases.map((phase, index) => ({
        label: phase,
        data: data.map((project) => {
          const phaseData = project.phases.find((p) => p.phase === phase);
          return phaseData ? phaseData.hours : 0;
        }),
        backgroundColor: colors[index % colors.length],
      })),
    };

    this.chart?.update();
  }
}
