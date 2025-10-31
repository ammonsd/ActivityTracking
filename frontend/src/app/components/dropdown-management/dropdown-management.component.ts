import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DropdownService } from '../../services/dropdown.service';
import { AuthService } from '../../services/auth.service';
import { DropdownValue } from '../../models/task-activity.model';

@Component({
  selector: 'app-dropdown-management',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="dropdown-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Dropdown Management</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div *ngIf="currentRole === 'USER'" class="access-denied">
            <mat-icon>block</mat-icon>
            <p>You do not have permission to access this page.</p>
          </div>

          <mat-tab-group *ngIf="currentRole !== 'USER'">
            <mat-tab label="Clients">
              <div class="tab-content">
                <div *ngIf="currentRole === 'GUEST'" class="read-only-notice">
                  <mat-icon>info</mat-icon>
                  <span
                    >Read-only mode - You do not have permission to modify
                    dropdown values</span
                  >
                </div>

                <button
                  mat-raised-button
                  color="primary"
                  (click)="loadClients()"
                >
                  <mat-icon>refresh</mat-icon> Refresh
                </button>

                <div *ngIf="loadingClients" class="loading-spinner">
                  <mat-spinner></mat-spinner>
                </div>

                <table
                  mat-table
                  [dataSource]="clients"
                  *ngIf="!loadingClients"
                  class="dropdown-table"
                >
                  <ng-container matColumnDef="itemValue">
                    <th mat-header-cell *matHeaderCellDef>Client Name</th>
                    <td mat-cell *matCellDef="let item">
                      {{ item.itemValue }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="displayOrder">
                    <th mat-header-cell *matHeaderCellDef>Order</th>
                    <td mat-cell *matCellDef="let item">
                      {{ item.displayOrder }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="isActive">
                    <th mat-header-cell *matHeaderCellDef>Status</th>
                    <td mat-cell *matCellDef="let item">
                      <span
                        [class.active]="item.isActive"
                        [class.inactive]="!item.isActive"
                      >
                        {{ item.isActive ? 'Active' : 'Inactive' }}
                      </span>
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let item">
                      <button
                        mat-icon-button
                        color="primary"
                        (click)="editDropdown(item, 'CLIENT')"
                        [disabled]="currentRole === 'GUEST'"
                        [title]="
                          currentRole === 'GUEST'
                            ? 'Read-only mode'
                            : 'Edit Client'
                        "
                      >
                        <mat-icon>edit</mat-icon>
                      </button>
                      <button
                        mat-icon-button
                        color="warn"
                        (click)="deleteDropdown(item, 'CLIENT')"
                        [disabled]="currentRole === 'GUEST'"
                        [title]="
                          currentRole === 'GUEST'
                            ? 'Read-only mode'
                            : 'Delete Client'
                        "
                      >
                        <mat-icon>delete</mat-icon>
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr
                    mat-row
                    *matRowDef="let row; columns: displayedColumns"
                  ></tr>
                </table>
              </div>
            </mat-tab>

            <mat-tab label="Projects">
              <div class="tab-content">
                <div *ngIf="currentRole === 'GUEST'" class="read-only-notice">
                  <mat-icon>info</mat-icon>
                  <span
                    >Read-only mode - You do not have permission to modify
                    dropdown values</span
                  >
                </div>

                <button
                  mat-raised-button
                  color="primary"
                  (click)="loadProjects()"
                >
                  <mat-icon>refresh</mat-icon> Refresh
                </button>

                <div *ngIf="loadingProjects" class="loading-spinner">
                  <mat-spinner></mat-spinner>
                </div>

                <table
                  mat-table
                  [dataSource]="projects"
                  *ngIf="!loadingProjects"
                  class="dropdown-table"
                >
                  <ng-container matColumnDef="itemValue">
                    <th mat-header-cell *matHeaderCellDef>Project Name</th>
                    <td mat-cell *matCellDef="let item">
                      {{ item.itemValue }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="displayOrder">
                    <th mat-header-cell *matHeaderCellDef>Order</th>
                    <td mat-cell *matCellDef="let item">
                      {{ item.displayOrder }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="isActive">
                    <th mat-header-cell *matHeaderCellDef>Status</th>
                    <td mat-cell *matCellDef="let item">
                      <span
                        [class.active]="item.isActive"
                        [class.inactive]="!item.isActive"
                      >
                        {{ item.isActive ? 'Active' : 'Inactive' }}
                      </span>
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let item">
                      <button
                        mat-icon-button
                        color="primary"
                        (click)="editDropdown(item, 'PROJECT')"
                        [disabled]="currentRole === 'GUEST'"
                        [title]="
                          currentRole === 'GUEST'
                            ? 'Read-only mode'
                            : 'Edit Project'
                        "
                      >
                        <mat-icon>edit</mat-icon>
                      </button>
                      <button
                        mat-icon-button
                        color="warn"
                        (click)="deleteDropdown(item, 'PROJECT')"
                        [disabled]="currentRole === 'GUEST'"
                        [title]="
                          currentRole === 'GUEST'
                            ? 'Read-only mode'
                            : 'Delete Project'
                        "
                      >
                        <mat-icon>delete</mat-icon>
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr
                    mat-row
                    *matRowDef="let row; columns: displayedColumns"
                  ></tr>
                </table>
              </div>
            </mat-tab>

            <mat-tab label="Phases">
              <div class="tab-content">
                <div *ngIf="currentRole === 'GUEST'" class="read-only-notice">
                  <mat-icon>info</mat-icon>
                  <span
                    >Read-only mode - You do not have permission to modify
                    dropdown values</span
                  >
                </div>

                <button
                  mat-raised-button
                  color="primary"
                  (click)="loadPhases()"
                >
                  <mat-icon>refresh</mat-icon> Refresh
                </button>

                <div *ngIf="loadingPhases" class="loading-spinner">
                  <mat-spinner></mat-spinner>
                </div>

                <table
                  mat-table
                  [dataSource]="phases"
                  *ngIf="!loadingPhases"
                  class="dropdown-table"
                >
                  <ng-container matColumnDef="itemValue">
                    <th mat-header-cell *matHeaderCellDef>Phase Name</th>
                    <td mat-cell *matCellDef="let item">
                      {{ item.itemValue }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="displayOrder">
                    <th mat-header-cell *matHeaderCellDef>Order</th>
                    <td mat-cell *matCellDef="let item">
                      {{ item.displayOrder }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="isActive">
                    <th mat-header-cell *matHeaderCellDef>Status</th>
                    <td mat-cell *matCellDef="let item">
                      <span
                        [class.active]="item.isActive"
                        [class.inactive]="!item.isActive"
                      >
                        {{ item.isActive ? 'Active' : 'Inactive' }}
                      </span>
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let item">
                      <button
                        mat-icon-button
                        color="primary"
                        (click)="editDropdown(item, 'PHASE')"
                        [disabled]="currentRole === 'GUEST'"
                        [title]="
                          currentRole === 'GUEST'
                            ? 'Read-only mode'
                            : 'Edit Phase'
                        "
                      >
                        <mat-icon>edit</mat-icon>
                      </button>
                      <button
                        mat-icon-button
                        color="warn"
                        (click)="deleteDropdown(item, 'PHASE')"
                        [disabled]="currentRole === 'GUEST'"
                        [title]="
                          currentRole === 'GUEST'
                            ? 'Read-only mode'
                            : 'Delete Phase'
                        "
                      >
                        <mat-icon>delete</mat-icon>
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr
                    mat-row
                    *matRowDef="let row; columns: displayedColumns"
                  ></tr>
                </table>
              </div>
            </mat-tab>
          </mat-tab-group>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .dropdown-container {
        padding: 20px;
        max-width: 1200px;
        margin: 0 auto;
      }

      .tab-content {
        padding: 20px 0;
        display: flex;
        flex-direction: column;
        align-items: flex-end;
      }

      .tab-content button {
        margin-bottom: 20px;
      }

      .dropdown-table {
        width: 100%;
        margin-top: 20px;
        align-self: stretch;
      }

      .loading-spinner {
        display: flex;
        justify-content: center;
        padding: 40px;
      }

      th {
        font-weight: bold;
        background-color: #f5f5f5;
      }

      .active {
        color: #4caf50;
        font-weight: 500;
      }

      .inactive {
        color: #9e9e9e;
      }
    `,
  ],
})
export class DropdownManagementComponent implements OnInit {
  clients: DropdownValue[] = [];
  projects: DropdownValue[] = [];
  phases: DropdownValue[] = [];
  displayedColumns: string[] = [
    'itemValue',
    'displayOrder',
    'isActive',
    'actions',
  ];

  loadingClients = false;
  loadingProjects = false;
  loadingPhases = false;
  currentRole = '';

  constructor(
    private readonly dropdownService: DropdownService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentRole = this.authService.getCurrentRole();
    this.loadClients();
    this.loadProjects();
    this.loadPhases();
  }

  loadClients(): void {
    this.loadingClients = true;
    this.dropdownService.getClients().subscribe({
      next: (data) => {
        this.clients = data;
        this.loadingClients = false;
      },
      error: (err) => {
        console.error('Error loading clients:', err);
        this.loadingClients = false;
      },
    });
  }

  loadProjects(): void {
    this.loadingProjects = true;
    this.dropdownService.getProjects().subscribe({
      next: (data) => {
        this.projects = data;
        this.loadingProjects = false;
      },
      error: (err) => {
        console.error('Error loading projects:', err);
        this.loadingProjects = false;
      },
    });
  }

  loadPhases(): void {
    this.loadingPhases = true;
    this.dropdownService.getPhases().subscribe({
      next: (data) => {
        this.phases = data;
        this.loadingPhases = false;
      },
      error: (err) => {
        console.error('Error loading phases:', err);
        this.loadingPhases = false;
      },
    });
  }

  editDropdown(item: DropdownValue, category: string): void {
    console.log('Edit dropdown:', item, category);
    alert(
      `Edit functionality not yet implemented for ${category}: ${item.itemValue}`
    );
    // TODO: Implement edit dialog/form
  }

  deleteDropdown(item: DropdownValue, category: string): void {
    if (
      confirm(`Are you sure you want to delete ${category}: ${item.itemValue}?`)
    ) {
      this.dropdownService.deleteDropdownValue(item.id!).subscribe({
        next: () => {
          console.log('Dropdown deleted successfully');
          // Reload the appropriate category
          if (category === 'CLIENT') {
            this.loadClients();
          } else if (category === 'PROJECT') {
            this.loadProjects();
          } else if (category === 'PHASE') {
            this.loadPhases();
          }
        },
        error: (err) => {
          console.error('Error deleting dropdown:', err);
          alert('Failed to delete. You may not have admin permission.');
        },
      });
    }
  }
}
