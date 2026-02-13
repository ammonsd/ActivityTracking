# Angular User Dashboard

This is an Angular 19 user dashboard that integrates with the Spring Boot Task Activity backend. Administrative functions (user management, dropdown management) are available in the **React Admin Dashboard** (`/dashboard`).

## 🎯 Purpose

This Angular frontend demonstrates:

- **Angular 19** with standalone components
- **Angular Material** UI components
- **RESTful API** integration with Spring Boot
- **TypeScript** type safety and modern JavaScript features
- **Responsive design** and professional UI/UX
- **Service-based architecture** for API calls
- **User-focused features** (tasks, expenses, profile, reports)

**Note:** Administrative features (user management, dropdown values management) are provided by the React Admin Dashboard to provide a modern, optimized experience for administrative tasks.

## 📋 Prerequisites

- Node.js (v18 or higher)
- npm (v9 or higher)
- Spring Boot backend running on `http://localhost:8080`

## 🚀 Quick Start

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Start Development Server

```bash
npm start
```

The application will be available at `http://localhost:4200`

### 3. Build for Production

```bash
npm run build
```

Production files will be in the `dist/` directory.

## 📁 Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── components/          # UI Components
│   │   │   ├── dashboard/       # Main user dashboard
│   │   │   ├── task-list/       # Task activities view
│   │   │   ├── task-edit-dialog/    # Task editor dialog
│   │   │   ├── expense-list/    # Expense tracking view
│   │   │   ├── expense-edit-dialog/ # Expense editor dialog
│   │   │   ├── receipt-upload-dialog/ # Receipt upload
│   │   │   ├── profile/         # User profile management
│   │   │   ├── reports/         # Analytics & Reports Dashboard
│   │   │   └── confirm-dialog/  # Confirmation dialogs
│   │   ├── services/            # API Services
│   │   │   ├── task-activity.service.ts
│   │   │   ├── expense.service.ts
│   │   │   ├── user.service.ts  # Used by profile component
│   │   │   ├── dropdown.service.ts  # Used by task/expense dialogs
│   │   │   ├── reports.service.ts
│   │   │   ├── chart-config.service.ts
│   │   │   └── auth.service.ts
│   │   ├── models/              # TypeScript interfaces
│   │   │   ├── task-activity.model.ts
│   │   │   ├── expense.model.ts
│   │   │   └── user.model.ts
│   │   ├── guards/              # Route guards
│   │   │   └── auth.guard.ts
│   │   ├── interceptors/        # HTTP interceptors
│   │   │   └── auth.interceptor.ts
│   │   ├── app.component.ts     # Root component
│   │   ├── app.routes.ts        # Route configuration
│   │   └── app.config.ts        # App configuration
│   ├── environments/            # Environment configs
│   │   ├── environment.ts       # Development
│   │   └── environment.prod.ts  # Production
│   └── styles.scss              # Global styles
├── angular.json                 # Angular CLI config
├── package.json                 # Dependencies
└── tsconfig.json               # TypeScript config
```

## 🎨 Features

### Dashboard

- Overview cards for navigation
- Quick access to all modules
- Modern Material Design interface
- Link to React Admin Dashboard for ADMIN users

### Task Activities

- View all task activities
- Filter by client, project, phase, and date range
- Paginated data table with sorting
- Real-time data from API
- Add, edit, clone, and delete actions
- Weekly timesheet view (Monday-Sunday)
- CSV export capabilities

### Expense Management

- View and manage expenses
- Receipt upload/download in dialogs
- Filter by client, project, type, status, and date range
- Multi-stage approval workflow visibility
- Add, edit, clone, and delete actions (role-based)
- Expense totals and status tracking
- Email notifications for status changes

### User Profile

- Self-service profile management
- Update first name, last name, company, email
- Integrated password change dialog
- Real-time validation with specific error messages
- Password requirements enforced (10+ characters, uppercase, digit, special char)

### Analytics & Reports

- Interactive charts and visualizations (Chart.js)
- Flexible date range filtering with presets
- Time distribution by client and project
- Daily/weekly/monthly time tracking visualizations
- Phase distribution analysis
- ADMIN-only user performance analytics

## 🔌 API Integration

The Angular app communicates with Spring Boot REST APIs:

| Service              | Endpoint               | Purpose                          |
| -------------------- | ---------------------- | -------------------------------- |
| TaskActivityService  | `/api/task-activities` | CRUD for tasks                   |
| ExpenseService       | `/api/expenses`        | CRUD for expenses                |
| UserService          | `/api/users`           | User profile management          |
| DropdownService      | `/api/dropdowns`       | Dropdown values for forms        |
| ReportsService       | `/api/reports`         | Analytics data                   |
| ChartConfigService   | N/A                    | Chart.js configuration           |
| AuthService          | `/api/auth`            | Authentication state management  |

### Environment Configuration

**Development** (`src/environments/environment.ts`):

```typescript
export const environment = {
  production: false,
  apiUrl: "http://localhost:8080/api",
};
```

**Production** (`src/environments/environment.prod.ts`):

```typescript
export const environment = {
  production: true,
  apiUrl: "/api",
};
```

## 🔐 Authentication

The Angular app integrates with Spring Security:

- **Login**: Users authenticate via Spring Boot's `/login` page (Thymeleaf)
- **Session Management**: Spring Security manages the session
- **API Authentication**: HTTP Basic Auth credentials stored in session storage
- **Auth Guards**: Protect routes requiring authentication
- **Auth Interceptor**: Automatically adds Basic Auth headers to API requests
- **Logout**: Redirects to Spring Security's `/logout` endpoint

**No separate Angular login** - Single authentication point through Spring Security.

## 🛠️ Development

### Running Both Applications

**Terminal 1 - Spring Boot Backend:**

```bash
# From project root
mvnw.cmd spring-boot:run
```

Backend runs on: `http://localhost:8080`

