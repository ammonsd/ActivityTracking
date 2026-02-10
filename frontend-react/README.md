# React Admin Dashboard

**Author:** Dean Ammons  
**Date:** February 2026

Modern React-based admin dashboard for Task Activity Management System built with TypeScript, Material-UI, and Vite.

## Technology Stack

- **React 19.2.0** - Latest React with modern hooks and concurrent features
- **TypeScript 5.9.3** - Type-safe development
- **Material-UI v7.3.7** - Google's Material Design component library
- **Vite 7.2.4** - Next-generation frontend build tool with HMR
- **Axios** - Promise-based HTTP client for API integration
- **Zustand** - Lightweight state management
- **React Router** - Client-side routing

## Features

### Implemented Features (Phase 3-7)

✅ **User Management** (Phase 4)
- Full CRUD operations with role assignment
- Filter by username, role, company
- Pagination with configurable rows per page
- Delete protection for active users
- Password validation and admin password change

✅ **Dropdown Management** (Phase 5)
- Manage dropdown values for TASK and EXPENSE categories
- Category/subcategory filtering with dynamic updates
- Add new categories with initial values
- Inline form for adding values to existing categories
- Edit values with display order and active status
- Delete confirmation with context information
- Summary statistics

✅ **Guest Activity Report** (Phase 6)
- Real-time login audit tracking
- Statistics dashboard with metric cards
- CSV export functionality
- Success rate tracking

✅ **Roles Management** (Phase 7)
- Comprehensive role and permission management
- Hierarchical permission selection
- Resource-level permission control
- Constraint validation for role deletion

### Upcoming Features

🔄 **System Settings** (Phase 8) - Coming soon

## Project Structure

```
frontend-react/
├── src/
│   ├── api/              # API service layer
│   │   ├── axios.client.ts
│   │   ├── auth.api.ts
│   │   ├── dropdown.api.ts
│   │   ├── guestActivity.api.ts
│   │   ├── rolesManagement.api.ts
│   │   └── userManagement.api.ts
│   ├── components/       # Reusable components
│   │   ├── common/
│   │   ├── dropdownManagement/
│   │   ├── guestActivity/
│   │   ├── layout/
│   │   ├── rolesManagement/
│   │   └── userManagement/
│   ├── config/          # Configuration files
│   │   └── features.ts
│   ├── pages/           # Page components
│   │   ├── DashboardHome.tsx
│   │   ├── DropdownManagement.tsx
│   │   ├── GuestActivity.tsx
│   │   ├── RolesManagement.tsx
│   │   └── UserManagement.tsx
│   ├── store/           # State management
│   │   └── authStore.ts
│   ├── types/           # TypeScript type definitions
│   │   ├── auth.types.ts
│   │   ├── dropdown.types.ts
│   │   ├── features.types.ts
│   │   ├── guestActivity.types.ts
│   │   ├── rolesManagement.types.ts
│   │   └── userManagement.types.ts
│   ├── utils/           # Utility functions
│   ├── App.tsx          # Main app component
│   └── main.tsx         # Entry point
├── public/              # Static assets
├── dist/                # Build output (gitignored)
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## Development

### Prerequisites

- Node.js 20.19+ or 22.12+ (Vite requirement)
- npm 10.2.4+
- Spring Boot backend running on port 8080

### Installation

```bash
cd frontend-react
npm install
```

### Development Server

```bash
npm run dev
```

Runs on **http://localhost:4201**

The dev server uses Vite proxy to forward API calls to Spring Boot backend (port 8080).

### Build for Production

```bash
npm run build
```

Builds static files to `dist/` directory. These files are automatically copied to `target/classes/static/dashboard/` by Maven during the Spring Boot build process.

### Type Checking

```bash
npm run type-check
```

### Linting

```bash
npm run lint
```

## Authentication

- **Session-based authentication** shared with Spring Boot backend
- **ADMIN-only access** enforced via Spring Security
- Automatic redirect to login if unauthorized (401)
- Session cookies work across `/api`, `/dashboard`, and root paths

## API Integration

All API calls go through `axios.client.ts` which:
- Uses relative paths (`/api`) to leverage Vite proxy
- Includes credentials (session cookies)
- Handles 401 errors with auto-redirect to login
- Preserves return URL for post-login redirect

## Deployment

### Development Mode
- React runs on Vite dev server (port 4201)
- Fast hot-reload for development
- Accessed via: `http://localhost:4201`

### Production Mode
- React built as static files by Maven
- Served by Spring Boot at `/dashboard`
- Accessed via: `http://localhost:8080/dashboard`
- Seamless integration with Spring Security

## Contributing

When adding new features:
1. Create TypeScript types in `src/types/`
2. Create API service in `src/api/`
3. Create components in `src/components/<feature>/`
4. Create page component in `src/pages/`
5. Add route in `src/App.tsx`
6. Update `src/config/features.ts`
7. Follow established Material-UI patterns
8. Add proper TypeScript types for all props and state

## Documentation

For more details, see:
- [Developer Guide](../docs/Developer_Guide.md)
- [Administrator User Guide](../docs/Administrator_User_Guide.md)
- [Technical Features Summary](../docs/Technical_Features_Summary.md)

---

**Note:** This React dashboard complements the existing Angular and Thymeleaf UIs, providing a modern admin interface for system management tasks.
