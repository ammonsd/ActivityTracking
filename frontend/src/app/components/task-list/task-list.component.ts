/**
 * Description: Task List component - displays and manages task activities with filtering and editing capabilities
 *
 * Author: Dean Ammons
 * Date: December 2025
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
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
import { MatDialog } from '@angular/material/dialog';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TaskActivityService } from '../../services/task-activity.service';
import { AuthService } from '../../services/auth.service';
import { ReportsService } from '../../services/reports.service';
import { TaskActivity } from '../../models/task-activity.model';
import { TaskEditDialogComponent } from '../task-edit-dialog/task-edit-dialog.component';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { CsvExportDialogComponent } from '../csv-export-dialog/csv-export-dialog.component';

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
    MatSidenavModule,
    MatListModule,
    MatTooltipModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="task-list-wrapper">
      <!-- Main Content Area -->
      <div class="main-content">
        <mat-card>
          <mat-card-header>
            <mat-card-title>Task Activities</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <!-- Action Buttons Section -->
            <div class="action-buttons">
              <button
                mat-raised-button
                color="primary"
                (click)="navigateToDashboard()"
              >
                <mat-icon>dashboard</mat-icon> Dashboard
              </button>
              <button
                mat-raised-button
                color="accent"
                (click)="navigateToExpenses()"
                *ngIf="canAccessExpenses || currentRole === 'GUEST'"
                [disabled]="currentRole === 'GUEST'"
                [matTooltip]="
                  currentRole === 'GUEST'
                    ? 'Read-only for guests'
                    : 'View expense tracking'
                "
              >
                <mat-icon>receipt</mat-icon> Expenses
              </button>
              <button mat-raised-button color="primary" (click)="addTask()">
                <mat-icon>add</mat-icon> Add New Task
              </button>
            </div>
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
                  <mat-option
                    *ngFor="let phase of uniquePhases"
                    [value]="phase"
                  >
                    {{ phase }}
                  </mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Task ID</mat-label>
                <input
                  matInput
                  [(ngModel)]="selectedTaskId"
                  (keyup.enter)="applyFilters()"
                  (blur)="applyFilters()"
                  maxlength="10"
                  placeholder="e.g. TA-001"
                />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Start Date</mat-label>
                <input
                  matInput
                  [matDatepicker]="startPicker"
                  [(ngModel)]="startDate"
                  (dateChange)="applyFilters()"
                  (click)="startPicker.open()"
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
                  (click)="endPicker.open()"
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
              <button mat-raised-button color="primary" (click)="addTask()">
                <mat-icon>add</mat-icon> Add Task
              </button>
              <button mat-raised-button color="accent" (click)="clearFilters()">
                <mat-icon>clear</mat-icon> Clear Filters
              </button>
              <button
                mat-raised-button
                color="primary"
                (click)="exportToCSV()"
                [disabled]="currentRole === 'GUEST'"
                [matTooltip]="
                  currentRole === 'GUEST'
                    ? 'Export disabled for guests'
                    : 'Export filtered tasks to CSV'
                "
              >
                <mat-icon>file_download</mat-icon> Export CSV
              </button>
            </div>

            <!-- Pagination Controls -->
            <div *ngIf="totalElements > 0 && !loading" class="pagination">
              <button
                *ngIf="totalPages > 1"
                mat-icon-button
                [disabled]="currentPage === 0"
                (click)="goToPage(0)"
                title="First Page"
              >
                <mat-icon>first_page</mat-icon>
              </button>
              <button
                *ngIf="totalPages > 1"
                mat-icon-button
                [disabled]="currentPage === 0"
                (click)="goToPage(currentPage - 1)"
                title="Previous Page"
              >
                <mat-icon>chevron_left</mat-icon>
              </button>

              <span class="pagination-info">
                {{ startEntry }} to {{ endEntry }} of {{ totalElements }}
              </span>

              <button
                *ngIf="totalPages > 1"
                mat-icon-button
                [disabled]="currentPage === totalPages - 1"
                (click)="goToPage(currentPage + 1)"
                title="Next Page"
              >
                <mat-icon>chevron_right</mat-icon>
              </button>
              <button
                *ngIf="totalPages > 1"
                mat-icon-button
                [disabled]="currentPage === totalPages - 1"
                (click)="goToPage(totalPages - 1)"
                title="Last Page"
              >
                <mat-icon>last_page</mat-icon>
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
                  {{ task.taskDate | date: 'M/d/yy' }}
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
                <td
                  mat-cell
                  *matCellDef="let task"
                  [ngStyle]="{
                    color: isTaskBillable(task) ? 'inherit' : 'red',
                    'font-weight': isTaskBillable(task) ? 'normal' : 'bold',
                  }"
                >
                  {{ task.hours }}
                </td>
              </ng-container>

              <ng-container matColumnDef="taskId">
                <th mat-header-cell *matHeaderCellDef>Task ID</th>
                <td
                  mat-cell
                  *matCellDef="let task"
                  style="white-space: nowrap;"
                >
                  {{ task.taskId }}
                </td>
              </ng-container>

              <ng-container matColumnDef="taskName">
                <th mat-header-cell *matHeaderCellDef>Task Name</th>
                <td
                  mat-cell
                  *matCellDef="let task"
                  style="max-width: 200px; overflow-wrap: break-word; word-wrap: break-word;"
                >
                  {{ task.taskName }}
                </td>
              </ng-container>

              <ng-container matColumnDef="details">
                <th mat-header-cell *matHeaderCellDef>Details</th>
                <td
                  mat-cell
                  *matCellDef="let task"
                  style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px;"
                >
                  {{ task.details }}
                </td>
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
                    color="accent"
                    (click)="cloneTask(task)"
                    title="Clone Task"
                  >
                    <mat-icon>content_copy</mat-icon>
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

      <!-- Right Sidebar Menu -->
      <div
        class="sidebar-menu"
        *ngIf="currentRole === 'ADMIN' || showSidebarForNonAdmin"
      >
        <button
          mat-icon-button
          class="sidebar-toggle"
          (click)="sidebarOpen = !sidebarOpen"
          [matTooltip]="sidebarOpen ? 'Close menu' : 'Open menu'"
        >
          <mat-icon>{{ sidebarOpen ? 'close' : 'menu' }}</mat-icon>
        </button>

        <div class="sidebar-content" [class.open]="sidebarOpen">
          <div class="sidebar-header">
            <h3>Admin Menu</h3>
          </div>

          <mat-nav-list>
            <!-- Manage Users - ADMIN only or read-only for non-admin -->
            <a
              mat-list-item
              (click)="navigateToManageUsers()"
              [class.disabled]="currentRole !== 'ADMIN'"
              [matTooltip]="
                currentRole === 'ADMIN'
                  ? 'Manage system users'
                  : 'Read-only access'
              "
            >
              <mat-icon matListItemIcon>people</mat-icon>
              <span matListItemTitle>Manage Users</span>
            </a>

            <!-- Guest Activity - ADMIN only -->
            <a
              mat-list-item
              (click)="navigateToGuestActivity()"
              *ngIf="currentRole === 'ADMIN'"
              matTooltip="View guest user activity"
            >
              <mat-icon matListItemIcon>analytics</mat-icon>
              <span matListItemTitle>Guest Activity</span>
            </a>

            <!-- Manage Dropdowns - ADMIN only or read-only for non-admin -->
            <a
              mat-list-item
              (click)="navigateToManageDropdowns()"
              [class.disabled]="currentRole !== 'ADMIN'"
              [matTooltip]="
                currentRole === 'ADMIN'
                  ? 'Manage dropdown options'
                  : 'Read-only access'
              "
            >
              <mat-icon matListItemIcon>settings</mat-icon>
              <span matListItemTitle>Manage Dropdowns</span>
            </a>

            <!-- Export CSV - Show for all if non-admin access is enabled -->
            <a
              mat-list-item
              (click)="exportToCSV()"
              *ngIf="showSidebarForNonAdmin || currentRole === 'ADMIN'"
              [class.disabled]="currentRole === 'GUEST'"
              [matTooltip]="
                currentRole === 'GUEST'
                  ? 'Export disabled for guests'
                  : 'Export tasks to CSV'
              "
            >
              <mat-icon matListItemIcon>file_download</mat-icon>
              <span matListItemTitle>Export CSV</span>
            </a>
          </mat-nav-list>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .task-list-wrapper {
        display: flex;
        height: calc(100vh - 120px);
        position: relative;
      }

      .main-content {
        flex: 1;
        padding: 20px;
        max-width: 1400px;
        margin: 0 auto;
        overflow-y: auto;
      }

      .action-buttons {
        display: flex;
        gap: 10px;
        margin-bottom: 20px;
        flex-wrap: wrap;
      }

      .action-buttons button {
        display: flex;
        align-items: center;
        gap: 5px;
      }

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
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
        gap: 10px;
        margin-bottom: 20px;
        align-items: end;
      }

      .filter-section mat-form-field {
        min-width: 0;
      }

      .table-actions {
        display: flex;
        gap: 8px;
        align-items: flex-end;
        grid-column: -1;
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
        text-align: right;
        padding: 4px 8px !important;
      }

      .actions-cell button {
        margin: 0 1px;
        min-width: 36px;
        padding: 4px 8px;
      }

      mat-icon {
        font-size: 20px;
      }

      .pagination {
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 4px;
        padding: 12px 0;
        margin-bottom: 10px;
      }

      .pagination-info {
        margin: 0 12px;
        font-size: 14px;
        color: #333;
        font-weight: bold;
      }

      /* Sidebar Styles */
      .sidebar-menu {
        position: fixed;
        right: 0;
        top: 64px;
        height: calc(100vh - 64px);
        z-index: 1000;
        pointer-events: none;
      }

      .sidebar-toggle {
        position: fixed;
        right: 10px;
        top: 80px;
        background-color: #3f51b5;
        color: white;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
        z-index: 1001;
        pointer-events: auto;
      }

      .sidebar-toggle:hover {
        background-color: #303f9f;
      }

      .sidebar-content {
        width: 0;
        height: 100%;
        background-color: #fafafa;
        box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
        overflow: hidden;
        transition: width 0.3s ease;
        pointer-events: auto;
      }

      .sidebar-content.open {
        width: 280px;
      }

      .sidebar-header {
        padding: 20px;
        background-color: #3f51b5;
        color: white;
      }

      .sidebar-header h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 500;
      }

      .sidebar-content mat-nav-list {
        padding-top: 8px;
      }

      .sidebar-content a[mat-list-item] {
        cursor: pointer;
        min-height: 48px;
      }

      .sidebar-content a[mat-list-item]:hover:not(.disabled) {
        background-color: #e8eaf6;
      }

      .sidebar-content a[mat-list-item].disabled {
        opacity: 0.5;
        cursor: not-allowed;
        pointer-events: none;
      }

      .sidebar-content mat-icon {
        color: #3f51b5;
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
  canAccessExpenses = false;
  sidebarOpen = false;
  showSidebarForNonAdmin = false; // Set to true to show sidebar for non-admin roles

  // Pagination properties
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Filter properties
  selectedClient = '';
  selectedProject = '';
  selectedPhase = '';
  selectedTaskId = '';
  startDate: Date | null = null;
  endDate: Date | null = null;
  uniqueClients: string[] = [];
  uniqueProjects: string[] = [];
  uniquePhases: string[] = [];

  // Computed properties for pagination display
  get startEntry(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get endEntry(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  constructor(
    private readonly taskService: TaskActivityService,
    private readonly authService: AuthService,
    private readonly reportsService: ReportsService,
    private readonly dialog: MatDialog,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar,
    private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUsername();
    this.currentRole = this.authService.getCurrentRole();

    // Check expense access
    this.checkExpenseAccess();

    // Set displayed columns based on role
    // ADMIN sees username column, others don't
    if (this.currentRole === 'ADMIN') {
      this.displayedColumns = [
        'taskDate',
        'client',
        'project',
        'phase',
        'hours',
        'taskId',
        'taskName',
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
        'taskId',
        'taskName',
        'details',
        'actions',
      ];
    }

    this.loadTasks();
  }

  checkExpenseAccess(): void {
    // Import ExpenseService if needed and check access
    // For now, set based on role
    this.canAccessExpenses =
      this.currentRole === 'ADMIN' ||
      this.currentRole === 'USER' ||
      this.currentRole === 'EXPENSE_ADMIN';
  }

  // Navigation methods
  navigateToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  navigateToExpenses(): void {
    if (this.currentRole !== 'GUEST') {
      this.router.navigate(['/expenses']);
    }
  }

  navigateToManageUsers(): void {
    if (this.currentRole === 'ADMIN') {
      // Navigate to the Java/Thymeleaf manage users page
      globalThis.location.href = '/task-activity/manage-users';
    }
  }

  navigateToGuestActivity(): void {
    if (this.currentRole === 'ADMIN') {
      // Navigate to the Java/Thymeleaf guest activity page
      globalThis.location.href = '/task-activity/manage-users/guest-activity';
    }
  }

  navigateToManageDropdowns(): void {
    if (this.currentRole === 'ADMIN') {
      this.router.navigate(['/dropdowns']);
    }
  }

  exportToCSV(): void {
    if (this.currentRole === 'GUEST') {
      return;
    }

    const params = new URLSearchParams();

    if (this.selectedClient) params.append('client', this.selectedClient);
    if (this.selectedProject) params.append('project', this.selectedProject);
    if (this.selectedPhase) params.append('phase', this.selectedPhase);
    if (this.selectedTaskId) params.append('taskId', this.selectedTaskId);
    if (this.startDate) {
      const year = this.startDate.getFullYear();
      const month = String(this.startDate.getMonth() + 1).padStart(2, '0');
      const day = String(this.startDate.getDate()).padStart(2, '0');
      params.append('startDate', `${year}-${month}-${day}`);
    }
    if (this.endDate) {
      const year = this.endDate.getFullYear();
      const month = String(this.endDate.getMonth() + 1).padStart(2, '0');
      const day = String(this.endDate.getDate()).padStart(2, '0');
      params.append('endDate', `${year}-${month}-${day}`);
    }

    const exportUrl = `/task-activity/list/export-csv?${params.toString()}`;
    this.http.get(exportUrl, { responseType: 'text' }).subscribe({
      next: (csvData) => {
        this.dialog.open(CsvExportDialogComponent, {
          data: {
            title: 'Export Task Activity List to CSV',
            csvData,
            filename: 'task-activity-export.csv',
          },
          width: '700px',
          maxWidth: '95vw',
        });
      },
      error: () => {
        this.snackBar.open('Failed to export CSV. Please try again.', 'Close', {
          duration: 4000,
        });
      },
    });
  }

  loadTasks(): void {
    this.loading = true;
    this.error = null;

    // Format dates for API (without timezone conversion)
    const formatDate = (date: Date | null): string | undefined => {
      if (!date) return undefined;
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    const startDateStr = formatDate(this.startDate);
    const endDateStr = formatDate(this.endDate);

    // Only pass filter values if they're not empty strings
    const clientFilter = this.selectedClient ? this.selectedClient : undefined;
    const projectFilter = this.selectedProject
      ? this.selectedProject
      : undefined;
    const phaseFilter = this.selectedPhase ? this.selectedPhase : undefined;
    const taskIdFilter = this.selectedTaskId ? this.selectedTaskId : undefined;

    this.taskService
      .getAllTasks(
        this.currentPage,
        this.pageSize,
        clientFilter,
        projectFilter,
        phaseFilter,
        taskIdFilter,
        startDateStr,
        endDateStr,
      )
      .subscribe({
        next: (response) => {
          // Handle ApiResponse wrapper - data is in response.data
          this.tasks = response.data || [];
          this.filteredTasks = this.tasks;

          // Extract pagination metadata
          this.totalElements = response.totalElements || 0;
          this.totalPages = response.totalPages || 0;
          this.currentPage = response.currentPage || 0;

          // Extract unique values for filters from current page
          const currentClients = [...new Set(this.tasks.map((t) => t.client))];
          const currentProjects = [
            ...new Set(this.tasks.map((t) => t.project)),
          ];
          const currentPhases = [...new Set(this.tasks.map((t) => t.phase))];

          // Merge with existing unique values
          this.uniqueClients = [
            ...new Set([...this.uniqueClients, ...currentClients]),
          ].sort((a, b) => a.localeCompare(b));
          this.uniqueProjects = [
            ...new Set([...this.uniqueProjects, ...currentProjects]),
          ].sort((a, b) => a.localeCompare(b));
          this.uniquePhases = [
            ...new Set([...this.uniquePhases, ...currentPhases]),
          ].sort((a, b) => a.localeCompare(b));

          this.loading = false;
          console.log('Loaded tasks:', this.tasks);
        },
        error: (err) => {
          console.error('Error loading tasks:', err);
          // Try to extract error message from backend
          if (err.error?.message) {
            this.error = err.error.message;
          } else if (err.message) {
            this.error = err.message;
          } else {
            this.error =
              'Failed to load tasks. Make sure the Spring Boot backend is running.';
          }
          this.loading = false;
        },
      });
  }

  // Pagination methods
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadTasks();
    }
  }

  applyFilters(): void {
    // Reset to first page when filters change and reload from server
    this.currentPage = 0;
    this.loadTasks();
  }

  clearFilters(): void {
    this.selectedClient = '';
    this.selectedProject = '';
    this.selectedPhase = '';
    this.selectedTaskId = '';
    this.startDate = null;
    this.endDate = null;
    this.applyFilters();
  }

  addTask(): void {
    // Create an empty task with today's date
    const today = new Date().toISOString().split('T')[0];
    const emptyTask: TaskActivity = {
      taskDate: today,
      client: '',
      project: '',
      phase: '',
      hours: 0,
      details: '',
      username: this.authService.getCurrentUsername() || '',
    };

    const dialogRef = this.dialog.open(TaskEditDialogComponent, {
      width: '600px',
      maxHeight: '90vh',
      data: { task: emptyTask, isAddMode: true },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        console.log('Submitting new task:', result);
        // Remove the id field if present since this is a new task
        const { id, ...taskData } = result;
        this.taskService.createTask(taskData).subscribe({
          next: (response) => {
            console.log('Task created successfully:', response);
            this.loadTasks(); // Reload the list
          },
          error: (err) => {
            console.error('Error creating task:', err);
            console.error('Error status:', err.status);
            console.error('Error message:', err.error);

            let errorMessage = 'Failed to create task. ';
            if (err.status === 403) {
              errorMessage = 'You do not have permission to create tasks.';
            } else if (err.status === 409 && err.error?.message) {
              // 409 Conflict - likely a duplicate task
              errorMessage = err.error.message;
            } else if (err.error?.message) {
              errorMessage = err.error.message;
            } else {
              errorMessage = 'Failed to create task. Please try again.';
            }

            this.snackBar.open(errorMessage, 'Close', {
              duration: 5000,
              horizontalPosition: 'center',
              verticalPosition: 'top',
              panelClass: ['error-snackbar'],
            });
          },
        });
      }
    });
  }

  cloneTask(task: TaskActivity): void {
    // Create a copy of the task with today's date and no ID
    const today = new Date().toISOString().split('T')[0];
    const clonedTask: TaskActivity = {
      taskDate: today,
      client: task.client,
      project: task.project,
      phase: task.phase,
      hours: task.hours,
      details: task.details,
      username: this.authService.getCurrentUsername() || '',
    };

    const dialogRef = this.dialog.open(TaskEditDialogComponent, {
      width: '600px',
      maxHeight: '90vh',
      data: { task: clonedTask, isAddMode: true },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        console.log('Submitting cloned task:', result);
        // Remove the id field if present since this is a new task
        const { id, ...taskData } = result;
        this.taskService.createTask(taskData).subscribe({
          next: (response) => {
            console.log('Task cloned successfully:', response);
            this.loadTasks(); // Reload the list
          },
          error: (err) => {
            console.error('Error cloning task:', err);
            console.error('Error status:', err.status);
            console.error('Error message:', err.error);

            let errorMessage = 'Failed to clone task. ';
            if (err.status === 403) {
              errorMessage = 'You do not have permission to create tasks.';
            } else if (err.status === 409 && err.error?.message) {
              // 409 Conflict - likely a duplicate task
              errorMessage = err.error.message;
            } else if (err.error?.message) {
              errorMessage = err.error.message;
            } else {
              errorMessage = 'Failed to clone task. Please try again.';
            }

            this.snackBar.open(errorMessage, 'Close', {
              duration: 5000,
              horizontalPosition: 'center',
              verticalPosition: 'top',
              panelClass: ['error-snackbar'],
            });
          },
        });
      }
    });
  }

  editTask(task: TaskActivity): void {
    const dialogRef = this.dialog.open(TaskEditDialogComponent, {
      width: '600px',
      maxHeight: '90vh',
      data: { task: { ...task }, isAddMode: false }, // Pass a copy
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        console.log('Submitting task update:', result);
        this.taskService.updateTask(result.id, result).subscribe({
          next: (response) => {
            console.log('Task updated successfully:', response);
            this.loadTasks(); // Reload the list
          },
          error: (err) => {
            console.error('Error updating task:', err);
            console.error('Error status:', err.status);
            console.error('Error message:', err.error);

            let errorMessage = 'Failed to update task. ';
            if (err.status === 403) {
              errorMessage = 'You do not have permission to update this task.';
            } else if (err.status === 404) {
              errorMessage = 'The task no longer exists.';
            } else if (err.status === 409 && err.error?.message) {
              // 409 Conflict - likely a duplicate task
              errorMessage = err.error.message;
            } else if (err.error?.message) {
              errorMessage = err.error.message;
            } else {
              errorMessage = 'Failed to update task. Please try again.';
            }

            this.snackBar.open(errorMessage, 'Close', {
              duration: 5000,
              horizontalPosition: 'center',
              verticalPosition: 'top',
              panelClass: ['error-snackbar'],
            });
          },
        });
      }
    });
  }

  deleteTask(task: TaskActivity): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Delete',
        message: `Are you sure you want to delete this task from ${task.taskDate}?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.taskService.deleteTask(task.id!).subscribe({
          next: () => {
            console.log('Task deleted successfully');
            this.loadTasks(); // Reload the list
          },
          error: (err) => {
            console.error('Error deleting task:', err);
            let errorMessage: string;
            if (err.status === 403) {
              errorMessage = 'You do not have permission to delete this task.';
            } else if (err.error?.message) {
              errorMessage = err.error.message;
            } else {
              errorMessage = 'Failed to delete task. Please try again.';
            }
            this.snackBar.open(errorMessage, 'Close', {
              duration: 5000,
              horizontalPosition: 'center',
              verticalPosition: 'top',
              panelClass: ['error-snackbar'],
            });
          },
        });
      }
    });
  }

  // Modified by: Dean Ammons - February 2026
  // Change: Align task-level billability check with ReportsService signature to remove extra parameters
  // Reason: Angular build failed (TS2554) after ReportsService exposed a method that accepts the TaskActivity model
  isTaskBillable(task: TaskActivity): boolean {
    return this.reportsService.isTaskBillable(task);
  }
}
