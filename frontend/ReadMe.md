# Angular Admin Dashboard# Frontend

This is an Angular 19 admin dashboard that integrates with the Spring Boot Task Activity backend.This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 19.2.19.

## 🎯 Purpose## Development server

This Angular frontend demonstrates:To start a local development server, run:

- **Angular 19** with standalone components

- **Angular Material** UI components```bash

- **RESTful API** integration with Spring Bootng serve

- **TypeScript** type safety and modern JavaScript features```

- **Responsive design** and professional UI/UX

- **Service-based architecture** for API callsOnce the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## 📋 Prerequisites## Code scaffolding

- Node.js (v18 or higher)Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

- npm (v9 or higher)

- Spring Boot backend running on `http://localhost:8080````bash

ng generate component component-name

## 🚀 Quick Start```

### 1. Install DependenciesFor a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

`bash`bash

cd frontendng generate --help

npm install```

````

## Building

### 2. Start Development Server

To build the project run:

```bash

npm start```bash

```ng build

````

The application will be available at `http://localhost:4200`

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

### 3. Build for Production

## Running unit tests

```bash

npm run buildTo execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```

```bash

Production files will be in the `dist/` directory.ng test

```

## 📁 Project Structure

## Running end-to-end tests

````

frontend/For end-to-end (e2e) testing, run:

├── src/

│   ├── app/```bash

│   │   ├── components/          # UI Componentsng e2e

│   │   │   ├── dashboard/       # Main dashboard```

│   │   │   ├── task-list/       # Task activities view

│   │   │   ├── user-list/       # User managementAngular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

│   │   │   └── dropdown-management/  # Dropdown values

│   │   ├── services/            # API Services## Additional Resources

│   │   │   ├── task-activity.service.ts

│   │   │   ├── user.service.tsFor more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

│   │   │   └── dropdown.service.ts
│   │   ├── models/              # TypeScript interfaces
│   │   │   └── task-activity.model.ts
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
````

## 🎨 Features

### Dashboard

- Overview cards for navigation
- Quick access to all modules
- Modern Material Design interface

### Task Activities

- View all task activities
- Paginated data table
- Real-time data from API
- Edit and delete actions

### User Management

- List all users
- View user roles and status
- Add/Edit/Delete users
- Role-based badges

### Dropdown Management

- Tabbed interface for Clients, Projects, Phases
- View and manage dropdown values
- Active/Inactive status indicators

## 🔌 API Integration

The Angular app communicates with Spring Boot REST APIs:

| Service             | Endpoint               | Purpose         |
| ------------------- | ---------------------- | --------------- |
| TaskActivityService | `/api/task-activities` | CRUD for tasks  |
| UserService         | `/api/users`           | User management |
| DropdownService     | `/api/dropdowns`       | Dropdown values |

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

## 🛠️ Development

### Running Both Applications

**Terminal 1 - Spring Boot Backend:**

```bash
# From project root
mvnw.cmd spring-boot:run
```

**Terminal 2 - Angular Frontend:**

```bash
# From frontend directory
cd frontend
npm start
```

### Hot Reload

Angular CLI provides hot reload - changes to TypeScript/HTML/CSS files will automatically reload the browser.

## 📚 Key Angular Concepts Demonstrated

### 1. Standalone Components

All components use the new standalone API (no NgModules):

```typescript
@Component({
  standalone: true,
  imports: [CommonModule, MatTableModule, ...]
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
  { path: "dashboard", component: DashboardComponent },
  { path: "tasks", component: TaskListComponent },
];
```

### 5. Angular Material

Professional UI components:

- `MatTableModule` - Data tables
- `MatButtonModule` - Buttons
- `MatCardModule` - Cards
- `MatToolbarModule` - Navigation
- `MatSidenavModule` - Side navigation

### 6. TypeScript Interfaces

Type-safe models:

```typescript
export interface TaskActivity {
  id?: number;
  taskDate: string;
  clientName: string;
  // ...
}
```

## 🎓 Interview Preparation Topics

This project demonstrates knowledge of:

1. **Angular Framework**

   - Components, Services, Routing
   - Dependency Injection
   - Lifecycle hooks (ngOnInit)

2. **TypeScript**

   - Interfaces and types
   - Access modifiers (readonly, private)
   - Modern ES6+ features

3. **HTTP Communication**

   - HttpClient for API calls
   - Observable pattern
   - Error handling

4. **UI/UX**

   - Angular Material components
   - Responsive design
   - Navigation patterns

5. **Architecture**

   - Service-based architecture
   - Separation of concerns
   - Environment configuration

6. **Modern Development**
   - npm and package management
   - Angular CLI
   - Development vs Production builds

## 🔧 Troubleshooting

### Backend Connection Issues

If you see errors about failed API calls:

1. **Verify Spring Boot is running:**

   ```bash
   curl http://localhost:8080/api/health
   ```

2. **Check CORS configuration** in `SecurityConfig.java`

3. **Verify API endpoints** match service URLs

### Port Already in Use

If port 4200 is in use:

```bash
ng serve --port 4201
```

## 📖 Additional Resources

- [Angular Documentation](https://angular.dev)
- [Angular Material](https://material.angular.io)
- [RxJS Documentation](https://rxjs.dev)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)

## 🎯 Next Steps

To enhance the application:

1. **Add authentication** - Integrate with Spring Security
2. **Form validation** - Add reactive forms with validation
3. **Charts & Analytics** - Add data visualization
4. **Real-time updates** - Implement WebSocket communication
5. **Unit tests** - Add Jasmine/Karma tests
6. **E2E tests** - Add Protractor/Cypress tests

---

**Good luck with your interview! 🚀**