**Terminal 2 - Angular Frontend:**

```bash
# From frontend directory
cd frontend
npm start
```

Frontend runs on: `http://localhost:4200`

### Hot Reload

Angular CLI provides hot reload - changes to TypeScript/HTML/CSS files will automatically reload the browser.

### Angular CLI Commands

```bash
# Install new packages
npm install package-name

# Generate new component
ng generate component components/my-component

# Generate new service
ng generate service services/my-service

# Build for production
npm run build

# Run tests
npm test
```

## 📚 Key Angular Concepts Demonstrated

### 1. Standalone Components

All components use the new standalone API (no NgModules):

```typescript
@Component({
  standalone: true,
  imports: [CommonModule, MatTableModule, ...],
})
```

### 2. Dependency Injection

Services are injected via constructor:

```typescript
constructor(private readonly taskService: TaskActivityService) {}
```

### 3. RxJS Observables

Async data handling with observables:

```typescript
this.taskService.getAllTasks().subscribe({
  next: (data) => (this.tasks = data),
  error: (err) => console.error(err),
});
```

### 4. Routing

Declarative routing with Angular Router:

```typescript
export const routes: Routes = [
  { path: "dashboard", component: DashboardComponent, canActivate: [authGuard] },
  { path: "tasks", component: TaskListComponent, canActivate: [authGuard] },
  { path: "expenses", component: ExpenseListComponent, canActivate: [authGuard] },
  { path: "reports", component: ReportsComponent, canActivate: [authGuard] },
  { path: "profile", component: ProfileComponent, canActivate: [authGuard] },
];
```

### 5. Angular Material

Professional UI components:

- `MatTableModule` - Data tables with sorting and pagination
- `MatButtonModule` - Buttons
- `MatCardModule` - Cards
- `MatToolbarModule` - Navigation toolbar
- `MatSidenavModule` - Side navigation drawer
- `MatTabsModule` - Tabbed interfaces
- `MatIconModule` - Material icons
- `MatDialogModule` - Modal dialogs
- `MatFormFieldModule` - Form fields
- `MatSelectModule` - Dropdowns
- `MatDatepickerModule` - Date pickers
- `MatCheckboxModule` - Checkboxes

