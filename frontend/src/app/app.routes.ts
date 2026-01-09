/**
 * Description: Application routes - defines the routing configuration for all application pages and guards
 *
 * Author: Dean Ammons
 * Date: December 2025
 */

import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { TaskListComponent } from './components/task-list/task-list.component';
import { ExpenseListComponent } from './components/expense-list/expense-list.component';
import { UserListComponent } from './components/user-list/user-list.component';
import { DropdownManagementComponent } from './components/dropdown-management/dropdown-management.component';
import { ReportsComponent } from './components/reports/reports.component';
import { ProfileComponent } from './components/profile/profile.component';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
  },
  { path: 'tasks', component: TaskListComponent, canActivate: [authGuard] },
  {
    path: 'expenses',
    component: ExpenseListComponent,
    canActivate: [authGuard],
  },
  {
    path: 'users',
    component: UserListComponent,
    canActivate: [authGuard],
  },
  {
    path: 'dropdowns',
    component: DropdownManagementComponent,
    canActivate: [authGuard],
  },
  { path: 'reports', component: ReportsComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
];
