import { Component, ViewChild } from '@angular/core';
import {
  RouterOutlet,
  RouterLink,
  RouterLinkActive,
  Router,
} from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule, MatDrawer } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';
import { ExpenseService } from './services/expense.service';

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  @ViewChild('drawer') drawer?: MatDrawer;

  title = 'Admin Dashboard';
  isAuthenticated$;
  currentUser = '';
  currentRole = '';
  currentDate = new Date();
  passwordExpiringWarning = '';
  canAccessExpenses = false;
  userDisplayName = '';

  constructor(
    public readonly authService: AuthService,
    private readonly router: Router,
    private readonly expenseService: ExpenseService
  ) {
    this.isAuthenticated$ = this.authService.isAuthenticated$;
    this.currentUser = this.authService.getCurrentUsername();
    this.currentRole = this.authService.getCurrentRole();

    console.log('AppComponent - Initial username:', this.currentUser);
    console.log('AppComponent - Initial role:', this.currentRole);

    // Check if user can access expenses
    this.checkExpenseAccess();

    // Subscribe to user changes
    this.authService.currentUser$.subscribe({
      next: (username: string) => {
        console.log('AppComponent - Username updated:', username);
        this.currentUser = username;
        this.updateDisplayName();
        // Re-check expense access when user changes
        if (username) {
          this.checkExpenseAccess();
        }
      },
    });

    // Subscribe to firstname changes
    this.authService.userFirstname$.subscribe({
      next: () => {
        this.updateDisplayName();
      },
    });

    // Subscribe to lastname changes
    this.authService.userLastname$.subscribe({
      next: () => {
        this.updateDisplayName();
      },
    });

    // Subscribe to role changes
    this.authService.userRole$.subscribe({
      next: (role: string) => {
        console.log('AppComponent - Role updated:', role);
        this.currentRole = role;
      },
    });

    // Subscribe to password expiring warning
    this.authService.passwordExpiringWarning$.subscribe({
      next: (warning: string) => {
        console.log('AppComponent - Password warning:', warning);
        this.passwordExpiringWarning = warning;
      },
    });
  }

  updateDisplayName(): void {
    let firstname = '';
    let lastname = '';

    this.authService.userFirstname$
      .subscribe((fn) => (firstname = fn))
      .unsubscribe();
    this.authService.userLastname$
      .subscribe((ln) => (lastname = ln))
      .unsubscribe();

    if (firstname || lastname) {
      const fullName = `${firstname} ${lastname}`.trim();
      this.userDisplayName = `${fullName} (${this.currentUser})`;
    } else {
      this.userDisplayName = this.currentUser;
    }
  }

  checkExpenseAccess(): void {
    this.expenseService.canAccessExpenses().subscribe({
      next: (response) => {
        this.canAccessExpenses = response.data || false;
        console.log(
          'AppComponent - Can access expenses:',
          this.canAccessExpenses
        );
      },
      error: (err) => {
        console.error('AppComponent - Error checking expense access:', err);
        this.canAccessExpenses = false;
      },
    });
  }

  logout(): void {
    // Clear Angular auth state first
    this.authService.logout();

    // Navigate to logout endpoint (now accepts GET)
    globalThis.location.href = '/logout';
  }

  navigateExternal(
    event: Event,
    url: string,
    preventDefault: boolean = false
  ): void {
    event.preventDefault();
    if (!preventDefault) {
      globalThis.location.href = url;
    }
  }
}
