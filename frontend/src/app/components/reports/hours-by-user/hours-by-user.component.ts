/**
 * Description: Hours by User report component - displays hours worked by each user
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { ReportsService } from '../../../services/reports.service';

@Component({
  selector: 'app-hours-by-user',
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    BaseChartDirective,
  ],
  templateUrl: './hours-by-user.component.html',
  styleUrl: './hours-by-user.component.scss',
})
export class HoursByUserComponent implements OnInit {
  loading = false;

  barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [],
  };

  barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'top',
      },
      tooltip: {
        callbacks: {
          label: (context) => {
            const label = context.dataset.label || '';
            const value = context.parsed.y;
            const dataIndex = context.dataIndex;
            const percentage = this.barChartData.datasets[0].data[
              dataIndex
            ] as any;
            return `${label}: ${value}h (${percentage?.percentage?.toFixed(
              1
            )}%)`;
          },
        },
      },
    },
    scales: {
      x: {
        title: {
          display: true,
          text: 'User',
        },
      },
      y: {
        title: {
          display: true,
          text: 'Hours',
        },
        beginAtZero: true,
      },
    },
  };

  constructor(private readonly reportsService: ReportsService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reportsService.getHoursByUser().subscribe({
      next: (data) => {
        const sortedData = [...data].sort((a, b) => b.hours - a.hours);

        this.barChartData = {
          labels: sortedData.map((d) => d.username),
          datasets: [
            {
              label: 'Hours Worked',
              data: sortedData.map(
                (d) =>
                  ({
                    x: d.username,
                    y: d.hours,
                    percentage: d.percentage,
                  } as any)
              ),
              backgroundColor: this.generateColors(sortedData.length),
              borderColor: this.generateColors(sortedData.length, 0.8),
              borderWidth: 1,
            },
          ],
        };
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading hours by user:', error);
        this.loading = false;
      },
    });
  }

  private generateColors(count: number, alpha: number = 0.6): string[] {
    const colors = [];
    for (let i = 0; i < count; i++) {
      const hue = (i * 360) / count;
      colors.push(`hsla(${hue}, 70%, 60%, ${alpha})`);
    }
    return colors;
  }
}
