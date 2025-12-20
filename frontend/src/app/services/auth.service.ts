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

  private readonly userFirstnameSubject = new BehaviorSubject<string>('');
  public readonly userFirstname$ = this.userFirstnameSubject.asObservable();

  private readonly userLastnameSubject = new BehaviorSubject<string>('');
  public readonly userLastname$ = this.userLastnameSubject.asObservable();

  private readonly userRoleSubject = new BehaviorSubject<string>('');
  public readonly userRole$ = this.userRoleSubject.asObservable();

  private readonly passwordExpiringWarningSubject = new BehaviorSubject<string>(
    ''
  );
  public readonly passwordExpiringWarning$ =
    this.passwordExpiringWarningSubject.asObservable();

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

      // Try to restore from sessionStorage first (for page refreshes)
      const storedUsername = sessionStorage.getItem('username');
      const storedRole = sessionStorage.getItem('userRole');

      if (storedUsername && storedRole) {
        this.username = storedUsername;
        this.userRole = storedRole;
        this.currentUserSubject.next(storedUsername);
        this.userRoleSubject.next(storedRole);
        console.log(
          'AuthService - Restored from session:',
          storedUsername,
          storedRole
        );
      } else {
        console.log('AuthService - No cached data, will fetch from API');
      }

      // Try to get username and role from API (always call to ensure fresh data)
      this.http.get<any>(`${environment.apiUrl}/users/me`).subscribe({
        next: (response) => {
          // ApiResponse wrapper - username is in response.data.username
          if (response.data?.username) {
            this.username = response.data.username;
            this.userRole = response.data.role;
            sessionStorage.setItem('username', response.data.username);
            sessionStorage.setItem('userRole', response.data.role);

            console.log(
              'AuthService - User loaded from API:',
              response.data.username
            );
            console.log(
              'AuthService - Role loaded from API:',
              response.data.role
            );
            console.log('AuthService - Role type:', typeof response.data.role);
            console.log(
              'AuthService - Role stringified:',
              JSON.stringify(response.data.role)
            );

            // Always emit to observables (even if value unchanged, to ensure UI updates)
            this.currentUserSubject.next(response.data.username);
            this.userRoleSubject.next(response.data.role);
            this.userFirstnameSubject.next(response.data.firstname || '');
            this.userLastnameSubject.next(response.data.lastname || '');

            // Handle password expiring warning
            if (response.data.passwordExpiringWarning) {
              // Don't show warning for GUEST users (they can't change passwords)
              if (response.data.role !== 'GUEST') {
                this.passwordExpiringWarningSubject.next(
                  response.data.passwordExpiringWarning
                );
              }
            }
          }
        },
        error: (err) => {
          // API call failed but we trust Spring Security session
          console.warn(
            'Failed to fetch user details from API, but Spring Security session is valid',
            err
          );
          // If we don't have cached data, set a default to prevent blank screen
          if (!this.username) {
            console.warn(
              'No cached user data - app may have limited functionality'
            );
            // Still authenticated via Spring Security session, just missing user details
            // The app will continue to work, subsequent API calls will succeed
          }
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
                this.userRoleSubject.next(response.data.role);
                this.currentUserSubject.next(response.data.username);
                this.userFirstnameSubject.next(response.data.firstname || '');
                this.userLastnameSubject.next(response.data.lastname || '');

                // Handle password expiring warning
                if (response.data.passwordExpiringWarning) {
                  // Don't show warning for GUEST users (they can't change passwords)
                  if (response.data.role !== 'GUEST') {
                    this.passwordExpiringWarningSubject.next(
                      response.data.passwordExpiringWarning
                    );
                  }
                }
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
    this.currentUserSubject.next('');
    this.userFirstnameSubject.next('');
    this.userLastnameSubject.next('');
    this.userRoleSubject.next('');
    this.passwordExpiringWarningSubject.next('');
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
