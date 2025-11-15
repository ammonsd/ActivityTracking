import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReportsService } from '../../../services/reports.service';
import { WeeklySummaryDto } from '../../../models/report.model';

@Component({
  selector: 'app-weekly-summary',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './weekly-summary.component.html',
  styleUrl: './weekly-summary.component.scss',
})
export class WeeklySummaryComponent implements OnInit {
  loading = false;
  data: WeeklySummaryDto[] = [];
  displayedColumns: string[] = [
    'weekRange',
    'totalHours',
    'topClients',
    'change',
  ];

  constructor(private readonly reportsService: ReportsService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reportsService.getWeeklySummary().subscribe({
      next: (data: WeeklySummaryDto[]) => {
        this.data = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading weekly summary:', err);
        this.loading = false;
      },
    });
  }

  formatWeekRange(start: string, end: string): string {
    return `${new Date(start).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    })} - ${new Date(end).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    })}`;
  }

  getTopClients(week: WeeklySummaryDto): string {
    return week.clients
      .slice(0, 3)
      .map((c) => `${c.client} (${c.hours}h)`)
      .join(', ');
  }
}
