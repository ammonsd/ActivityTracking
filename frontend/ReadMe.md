# Angular Admin Dashboard# Angular Admin Dashboard# Frontend



This is an Angular 19 admin dashboard that integrates with the Spring Boot Task Activity backend.This is an Angular 19 admin dashboard that integrates with the Spring Boot Task Activity backend.This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 19.2.19.



## 🎯 Purpose## 🎯 Purpose## Development server



This Angular frontend demonstrates:This Angular frontend demonstrates:To start a local development server, run:



- **Angular 19** with standalone components- **Angular 19** with standalone components

- **Angular Material** UI components

- **RESTful API** integration with Spring Boot- **Angular Material** UI components```bash

- **TypeScript** type safety and modern JavaScript features

- **Responsive design** and professional UI/UX- **RESTful API** integration with Spring Bootng serve

- **Service-based architecture** for API calls

- **TypeScript** type safety and modern JavaScript features```

## 📋 Prerequisites

- **Responsive design** and professional UI/UX

- Node.js (v18 or higher)

- npm (v9 or higher)- **Service-based architecture** for API callsOnce the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

- Spring Boot backend running on `http://localhost:8080`

## 📋 Prerequisites## Code scaffolding

## 🚀 Quick Start

- Node.js (v18 or higher)Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

### 1. Install Dependencies

- npm (v9 or higher)

```bash

cd frontend- Spring Boot backend running on `http://localhost:8080````bash

npm install

```ng generate component component-name



### 2. Start Development Server## 🚀 Quick Start```



```bash### 1. Install DependenciesFor a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

npm start

````bash`bash



The application will be available at `http://localhost:4200`cd frontendng generate --help



### 3. Build for Productionnpm install```



```bash````

npm run build

```## Building



Production files will be in the `dist/` directory.### 2. Start Development Server



## 📁 Project StructureTo build the project run:



``````bash

frontend/

├── src/npm start```bash

│   ├── app/

│   │   ├── components/          # UI Components```ng build

│   │   │   ├── dashboard/       # Main dashboard

│   │   │   ├── task-list/       # Task activities view````

│   │   │   ├── user-list/       # User management

│   │   │   ├── dropdown-management/  # Dropdown valuesThe application will be available at `http://localhost:4200`

│   │   │   └── reports/         # Reports (coming soon)

│   │   ├── services/            # API ServicesThis will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

│   │   │   ├── task-activity.service.ts

│   │   │   ├── user.service.ts### 3. Build for Production

│   │   │   ├── dropdown.service.ts

│   │   │   └── auth.service.ts## Running unit tests

│   │   ├── models/              # TypeScript interfaces

│   │   │   └── task-activity.model.ts```bash

│   │   ├── guards/              # Route guards

│   │   │   ├── auth.guard.tsnpm run buildTo execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

│   │   │   └── admin.guard.ts

│   │   ├── interceptors/        # HTTP interceptors```

│   │   │   └── auth.interceptor.ts

│   │   ├── app.component.ts     # Root component```bash

│   │   ├── app.routes.ts        # Route configuration

│   │   └── app.config.ts        # App configurationProduction files will be in the `dist/` directory.ng test

│   ├── environments/            # Environment configs

│   │   ├── environment.ts       # Development```

│   │   └── environment.prod.ts  # Production

│   └── styles.scss              # Global styles## 📁 Project Structure

├── angular.json                 # Angular CLI config

├── package.json                 # Dependencies## Running end-to-end tests

└── tsconfig.json               # TypeScript config

```````



## 🎨 Featuresfrontend/For end-to-end (e2e) testing, run:



### Dashboard├── src/



- Overview cards for navigation│   ├── app/```bash

- Quick access to all modules

- Modern Material Design interface│   │   ├── components/          # UI Componentsng e2e

- Role-based visibility (ADMIN/GUEST features)

│   │   │   ├── dashboard/       # Main dashboard```

### Task Activities

│   │   │   ├── task-list/       # Task activities view

- View all task activities

- Filter by client, project, phase, and date range│   │   │   ├── user-list/       # User managementAngular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

- Paginated data table

- Real-time data from API│   │   │   └── dropdown-management/  # Dropdown values

- Edit and delete actions

│   │   ├── services/            # API Services## Additional Resources

### User Management

│   │   │   ├── task-activity.service.ts

- List all users (ADMIN/GUEST only)

- View user roles and status│   │   │   ├── user.service.tsFor more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

- Add/Edit/Delete users (ADMIN only)

- Role-based badges│   │   │   └── dropdown.service.ts

- Read-only mode for GUEST users│   │   ├── models/              # TypeScript interfaces

│   │   │   └── task-activity.model.ts

### Dropdown Management│   │   ├── app.component.ts     # Root component

│   │   ├── app.routes.ts        # Route configuration

- Tabbed interface for Clients, Projects, Phases (ADMIN/GUEST only)│   │   └── app.config.ts        # App configuration

- View and manage dropdown values│   ├── environments/            # Environment configs

