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

  title = 'Task Activity Admin Dashboard';
  isAuthenticated$;
  currentUser = '';
  currentRole = '';
  currentDate = new Date();

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.isAuthenticated$ = this.authService.isAuthenticated$;
    this.currentUser = this.authService.getCurrentUsername();
    this.currentRole = this.authService.getCurrentRole();

    console.log('AppComponent - Initial username:', this.currentUser);
    console.log('AppComponent - Initial role:', this.currentRole);

    // Subscribe to user changes
    this.authService.currentUser$.subscribe({
      next: (username: string) => {
        console.log('AppComponent - Username updated:', username);
        this.currentUser = username;
      },
    });

    // Subscribe to role changes
    this.authService.userRole$.subscribe({
      next: (role: string) => {
        console.log('AppComponent - Role updated:', role);
        this.currentRole = role;
      },
    });
  }

  logout(): void {
    // Clear Angular auth state first
    this.authService.logout();

    // Navigate to logout endpoint (now accepts GET)
    window.location.href = '/logout';
  }
}