### 6. TypeScript Interfaces

Type-safe models:

```typescript
export interface TaskActivity {
  id?: number;
  taskDate: string;
  client: string;
  project: string;
  phase: string;
  hours: number;
  details: string;
  username: string;
}
```

### 7. HTTP Interceptors

Automatic request modification:

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Add auth headers automatically
  const credentials = sessionStorage.getItem('auth');
  if (credentials) {
    req = req.clone({
      setHeaders: { Authorization: `Basic ${credentials}` }
    });
  }
  return next(req);
};
```

### 8. Route Guards

Protect routes with guards:

```typescript
export const authGuard = () => {
  const authService = inject(AuthService);
  if (authService.isAuthenticated()) {
    return true;
  }
  globalThis.location.href = '/login';
  return false;
};
```

## 🎓 Interview Preparation Topics

This project demonstrates knowledge of:

1. **Angular Framework**
   - Components, Services, Routing
   - Dependency Injection
   - Lifecycle hooks (ngOnInit)
   - Standalone components (Angular 19+)
   - Material Design integration
   - Reactive programming with RxJS

2. **TypeScript**
   - Interfaces and types
   - Access modifiers (readonly, private)
   - Modern ES6+ features
   - Strict type checking

3. **HTTP Communication**
   - HttpClient for API calls
   - Observable pattern
   - Error handling
   - Interceptors for auth

4. **UI/UX**
   - Angular Material components
   - Responsive design
   - Navigation patterns
   - Role-based UI visibility
   - Chart.js integration
   - Form validation and feedback

5. **Architecture**
   - Service-based architecture
   - Separation of concerns
   - Environment configuration
   - Guard-based route protection
   - Component communication
   - State management patterns

6. **Modern Development**
   - npm and package management
   - Angular CLI
   - Development vs Production builds
   - Hot module replacement
   - Code scaffolding

## 🔧 Troubleshooting

### Backend Connection Issues

If you see errors about failed API calls:

1. **Verify Spring Boot is running:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Check CORS configuration** in `SecurityConfig.java`

3. **Verify API endpoints** match service URLs in `environment.ts`

### Port Already in Use

If port 4200 is in use:

```bash
ng serve --port 4201
```

Then update the URL in your browser to `http://localhost:4201`

### Authentication Issues

If you can't access the Angular app:

1. **Login via Spring Boot first**: Navigate to `http://localhost:8080/login`
2. **Then access Angular**: Navigate to `http://localhost:4200`
3. **Check browser console** for authentication errors

## 📖 Additional Resources

- [Angular Documentation](https://angular.dev)
- [Angular Material](https://material.angular.io)
- [RxJS Documentation](https://rxjs.dev)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Angular CLI Reference](https://angular.dev/tools/cli)
- [Chart.js Documentation](https://www.chartjs.org/)

## 🎯 Current Features

This application already includes:

✅ **Form validation** - Reactive forms with comprehensive validators
✅ **Edit dialogs** - Material dialogs for inline editing (tasks, expenses, profile)
✅ **Charts & analytics** - Chart.js integration with interactive reports
✅ **Multiple views** - Dashboard, lists, reports, profile
✅ **Receipt management** - Upload/download receipts directly in dialogs
✅ **Role-based access** - Different features for USER, ADMIN, GUEST roles
✅ **Weekly timesheet view** - Monday-Sunday format with totals
✅ **CSV export** - Export filtered data
✅ **Password management** - Self-service password change with validation

## 🔗 Related Dashboards

For administrative functions, see:
- **React Admin Dashboard** (`/dashboard`) - User management, dropdown management, system configuration (modern React + Material-UI)
- **Spring Boot UI** (`/task-activity/list`, `/expenses/list`) - Classic Thymeleaf-based interfaces

---

**This Angular application demonstrates modern user-focused development with Spring Boot backend integration!** 🚀