- Active/Inactive status indicators│   │   ├── environment.ts       # Development

- Edit and delete actions (ADMIN only)│   │   └── environment.prod.ts  # Production

│   └── styles.scss              # Global styles

## 🔌 API Integration├── angular.json                 # Angular CLI config

├── package.json                 # Dependencies

The Angular app communicates with Spring Boot REST APIs:└── tsconfig.json               # TypeScript config

````

| Service             | Endpoint               | Purpose         |

| ------------------- | ---------------------- | --------------- |## 🎨 Features

| TaskActivityService | `/api/task-activities` | CRUD for tasks  |

| UserService         | `/api/users`           | User management |### Dashboard

| DropdownService     | `/api/dropdowns`       | Dropdown values |

- Overview cards for navigation

### Environment Configuration- Quick access to all modules

- Modern Material Design interface

**Development** (`src/environments/environment.ts`):

### Task Activities

```typescript

export const environment = {- View all task activities

  production: false,- Paginated data table

  apiUrl: "http://localhost:8080/api",- Real-time data from API

};- Edit and delete actions

```

### User Management

**Production** (`src/environments/environment.prod.ts`):

- List all users

```typescript- View user roles and status

export const environment = {- Add/Edit/Delete users

  production: true,- Role-based badges

  apiUrl: "/api",

};### Dropdown Management

```

- Tabbed interface for Clients, Projects, Phases

## 🔐 Authentication- View and manage dropdown values

- Active/Inactive status indicators

The Angular app integrates with Spring Security:

## 🔌 API Integration

- **Login**: Users authenticate via Spring Boot's `/login` page (Thymeleaf)

- **Session Management**: Spring Security manages the sessionThe Angular app communicates with Spring Boot REST APIs:

- **API Authentication**: HTTP Basic Auth credentials stored in session storage

- **Auth Guards**: Protect routes requiring authentication| Service             | Endpoint               | Purpose         |

- **Auth Interceptor**: Automatically adds Basic Auth headers to API requests| ------------------- | ---------------------- | --------------- |

- **Logout**: Redirects to Spring Security's `/logout` endpoint| TaskActivityService | `/api/task-activities` | CRUD for tasks  |

| UserService         | `/api/users`           | User management |

**No separate Angular login** - Single authentication point through Spring Security.| DropdownService     | `/api/dropdowns`       | Dropdown values |



## 🛠️ Development### Environment Configuration



### Running Both Applications**Development** (`src/environments/environment.ts`):



**Terminal 1 - Spring Boot Backend:**```typescript

export const environment = {

```bash  production: false,

# From project root  apiUrl: "http://localhost:8080/api",

mvnw.cmd spring-boot:run};

``````



Backend runs on: `http://localhost:8080`**Production** (`src/environments/environment.prod.ts`):



**Terminal 2 - Angular Frontend:**```typescript

export const environment = {

```bash  production: true,

# From frontend directory  apiUrl: "/api",

cd frontend};

npm start```

```

## 🛠️ Development

Frontend runs on: `http://localhost:4200`

### Running Both Applications

### Hot Reload

**Terminal 1 - Spring Boot Backend:**

Angular CLI provides hot reload - changes to TypeScript/HTML/CSS files will automatically reload the browser.

```bash

### Angular CLI Commands# From project root

mvnw.cmd spring-boot:run

```bash```

# Install new packages

npm install package-name**Terminal 2 - Angular Frontend:**



# Generate new component```bash

ng generate component components/my-component# From frontend directory

cd frontend

# Generate new servicenpm start

ng generate service services/my-service```



# Build for production### Hot Reload

npm run build

Angular CLI provides hot reload - changes to TypeScript/HTML/CSS files will automatically reload the browser.

# Run tests

npm test## 📚 Key Angular Concepts Demonstrated

```

### 1. Standalone Components

## 📚 Key Angular Concepts Demonstrated

All components use the new standalone API (no NgModules):

### 1. Standalone Components

```typescript

All components use the new standalone API (no NgModules):@Component({

  standalone: true,

```typescript  imports: [CommonModule, MatTableModule, ...]

@Component({})

  standalone: true,```

  imports: [CommonModule, MatTableModule, ...]

})### 2. Dependency Injection

```

Services are injected via constructor:

### 2. Dependency Injection

```typescript

Services are injected via constructor:constructor(private readonly taskService: TaskActivityService) {}

```

```typescript

constructor(private readonly taskService: TaskActivityService) {}### 3. RxJS Observables

```

Async data handling with observables:

### 3. RxJS Observables

```typescript

Async data handling with observables:this.taskService.getAllTasks().subscribe({

  next: (data) => (this.tasks = data),

```typescript  error: (err) => console.error(err),

this.taskService.getAllTasks().subscribe({});

  next: (data) => (this.tasks = data),```

  error: (err) => console.error(err),

});### 4. Routing

```

Declarative routing with Angular Router:

### 4. Routing

```typescript

Declarative routing with Angular Router:export const routes: Routes = [

  { path: "dashboard", component: DashboardComponent },

```typescript  { path: "tasks", component: TaskListComponent },

