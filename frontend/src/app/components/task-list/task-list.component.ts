import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { TaskActivityService } from '../../services/task-activity.service';
import { AuthService } from '../../services/auth.service';
import { TaskActivity } from '../../models/task-activity.model';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  template: `
    <div class="task-list-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Task Activities</mat-card-title>
          <div class="user-info">
            <span class="current-user">{{ currentUser }}</span>
            <span class="current-date">{{
              currentDate | date : 'M/d/yy'
            }}</span>
          </div>
        </mat-card-header>
        <mat-card-content>
          <!-- Filter Section -->
          <div class="filter-section">
            <mat-form-field appearance="outline">
              <mat-label>Client</mat-label>
              <mat-select
                [(ngModel)]="selectedClient"
                (selectionChange)="applyFilters()"
              >
                <mat-option value="">All Clients</mat-option>
                <mat-option
                  *ngFor="let client of uniqueClients"
                  [value]="client"
                >
                  {{ client }}
                </mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Project</mat-label>
              <mat-select
                [(ngModel)]="selectedProject"
                (selectionChange)="applyFilters()"
              >
                <mat-option value="">All Projects</mat-option>
                <mat-option
                  *ngFor="let project of uniqueProjects"
                  [value]="project"
                >
                  {{ project }}
                </mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Phase</mat-label>
              <mat-select
                [(ngModel)]="selectedPhase"
                (selectionChange)="applyFilters()"
              >
                <mat-option value="">All Phases</mat-option>
                <mat-option *ngFor="let phase of uniquePhases" [value]="phase">
                  {{ phase }}
                </mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Start Date</mat-label>
              <input
                matInput
                [matDatepicker]="startPicker"
                [(ngModel)]="startDate"
                (dateChange)="applyFilters()"
                placeholder="MM/DD/YYYY"
              />
              <mat-datepicker-toggle
                matSuffix
                [for]="startPicker"
              ></mat-datepicker-toggle>
              <mat-datepicker #startPicker></mat-datepicker>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>End Date</mat-label>
              <input
                matInput
                [matDatepicker]="endPicker"
                [(ngModel)]="endDate"
                (dateChange)="applyFilters()"
                placeholder="MM/DD/YYYY"
              />
              <mat-datepicker-toggle
                matSuffix
                [for]="endPicker"
              ></mat-datepicker-toggle>
              <mat-datepicker #endPicker></mat-datepicker>
            </mat-form-field>
          </div>

          <div class="table-actions">
            <button mat-raised-button color="accent" (click)="clearFilters()">
              <mat-icon>clear</mat-icon> Clear
            </button>
            <button mat-raised-button color="primary" (click)="loadTasks()">
              <mat-icon>refresh</mat-icon> Refresh
            </button>
          </div>

          <div *ngIf="loading" class="loading-spinner">
            <mat-spinner></mat-spinner>
          </div>

          <div *ngIf="error" class="error-message">
            {{ error }}
          </div>

          <table
            mat-table
            [dataSource]="filteredTasks"
            *ngIf="!loading && !error"
            class="task-table"
          >
            <ng-container matColumnDef="taskDate">
              <th mat-header-cell *matHeaderCellDef>Date</th>
              <td mat-cell *matCellDef="let task">
                {{ task.taskDate | date : 'M/d/yy' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="client">
              <th mat-header-cell *matHeaderCellDef>Client</th>
              <td mat-cell *matCellDef="let task">{{ task.client }}</td>
            </ng-container>

            <ng-container matColumnDef="project">
              <th mat-header-cell *matHeaderCellDef>Project</th>
              <td mat-cell *matCellDef="let task">{{ task.project }}</td>
            </ng-container>

            <ng-container matColumnDef="phase">
              <th mat-header-cell *matHeaderCellDef>Phase</th>
              <td mat-cell *matCellDef="let task">{{ task.phase }}</td>
            </ng-container>

            <ng-container matColumnDef="hours">
              <th mat-header-cell *matHeaderCellDef>Hours</th>
              <td mat-cell *matCellDef="let task">{{ task.hours }}</td>
            </ng-container>

            <ng-container matColumnDef="details">
              <th mat-header-cell *matHeaderCellDef>Details</th>
              <td mat-cell *matCellDef="let task">{{ task.details }}</td>
            </ng-container>

            <ng-container matColumnDef="username">
              <th mat-header-cell *matHeaderCellDef>Username</th>
              <td mat-cell *matCellDef="let task">{{ task.username }}</td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let task" class="actions-cell">
                <button
                  mat-icon-button
                  color="primary"
                  (click)="editTask(task)"
                  title="Edit Task"
                >
                  <mat-icon>edit</mat-icon>
                </button>
                <button
                  mat-icon-button
                  color="warn"
                  (click)="deleteTask(task)"
                  title="Delete Task"
                >
                  <mat-icon>delete</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>

          <div
            *ngIf="filteredTasks.length === 0 && !loading && !error"
            class="no-data"
          >
            <p>No tasks found matching the selected filters.</p>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .task-list-container {
        padding: 20px;
        max-width: 1400px;
        margin: 0 auto;
      }

      mat-card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
      }

      .user-info {
        display: flex;
        gap: 20px;
        font-size: 14px;
      }

      .current-user {
        font-weight: 500;
      }

      .filter-section {
        display: flex;
        gap: 15px;
        margin-bottom: 20px;
        align-items: center;
        flex-wrap: wrap;
      }

      .filter-section mat-form-field {
        min-width: 200px;
      }

      .table-actions {
        margin-bottom: 20px;
        display: flex;
        gap: 10px;
        justify-content: flex-end;
      }

      .task-table {
        width: 100%;
        margin-top: 20px;
      }

      .loading-spinner {
        display: flex;
        justify-content: center;
        padding: 40px;
      }

      .error-message {
        color: #f44336;
        padding: 20px;
        background-color: #ffebee;
        border-radius: 4px;
        margin: 20px 0;
      }

      .no-data {
        text-align: center;
        padding: 40px;
        color: #666;
      }

      th {
        font-weight: bold;
        background-color: #f5f5f5;
      }

      .actions-cell {
        white-space: nowrap;
      }

      mat-icon {
        font-size: 20px;
      }
    `,
  ],
})
export class TaskListComponent implements OnInit {
  tasks: TaskActivity[] = [];
  filteredTasks: TaskActivity[] = [];
  displayedColumns: string[] = [];
  loading = false;
  error: string | null = null;
  currentUser = '';
  currentRole = '';
  currentDate = new Date();

