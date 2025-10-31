import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { TaskListComponent } from './components/task-list/task-list.component';
import { UserListComponent } from './components/user-list/user-list.component';
import { DropdownManagementComponent } from './components/dropdown-management/dropdown-management.component';
import { ReportsComponent } from './components/reports/reports.component';
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
];
