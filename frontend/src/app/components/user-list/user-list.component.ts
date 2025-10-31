import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/task-activity.model';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="user-list-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>User Management</mat-card-title>
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
                >Read-only mode - You do not have permission to modify
                users</span
              >
            </div>

            <div class="table-actions">
              <button mat-raised-button color="primary" (click)="loadUsers()">
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
              [dataSource]="users"
              *ngIf="!loading && !error"
              class="user-table"
            >
              <ng-container matColumnDef="username">
                <th mat-header-cell *matHeaderCellDef>Username</th>
                <td mat-cell *matCellDef="let user">{{ user.username }}</td>
              </ng-container>

              <ng-container matColumnDef="firstname">
                <th mat-header-cell *matHeaderCellDef>First Name</th>
                <td mat-cell *matCellDef="let user">{{ user.firstname }}</td>
              </ng-container>

              <ng-container matColumnDef="lastname">
                <th mat-header-cell *matHeaderCellDef>Last Name</th>
                <td mat-cell *matCellDef="let user">{{ user.lastname }}</td>
              </ng-container>

              <ng-container matColumnDef="company">
                <th mat-header-cell *matHeaderCellDef>Company</th>
                <td mat-cell *matCellDef="let user">{{ user.company }}</td>
              </ng-container>

              <ng-container matColumnDef="role">
                <th mat-header-cell *matHeaderCellDef>Role</th>
                <td mat-cell *matCellDef="let user">
                  <span
                    class="role-badge"
                    [class.admin]="user.role === 'ADMIN'"
                  >
                    {{ user.role }}
                  </span>
                </td>
              </ng-container>

              <ng-container matColumnDef="enabled">
                <th mat-header-cell *matHeaderCellDef>Status</th>
                <td mat-cell *matCellDef="let user">
                  <span
                    [class.enabled]="user.enabled"
                    [class.disabled]="!user.enabled"
                  >
                    {{ user.enabled ? 'Active' : 'Inactive' }}
                  </span>
                </td>
              </ng-container>

              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let user">
                  <button
                    mat-icon-button
                    color="primary"
                    (click)="editUser(user)"
                    [disabled]="currentRole === 'GUEST'"
                    [title]="
                      currentRole === 'GUEST' ? 'Read-only mode' : 'Edit User'
                    "
                  >
                    <mat-icon>edit</mat-icon>
                  </button>
                  <button
                    mat-icon-button
                    color="warn"
                    (click)="deleteUser(user)"
                    [disabled]="
                      user.username === 'admin' || currentRole === 'GUEST'
                    "
                    [title]="
                      currentRole === 'GUEST'
                        ? 'Read-only mode'
                        : user.username === 'admin'
                        ? 'Cannot delete admin user'
                        : 'Delete User'
                    "
                  >
                    <mat-icon>delete</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
            </table>

            <div
              *ngIf="users.length === 0 && !loading && !error"
              class="no-data"
            >
              <p>No users found or API not accessible.</p>
            </div>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .user-list-container {
        padding: 20px;
        max-width: 1400px;
        margin: 0 auto;
      }

      .table-actions {
        margin-bottom: 20px;
        display: flex;
        gap: 10px;
        justify-content: flex-end;
      }

      .user-table {
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

      .role-badge {
        padding: 4px 12px;
        border-radius: 12px;
        background-color: #e3f2fd;
        color: #1976d2;
        font-size: 12px;
        font-weight: 500;
      }

      .role-badge.admin {
        background-color: #fce4ec;
        color: #c2185b;
      }

      .enabled {
        color: #4caf50;
        font-weight: 500;
      }

      .disabled {
        color: #f44336;
        font-weight: 500;
      }
    `,
  ],
})
export class UserListComponent implements OnInit {
  users: User[] = [];
  displayedColumns: string[] = [
    'username',
    'firstname',
    'lastname',
    'company',
    'role',
    'enabled',
    'actions',
  ];
  loading = false;
  error: string | null = null;
  currentRole = '';

  constructor(
    private readonly userService: UserService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentRole = this.authService.getCurrentRole();
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = null;

    this.userService.getAllUsers().subscribe({
      next: (response) => {
        // Handle ApiResponse wrapper - data is in response.data
        this.users = response.data || [];
        this.loading = false;
        console.log('Loaded users:', this.users);
      },
      error: (err) => {
        console.error('Error loading users:', err);
        this.error =
          'Failed to load users. Make sure the Spring Boot backend is running and you have admin access.';
        this.loading = false;
      },
    });
  }

  editUser(user: User): void {
    console.log('Edit user:', user);
    alert(`Edit functionality not yet implemented for user: ${user.username}`);
    // TODO: Implement edit dialog/form
  }

  deleteUser(user: User): void {
    if (user.username === 'admin') {
      alert('Cannot delete the admin user');
      return;
    }

    if (confirm(`Are you sure you want to delete user: ${user.username}?`)) {
      this.userService.deleteUser(user.id!).subscribe({
        next: () => {
          console.log('User deleted successfully');
          this.loadUsers(); // Reload the list
        },
        error: (err) => {
          console.error('Error deleting user:', err);
          alert('Failed to delete user. You may not have permission.');
        },
      });
    }
  }
}