  // Filter properties
  selectedClient = '';
  selectedProject = '';
  selectedPhase = '';
  startDate: Date | null = null;
  endDate: Date | null = null;
  uniqueClients: string[] = [];
  uniqueProjects: string[] = [];
  uniquePhases: string[] = [];

  constructor(
    private readonly taskService: TaskActivityService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUsername();
    this.currentRole = this.authService.getCurrentRole();

    // Set displayed columns based on role
    // ADMIN sees username column, others don't
    if (this.currentRole === 'ADMIN') {
      this.displayedColumns = [
        'taskDate',
        'client',
        'project',
        'phase',
        'hours',
        'details',
        'username',
        'actions',
      ];
    } else {
      this.displayedColumns = [
        'taskDate',
        'client',
        'project',
        'phase',
        'hours',
        'details',
        'actions',
      ];
    }

    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.error = null;

    this.taskService.getAllTasks().subscribe({
      next: (response) => {
        // Handle ApiResponse wrapper - data is in response.data
        this.tasks = response.data || [];

        // Sort by date descending (newest first)
        this.tasks.sort((a, b) => {
          const dateA = new Date(a.taskDate).getTime();
          const dateB = new Date(b.taskDate).getTime();
          return dateB - dateA;
        });

        // Extract unique values for filters
        this.uniqueClients = [
          ...new Set(this.tasks.map((t) => t.client)),
        ].sort();
        this.uniqueProjects = [
          ...new Set(this.tasks.map((t) => t.project)),
        ].sort();
        this.uniquePhases = [...new Set(this.tasks.map((t) => t.phase))].sort();

        // Apply filters
        this.applyFilters();

        this.loading = false;
        console.log('Loaded tasks:', this.tasks);
      },
      error: (err) => {
        console.error('Error loading tasks:', err);
        this.error =
          'Failed to load tasks. Make sure the Spring Boot backend is running.';
        this.loading = false;
      },
    });
  }

  applyFilters(): void {
    this.filteredTasks = this.tasks.filter((task) => {
      const clientMatch =
        !this.selectedClient || task.client === this.selectedClient;
      const projectMatch =
        !this.selectedProject || task.project === this.selectedProject;
      const phaseMatch =
        !this.selectedPhase || task.phase === this.selectedPhase;

      // Date filtering
      const taskDate = new Date(task.taskDate);
      const startMatch = !this.startDate || taskDate >= this.startDate;
      const endMatch = !this.endDate || taskDate <= this.endDate;

      return (
        clientMatch && projectMatch && phaseMatch && startMatch && endMatch
      );
    });
  }

  clearFilters(): void {
    this.selectedClient = '';
    this.selectedProject = '';
    this.selectedPhase = '';
    this.startDate = null;
    this.endDate = null;
    this.applyFilters();
  }

  editTask(task: TaskActivity): void {
    console.log('Edit task:', task);
    alert(`Edit functionality not yet implemented`);
    // TODO: Implement edit dialog/form
  }

  deleteTask(task: TaskActivity): void {
    if (
      confirm(
        `Are you sure you want to delete this task from ${task.taskDate}?`
      )
    ) {
      this.taskService.deleteTask(task.id!).subscribe({
        next: () => {
          console.log('Task deleted successfully');
          this.loadTasks(); // Reload the list
        },
        error: (err) => {
          console.error('Error deleting task:', err);
          alert('Failed to delete task. You may not have permission.');
        },
      });
    }
  }
}
