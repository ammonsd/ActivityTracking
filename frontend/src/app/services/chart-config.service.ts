/**
 * Description: Chart Configuration service - provides Chart.js configuration and helper functions for generating charts
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { Injectable } from '@angular/core';
import { ChartOptions } from 'chart.js';

@Injectable({
  providedIn: 'root',
})
export class ChartConfigService {
  // Color palette for charts
  private readonly colorPalette = [
    '#1976d2', // Primary blue
    '#dc004e', // Primary pink
    '#9c27b0', // Purple
    '#673ab7', // Deep purple
    '#3f51b5', // Indigo
    '#2196f3', // Blue
    '#00bcd4', // Cyan
    '#009688', // Teal
    '#4caf50', // Green
    '#8bc34a', // Light green
    '#ff9800', // Orange
    '#ff5722', // Deep orange
    '#795548', // Brown
    '#607d8b', // Blue grey
  ];

  constructor() {}

  getColorPalette(): string[] {
    return [...this.colorPalette];
  }

  // Pie Chart Configuration
  getPieChartOptions(title: string): ChartOptions<'pie'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right',
          labels: {
            padding: 15,
            font: {
              size: 12,
            },
          },
        },
        title: {
          display: true,
          text: title,
          font: {
            size: 16,
            weight: 'bold',
          },
          padding: {
            bottom: 20,
          },
        },
        tooltip: {
          callbacks: {
            label: (context) => {
              const label = context.label || '';
              const value = context.parsed || 0;
              const total = (context.dataset.data as number[]).reduce(
                (a: number, b: number) => a + b,
                0
              );
              const percent = ((value / total) * 100).toFixed(1);
              return `${label}: ${value} hrs (${percent}%)`;
            },
          },
        },
      },
    };
  }

  // Donut Chart Configuration
  getDonutChartOptions(title: string): ChartOptions<'doughnut'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right',
          labels: {
            padding: 15,
            font: {
              size: 12,
            },
          },
        },
        title: {
          display: true,
          text: title,
          font: {
            size: 16,
            weight: 'bold',
          },
          padding: {
            bottom: 20,
          },
        },
        tooltip: {
          callbacks: {
            label: (context) => {
              const label = context.label || '';
              const value = context.parsed || 0;
              const total = (context.dataset.data as number[]).reduce(
                (a: number, b: number) => a + b,
                0
              );
              const percent = ((value / total) * 100).toFixed(1);
              return `${label}: ${value} hrs (${percent}%)`;
            },
          },
        },
      },
    };
  }

  // Bar Chart Configuration
  getBarChartOptions(
    title: string,
    stacked: boolean = false
  ): ChartOptions<'bar'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          stacked: stacked,
          grid: {
            display: false,
          },
        },
        y: {
          stacked: stacked,
          beginAtZero: true,
          title: {
            display: true,
            text: 'Hours',
          },
        },
      },
      plugins: {
        legend: {
          display: stacked,
          position: 'top',
        },
        title: {
          display: true,
          text: title,
          font: {
            size: 16,
            weight: 'bold',
          },
          padding: {
            bottom: 20,
          },
        },
        tooltip: {
          callbacks: {
            label: (context) => {
              const label = context.dataset.label || '';
              const value = context.parsed.y || 0;
              return `${label}: ${value} hrs`;
            },
          },
        },
      },
    };
  }

  // Line Chart Configuration
  getLineChartOptions(title: string): ChartOptions<'line'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          grid: {
            display: false,
          },
        },
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: 'Hours',
          },
        },
      },
      plugins: {
        legend: {
          display: true,
          position: 'top',
        },
        title: {
          display: true,
          text: title,
          font: {
            size: 16,
            weight: 'bold',
          },
          padding: {
            bottom: 20,
          },
        },
        tooltip: {
          mode: 'index',
          intersect: false,
        },
      },
      interaction: {
        mode: 'nearest',
        axis: 'x',
        intersect: false,
      },
    };
  }

  // Grouped Bar Chart Configuration (for monthly comparison)
  getGroupedBarChartOptions(title: string): ChartOptions<'bar'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          stacked: false,
          grid: {
            display: false,
          },
        },
        y: {
          stacked: false,
          beginAtZero: true,
          title: {
            display: true,
            text: 'Hours',
          },
        },
      },
      plugins: {
        legend: {
          display: true,
          position: 'top',
        },
        title: {
          display: true,
          text: title,
          font: {
            size: 16,
            weight: 'bold',
          },
          padding: {
            bottom: 20,
          },
        },
      },
    };
  }
}
