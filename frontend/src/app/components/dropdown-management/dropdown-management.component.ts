import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { DropdownService } from '../../services/dropdown.service';
import { AuthService } from '../../services/auth.service';
import { DropdownValue } from '../../models/task-activity.model';
import { DropdownEditDialogComponent } from '../dropdown-edit-dialog/dropdown-edit-dialog.component';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-dropdown-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSelectModule,
    MatFormFieldModule,
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

          <div *ngIf="currentRole !== 'USER'">
            <div *ngIf="currentRole === 'GUEST'" class="read-only-notice">
              <mat-icon>info</mat-icon>
              <span
                >Read-only mode - You do not have permission to modify dropdown
                values</span
              >
            </div>

            <!-- Category Filter -->
            <div class="filter-section">
              <mat-form-field appearance="outline">
                <mat-label>Filter by Category</mat-label>
                <mat-select
                  [(ngModel)]="selectedCategory"
                  (selectionChange)="onCategoryChange()"
                >
                  <mat-option value="">All Categories</mat-option>
                  <mat-option *ngFor="let cat of categories" [value]="cat">
                    {{ cat }}
                  </mat-option>
                </mat-select>
              </mat-form-field>

              <button
                mat-raised-button
                color="primary"
                (click)="loadDropdownValues()"
                class="refresh-btn"
              >
                <mat-icon>refresh</mat-icon> Refresh
              </button>
            </div>

            <div *ngIf="loading" class="loading-spinner">
              <mat-spinner></mat-spinner>
            </div>

            <table
              mat-table
              [dataSource]="dropdownValues"
              *ngIf="!loading"
              class="dropdown-table"
            >
              <ng-container matColumnDef="category">
                <th mat-header-cell *matHeaderCellDef>Category</th>
                <td mat-cell *matCellDef="let item">{{ item.category }}</td>
              </ng-container>

              <ng-container matColumnDef="itemValue">
                <th mat-header-cell *matHeaderCellDef>Value</th>
                <td mat-cell *matCellDef="let item">{{ item.itemValue }}</td>
              </ng-container>

              <ng-container matColumnDef="displayOrder">
                <th mat-header-cell *matHeaderCellDef>Order</th>
                <td mat-cell *matCellDef="let item">{{ item.displayOrder }}</td>
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
                    (click)="editDropdown(item)"
                    [disabled]="currentRole === 'GUEST'"
                    [title]="
                      currentRole === 'GUEST' ? 'Read-only mode' : 'Edit'
                    "
                  >
                    <mat-icon>edit</mat-icon>
                  </button>
                  <button
                    mat-icon-button
                    color="warn"
                    (click)="deleteDropdown(item)"
                    [disabled]="currentRole === 'GUEST'"
                    [title]="
                      currentRole === 'GUEST' ? 'Read-only mode' : 'Delete'
                    "
                  >
                    <mat-icon>delete</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr
                mat-header-row
                *matHeaderRowDef="displayedColumnsFiltered"
              ></tr>
              <tr
                mat-row
                *matRowDef="let row; columns: displayedColumnsFiltered"
              ></tr>
            </table>

            <div
              *ngIf="!loading && dropdownValues.length === 0"
              class="no-data"
            >
              <p>
                {{
                  selectedCategory
                    ? 'No ' + selectedCategory.toLowerCase() + ' values found.'
                    : 'No dropdown values found.'
                }}
              </p>
            </div>
          </div>
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

      .filter-section {
        display: flex;
        gap: 16px;
        align-items: center;
        margin-bottom: 24px;
      }

      .filter-section mat-form-field {
        min-width: 250px;
      }

      .refresh-btn {
        margin-top: 8px;
      }

      .dropdown-table {
        width: 100%;
        margin-top: 20px;
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

      .no-data {
        text-align: center;
        padding: 40px;
        color: #999;
        font-style: italic;
      }

      .access-denied,
      .read-only-notice {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 16px;
        background-color: #fff3cd;
        border-radius: 4px;
        margin-bottom: 16px;
      }
    `,
  ],
})
export class DropdownManagementComponent implements OnInit {
  categories: string[] = [];
  dropdownValues: DropdownValue[] = [];
  selectedCategory = '';
  loading = false;
  currentRole = '';

  displayedColumnsFiltered: string[] = [
    'category',
    'itemValue',
    'displayOrder',
    'isActive',
    'actions',
  ];

  constructor(
    private readonly dropdownService: DropdownService,
    private readonly authService: AuthService,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.currentRole = this.authService.getCurrentRole();
    this.loadCategories();
    this.loadDropdownValues();
  }

  loadCategories(): void {
    this.dropdownService.getAllCategories().subscribe({
      next: (data) => {
        this.categories = data;
      },
      error: (err) => {
        console.error('Error loading categories:', err);
      },
    });
  }

  loadDropdownValues(): void {
    this.loading = true;

    const request = this.selectedCategory
      ? this.dropdownService.getValuesByCategory(this.selectedCategory)
      : this.dropdownService.getAllDropdownValues();

    request.subscribe({
      next: (data) => {
        this.dropdownValues = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading dropdown values:', err);
        this.loading = false;
      },
    });
  }

  onCategoryChange(): void {
    this.loadDropdownValues();
  }

  editDropdown(item: DropdownValue): void {
    const dialogRef = this.dialog.open(DropdownEditDialogComponent, {
      width: '550px',
      data: { item: { ...item }, category: item.category },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.dropdownService.updateDropdownValue(result.id, result).subscribe({
          next: () => {
            console.log('Dropdown updated successfully');
            this.loadDropdownValues();
          },
          error: (err) => {
            console.error('Error updating dropdown:', err);
            alert(
              'Failed to update dropdown value. You may not have admin permission.'
            );
          },
        });
      }
    });
  }

  deleteDropdown(item: DropdownValue): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Delete',
        message: `Are you sure you want to delete "${item.itemValue}" from ${item.category}?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.dropdownService.deleteDropdownValue(item.id!).subscribe({
          next: () => {
            console.log('Dropdown deleted successfully');
            this.loadDropdownValues();
          },
          error: (err) => {
            console.error('Error deleting dropdown:', err);
            alert('Failed to delete. You may not have admin permission.');
          },
        });
      }
    });
  }
}
