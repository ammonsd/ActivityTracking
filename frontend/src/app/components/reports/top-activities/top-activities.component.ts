import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReportsService } from '../../../services/reports.service';
import { TopActivityDto } from '../../../models/report.model';

@Component({
  selector: 'app-top-activities',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatListModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './top-activities.component.html',
  styleUrl: './top-activities.component.scss',
})
export class TopActivitiesComponent implements OnInit {
  loading = false;
  activities: TopActivityDto[] = [];
  maxHours = 0;

  constructor(private readonly reportsService: ReportsService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reportsService.getTopActivities().subscribe({
      next: (data: TopActivityDto[]) => {
        this.activities = data;
        this.maxHours = Math.max(...data.map((a) => a.hours));
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading top activities:', err);
        this.loading = false;
      },
    });
  }

  getProgressPercentage(hours: number): number {
    return (hours / this.maxHours) * 100;
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }
}
