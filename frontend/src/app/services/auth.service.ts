import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public readonly isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private readonly currentUserSubject = new BehaviorSubject<string>('');
  public readonly currentUser$ = this.currentUserSubject.asObservable();

  private readonly userRoleSubject = new BehaviorSubject<string>('');
  public readonly userRole$ = this.userRoleSubject.asObservable();

  // Store credentials (in production, use JWT tokens instead)
  private credentials: string | null = null;
  private username: string | null = null;
  private userRole: string | null = null;

  constructor(private readonly http: HttpClient) {
    // Check if user is accessing /app routes - if so, they passed Spring Security
    // Spring Boot would redirect unauthenticated users to /login
    const currentPath = globalThis.location.pathname;
    if (currentPath.startsWith('/app')) {
      // User came through Spring Security - trust the session
      this.isAuthenticatedSubject.next(true);

      // Try to get username and role from API
      this.http.get<any>(`${environment.apiUrl}/users/me`).subscribe({
        next: (response) => {
          // ApiResponse wrapper - username is in response.data.username
          if (response.data?.username) {
            this.username = response.data.username;
            this.userRole = response.data.role;
            sessionStorage.setItem('username', response.data.username);
            sessionStorage.setItem('userRole', response.data.role);

            console.log('AuthService - User loaded:', response.data.username);
            console.log('AuthService - Role loaded:', response.data.role);

            // Emit to observables
            this.currentUserSubject.next(response.data.username);
            this.userRoleSubject.next(response.data.role);
          }
        },
        error: () => {
          // API call failed but we trust Spring Security session
          console.warn(
            'Failed to fetch user details, but Spring Security session is valid'
          );
        },
      });
    }
  }

  login(username: string, password: string): Observable<any> {
    // Create Basic Auth header
    const credentials = btoa(`${username}:${password}`);
    const headers = new HttpHeaders({
      Authorization: `Basic ${credentials}`,
    });

    // Test authentication by calling a protected endpoint that requires auth
    return this.http
      .get(`${environment.apiUrl}/task-activities?size=1`, { headers })
      .pipe(
        tap(() => {
          // If successful, save credentials
          this.credentials = credentials;
          this.username = username;
          sessionStorage.setItem('auth', credentials);
          sessionStorage.setItem('username', username);
          this.isAuthenticatedSubject.next(true);

          // Fetch user role
          this.http.get<any>(`${environment.apiUrl}/users/me`).subscribe({
            next: (response) => {
              if (response.data?.role) {
                this.userRole = response.data.role;
                sessionStorage.setItem('userRole', response.data.role);
              }
            },
          });
        })
      );
  }

  logout(): void {
    this.credentials = null;
    this.username = null;
    this.userRole = null;
    sessionStorage.removeItem('auth');
    sessionStorage.removeItem('username');
    sessionStorage.removeItem('userRole');
    this.isAuthenticatedSubject.next(false);
  }

  getAuthHeader(): HttpHeaders {
    if (this.credentials) {
      return new HttpHeaders({
        Authorization: `Basic ${this.credentials}`,
      });
    }
    return new HttpHeaders();
  }

  getCurrentUsername(): string {
    return this.username || sessionStorage.getItem('username') || '';
  }

  getCurrentRole(): string {
    return this.userRole || sessionStorage.getItem('userRole') || '';
  }

  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }
}