export const routes: Routes = [];

  { path: "dashboard", component: DashboardComponent, canActivate: [authGuard] },```

  { path: "tasks", component: TaskListComponent, canActivate: [authGuard] },

];### 5. Angular Material

```

Professional UI components:

### 5. Angular Material

- `MatTableModule` - Data tables

Professional UI components:- `MatButtonModule` - Buttons

- `MatCardModule` - Cards

- `MatTableModule` - Data tables- `MatToolbarModule` - Navigation

- `MatButtonModule` - Buttons- `MatSidenavModule` - Side navigation

- `MatCardModule` - Cards

- `MatToolbarModule` - Navigation### 6. TypeScript Interfaces

- `MatSidenavModule` - Side navigation

- `MatTabsModule` - Tabbed interfacesType-safe models:

- `MatIconModule` - Material icons

```typescript

### 6. TypeScript Interfacesexport interface TaskActivity {

  id?: number;

Type-safe models:  taskDate: string;

  clientName: string;

```typescript  // ...

export interface TaskActivity {}

  id?: number;```

  taskDate: string;

  client: string;## 🎓 Interview Preparation Topics

  project: string;

  phase: string;This project demonstrates knowledge of:

  hours: number;

  details: string;1. **Angular Framework**

  username: string;

}   - Components, Services, Routing

```   - Dependency Injection

   - Lifecycle hooks (ngOnInit)

### 7. HTTP Interceptors

2. **TypeScript**

Automatic request modification:

   - Interfaces and types

```typescript   - Access modifiers (readonly, private)

export const authInterceptor: HttpInterceptorFn = (req, next) => {   - Modern ES6+ features

  // Add auth headers automatically

  const credentials = sessionStorage.getItem('auth');3. **HTTP Communication**

  if (credentials) {

    req = req.clone({   - HttpClient for API calls

      setHeaders: { Authorization: `Basic ${credentials}` }   - Observable pattern

    });   - Error handling

  }

  return next(req);4. **UI/UX**

};

```   - Angular Material components

   - Responsive design

### 8. Route Guards   - Navigation patterns



Protect routes with guards:5. **Architecture**



```typescript   - Service-based architecture

export const authGuard = () => {   - Separation of concerns

  const authService = inject(AuthService);   - Environment configuration

  if (authService.isAuthenticated()) {

    return true;6. **Modern Development**

  }   - npm and package management

  globalThis.location.href = '/login';   - Angular CLI

  return false;   - Development vs Production builds

};

```## 🔧 Troubleshooting



## 🎓 Interview Preparation Topics### Backend Connection Issues



This project demonstrates knowledge of:If you see errors about failed API calls:



1. **Angular Framework**1. **Verify Spring Boot is running:**

   - Components, Services, Routing

   - Dependency Injection   ```bash

   - Lifecycle hooks (ngOnInit)   curl http://localhost:8080/api/health

   - Standalone components (Angular 19+)   ```



2. **TypeScript**2. **Check CORS configuration** in `SecurityConfig.java`

   - Interfaces and types

   - Access modifiers (readonly, private)3. **Verify API endpoints** match service URLs

   - Modern ES6+ features

### Port Already in Use

3. **HTTP Communication**

   - HttpClient for API callsIf port 4200 is in use:

   - Observable pattern

   - Error handling```bash

   - Interceptorsng serve --port 4201

```

4. **UI/UX**

   - Angular Material components## 📖 Additional Resources

   - Responsive design

   - Navigation patterns- [Angular Documentation](https://angular.dev)

   - Role-based UI visibility- [Angular Material](https://material.angular.io)

- [RxJS Documentation](https://rxjs.dev)

5. **Architecture**- [TypeScript Handbook](https://www.typescriptlang.org/docs/)

   - Service-based architecture

   - Separation of concerns## 🎯 Next Steps

   - Environment configuration

   - Guard-based route protectionTo enhance the application:



6. **Modern Development**1. **Add authentication** - Integrate with Spring Security

   - npm and package management2. **Form validation** - Add reactive forms with validation

   - Angular CLI3. **Charts & Analytics** - Add data visualization

   - Development vs Production builds4. **Real-time updates** - Implement WebSocket communication

   - Hot module replacement5. **Unit tests** - Add Jasmine/Karma tests

6. **E2E tests** - Add Protractor/Cypress tests

## 🔧 Troubleshooting

---

### Backend Connection Issues

**Good luck with your interview! 🚀**

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

## 🎯 Next Steps

To enhance the application:

1. **Add form validation** - Implement reactive forms with validators
2. **Add edit dialogs** - Create Material dialogs for inline editing
3. **Add charts & analytics** - Integrate Chart.js or ngx-charts
4. **Add real-time updates** - Implement WebSocket communication
5. **Unit tests** - Add Jasmine/Karma tests
6. **E2E tests** - Add Cypress tests

---

**This Angular application demonstrates modern full-stack development with Spring Boot backend integration!** 🚀
