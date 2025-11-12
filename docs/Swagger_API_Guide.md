# Swagger API Guide

## Task Activity Management API Documentation

This guide provides comprehensive instructions for using the Task Activity Management REST API through Swagger UI.

---

## Table of Contents

1. [Accessing Swagger UI](#accessing-swagger-ui)
2. [Authentication Overview](#authentication-overview)
3. [Getting Started with JWT Authentication](#getting-started-with-jwt-authentication)
4. [Using the "Try It Out" Feature](#using-the-try-it-out-feature)
5. [Common API Workflows](#common-api-workflows)
6. [API Endpoints Overview](#api-endpoints-overview)
7. [Error Handling](#error-handling)
8. [Troubleshooting](#troubleshooting)

---

## Accessing Swagger UI

The Swagger UI is available at the following URLs when the application is running:

-   **Primary URL**: `http://localhost:8080/swagger-ui.html`
-   **Alternative URL**: `http://localhost:8080/swagger-ui/index.html`

The OpenAPI specification (JSON format) is available at:

-   `http://localhost:8080/v3/api-docs`

---

## Authentication Overview

The Task Activity Management application uses **two different authentication mechanisms**:

### 1. Form-Based Authentication (Web UI)

-   Used by the web application interface
-   Session-based with cookies
-   Login at `/login` with username and password
-   Suitable for browser-based access

### 2. JWT Authentication (REST API)

-   Used by REST API clients and Swagger UI
-   Token-based (stateless)
-   Login via `POST /api/auth/login`
-   Suitable for API integrations, mobile apps, and testing

### Important: Already Logged In?

**If you're already logged into the web application**, you're automatically authenticated in Swagger UI as well! Your browser sends the session cookie (JSESSIONID) with Swagger requests, so you can immediately use the "Try It Out" feature on any endpoint without needing to use the JWT "Authorize" button.

**When to use JWT authentication in Swagger:**

-   Testing API access **independently** of the web UI session
-   Simulating how external API clients (mobile apps, third-party services) would authenticate
-   Testing with **different user roles** while maintaining your web UI session
-   Development of stateless API integrations

**This guide focuses on JWT authentication for API access.**

---

## Getting Started with JWT Authentication

### Step 1: Obtain JWT Tokens

1. Navigate to Swagger UI at `http://localhost:8080/swagger-ui.html`
2. Scroll down to the **"Authentication"** section
3. Expand the **POST /api/auth/login** endpoint
4. Click the **"Try it out"** button
5. Enter your credentials in the request body:

```json
{
    "username": "admin",
    "password": "your_password"
}
```

6. Click **"Execute"**
7. You should receive a **200 OK** response with tokens:

```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "username": "admin"
}
```

8. **Copy the `accessToken` value** (the long string after "accessToken":)

### Step 2: Authorize in Swagger UI

1. Click the **"Authorize"** button (🔒 lock icon) at the top right of the Swagger UI page
2. A dialog box will appear titled "Available authorizations"
3. In the "Value" field, enter **ONLY your token** (do NOT include "Bearer"):
    ```
    eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    ```
    ⚠️ **Important**: Enter ONLY the token string. Swagger UI automatically adds "Bearer " prefix for you.
4. Click the **"Authorize"** button in the dialog
5. Click **"Close"** to close the dialog
6. The lock icon should now appear closed/locked (🔒) indicating you're authenticated

### Step 3: Make Authenticated Requests

Now you can use the "Try it out" button on any API endpoint, and your JWT token will be automatically included in the request headers as `Authorization: Bearer <your-token>`.

---

## Using the "Try It Out" Feature

Once authenticated, you can test any API endpoint:

1. **Expand the endpoint** you want to test (e.g., GET /api/tasks)
2. Click **"Try it out"**
3. **Fill in any required parameters**:
    - Path parameters (e.g., task ID)
    - Query parameters (e.g., filters, pagination)
    - Request body (for POST/PUT requests)
4. Click **"Execute"**
5. View the response:
    - **Response body**: The actual data returned
    - **Response code**: HTTP status (200, 201, 400, 401, etc.)
    - **Response headers**: Metadata about the response
    - **Curl command**: A curl command you can copy and run in terminal

---

## Common API Workflows

### 1. Listing All Tasks

**Endpoint**: `GET /api/tasks`

```
1. Authorize with your JWT token (see above)
2. Expand GET /api/tasks
3. Click "Try it out"
4. Optionally add query parameters:
   - page: 0 (first page)
   - size: 20 (items per page)
   - sort: startDate,desc (sort by start date descending)
5. Click "Execute"
```

**Expected Response**: `200 OK` with list of tasks

### 2. Creating a New Task Activity

**Endpoint**: `POST /api/task-activities`

**Purpose**: Create a new task activity record. Used by both the "Add Task" and "Clone Task" features in the Angular dashboard.

**Usage Scenarios:**

- **Add New Task**: Create a task activity from scratch
- **Clone Existing Task**: Duplicate a task with today's date (Angular dashboard automatically removes the ID before calling this endpoint)
- **API Integration**: Programmatically create task activities from external systems

**Request Body Example:**

```json
{
    "taskDate": "2025-11-10",
    "client": "Acme Corporation",
    "project": "Website Redesign",
    "phase": "Development",
    "hours": 6.5,
    "details": "Implemented responsive navigation menu",
    "username": "jdoe"
}
```

**Steps to Test:**

```
1. Expand POST /api/task-activities
2. Click "Try it out"
3. Modify the request body with your task data:
   - taskDate: Required (format: YYYY-MM-DD)
   - client: Required (must match dropdown value)
   - project: Required (must match dropdown value)
   - phase: Required (must match dropdown value)
   - hours: Required (0-24)
   - details: Optional
   - username: Required (automatically set by Angular for logged-in user)
4. Click "Execute"
```

**Expected Response**: `201 Created` with the new task activity details including the generated ID

**Response Example:**

```json
{
    "id": 123,
    "taskDate": "2025-11-10",
    "client": "Acme Corporation",
    "project": "Website Redesign",
    "phase": "Development",
    "hours": 6.5,
    "details": "Implemented responsive navigation menu",
    "username": "jdoe"
}
```

**Validation Rules:**

- `taskDate`: Must be a valid date, cannot be null
- `client`, `project`, `phase`: Must exist in dropdown values
- `hours`: Must be between 0 and 24
- `username`: Must be a valid user, automatically set by the application
- `details`: Optional, maximum 1000 characters

**Common Use Cases:**

| Use Case | Description | Angular Component |
|----------|-------------|-------------------|
| **Add Task** | User clicks "Add Task" button, fills form, saves | `TaskListComponent.addTask()` |
| **Clone Task** | User clicks clone icon, modifies cloned data, saves | `TaskListComponent.cloneTask()` |
| **Bulk Import** | External system imports multiple task records | API client with authentication |
| **Mobile App** | Mobile application creates task activities | Mobile app with JWT tokens |

**Security:**

- Requires authentication (JWT token or session)
- Users can only create tasks for themselves (username is validated)
- Administrators can create tasks for any user

### 3. Updating an Existing Task Activity

**Endpoint**: `PUT /api/task-activities/{id}`

**Purpose**: Update an existing task activity record.

```
1. Expand PUT /api/task-activities/{id}
2. Click "Try it out"
3. Enter the task ID in the path parameter
4. Modify the request body with updated values
5. Click "Execute"
```

**Important**: The `username` field is **immutable** and cannot be changed. The API will preserve the original username even if a different value is provided in the request.

**Expected Response**: `200 OK` with updated task activity details

### 4. Searching Task Activities

**Endpoint**: `POST /api/task-activities/search`

```
1. Expand POST /api/tasks/search
2. Click "Try it out"
3. Provide search criteria:
```

```json
{
    "projectId": 1,
    "startDate": "2025-11-01T00:00:00",
    "endDate": "2025-11-30T23:59:59"
}
```

```
4. Click "Execute"
```

**Expected Response**: `200 OK` with matching tasks

### 5. Deleting a Task

**Endpoint**: `DELETE /api/tasks/{id}`

```
1. Expand DELETE /api/tasks/{id}
2. Click "Try it out"
3. Enter the task ID to delete
4. Click "Execute"
```

**Expected Response**: `204 No Content` (successful deletion)

### 6. Refreshing Your Access Token

When your access token expires (after 24 hours), use the refresh token:

> **Note for Swagger UI Testing**: For manual testing in Swagger UI, it's usually easier to simply login again at `/api/auth/login` rather than using the refresh endpoint. The refresh token feature is primarily designed for **automated API clients** (mobile apps, background services, integrations) that need to maintain long-running sessions without storing user credentials.

**Endpoint**: `POST /api/auth/refresh`

```
1. Expand POST /api/auth/refresh
2. Click "Try it out"
3. Enter your refresh token:
```

```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

```
4. Click "Execute"
5. You'll receive a new accessToken
6. Re-authorize using the new token (see Step 2 above)
```

**Expected Response**: `200 OK` with new access token

---

## API Endpoints Overview

### Authentication Endpoints

| Method | Endpoint            | Description                     | Auth Required |
| ------ | ------------------- | ------------------------------- | ------------- |
| POST   | `/api/auth/login`   | Authenticate and get JWT tokens | No            |
| POST   | `/api/auth/refresh` | Refresh access token            | No            |

### Task Endpoints

| Method | Endpoint            | Description                | Auth Required |
| ------ | ------------------- | -------------------------- | ------------- |
| GET    | `/api/tasks`        | Get all tasks (paginated)  | Yes           |
| GET    | `/api/tasks/{id}`   | Get task by ID             | Yes           |
| POST   | `/api/tasks`        | Create new task            | Yes           |
| PUT    | `/api/tasks/{id}`   | Update existing task       | Yes           |
| DELETE | `/api/tasks/{id}`   | Delete task                | Yes           |
| POST   | `/api/tasks/search` | Search tasks with criteria | Yes           |

### Project Endpoints

| Method | Endpoint             | Description        | Auth Required |
| ------ | -------------------- | ------------------ | ------------- |
| GET    | `/api/projects`      | Get all projects   | Yes           |
| GET    | `/api/projects/{id}` | Get project by ID  | Yes           |
| POST   | `/api/projects`      | Create new project | Yes           |
| PUT    | `/api/projects/{id}` | Update project     | Yes           |
| DELETE | `/api/projects/{id}` | Delete project     | Yes           |

### Client Endpoints

| Method | Endpoint            | Description       | Auth Required |
| ------ | ------------------- | ----------------- | ------------- |
| GET    | `/api/clients`      | Get all clients   | Yes           |
| GET    | `/api/clients/{id}` | Get client by ID  | Yes           |
| POST   | `/api/clients`      | Create new client | Yes           |
| PUT    | `/api/clients/{id}` | Update client     | Yes           |
| DELETE | `/api/clients/{id}` | Delete client     | Yes           |

### User Management Endpoints

| Method | Endpoint          | Description     | Auth Required | Role Required |
| ------ | ----------------- | --------------- | ------------- | ------------- |
| GET    | `/api/users`      | Get all users   | Yes           | ADMIN         |
| GET    | `/api/users/{id}` | Get user by ID  | Yes           | ADMIN         |
| POST   | `/api/users`      | Create new user | Yes           | ADMIN         |
| PUT    | `/api/users/{id}` | Update user     | Yes           | ADMIN         |
| DELETE | `/api/users/{id}` | Delete user     | Yes           | ADMIN         |

### Health Check Endpoints

| Method | Endpoint           | Description               | Auth Required |
| ------ | ------------------ | ------------------------- | ------------- |
| GET    | `/api/health`      | Application health status | No            |
| GET    | `/actuator/health` | Actuator health check     | No            |

---

## Error Handling

### Common HTTP Status Codes

| Status Code                   | Meaning            | When It Occurs                           |
| ----------------------------- | ------------------ | ---------------------------------------- |
| **200 OK**                    | Success            | GET, PUT requests completed successfully |
| **201 Created**               | Resource created   | POST request created new resource        |
| **204 No Content**            | Success, no body   | DELETE request completed successfully    |
| **400 Bad Request**           | Invalid input      | Validation errors, malformed request     |
| **401 Unauthorized**          | Not authenticated  | Missing or invalid JWT token             |
| **403 Forbidden**             | Not authorized     | Valid token but insufficient permissions |
| **404 Not Found**             | Resource not found | Requested resource doesn't exist         |
| **500 Internal Server Error** | Server error       | Unexpected server-side error             |

### Example Error Responses

**401 Unauthorized** (Missing or expired token):

```json
{
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource"
}
```

**400 Bad Request** (Validation error):

```json
{
    "timestamp": "2025-11-10T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "errors": [
        {
            "field": "taskName",
            "message": "Task name is required"
        }
    ]
}
```

**404 Not Found**:

```json
{
    "timestamp": "2025-11-10T10:30:00",
    "status": 404,
    "error": "Not Found",
    "message": "Task not found with id: 999"
}
```

---

## Troubleshooting

### "Try It Out" Buttons Not Working

**Problem**: Clicking "Try it out" does nothing or requests fail with 401 errors.

**Solution**:

1. Make sure you've obtained a JWT token from `/api/auth/login`
2. Click the "Authorize" button (🔒) at the top
3. Enter your token as: `Bearer <your-token>`
4. Ensure there's a space after "Bearer"
5. Click "Authorize" and close the dialog

### Token Expired

**Problem**: You get 401 errors even though you were previously authenticated.

**Solution**:

-   Access tokens expire after 24 hours
-   Use the `/api/auth/refresh` endpoint with your refresh token
-   Refresh tokens are valid for 7 days
-   After 7 days, you'll need to log in again with username/password

### Invalid Credentials

**Problem**: `/api/auth/login` returns 401 with "Invalid username or password"

**Solution**:

1. Verify your username and password are correct
2. Check if your account is active (not disabled)
3. Contact your administrator if you've forgotten your password

### CORS Errors (when calling from external application)

**Problem**: Browser console shows CORS errors when calling API from a different domain.

**Solution**:

-   The API is configured to allow all origins (`*`)
-   If you still see CORS errors, check:
    -   Are you including credentials in the request?
    -   Is the Authorization header being sent correctly?
    -   Check browser console for specific CORS error details

### Request Body Format Errors

**Problem**: 400 Bad Request with "Malformed JSON" or similar errors.

**Solution**:

1. Ensure your JSON is properly formatted (use Swagger UI's example as a template)
2. Check for:
    - Missing commas between fields
    - Missing quotes around strings
    - Incorrect date/time formats (use ISO 8601: `YYYY-MM-DDTHH:mm:ss`)
    - Required fields are included

### Can't See Swagger UI

**Problem**: Swagger UI page doesn't load.

**Solution**:

1. Ensure the application is running
2. Check the correct URL: `http://localhost:8080/swagger-ui.html`
3. If running on a different port, update the URL accordingly
4. Check application logs for startup errors

---

## Best Practices

### 1. Token Management

-   Store tokens securely (never in browser localStorage for sensitive apps)
-   Implement token refresh logic in your client application
-   Clear tokens on logout

### 2. Error Handling

-   Always check response status codes
-   Implement proper error handling in your client
-   Display user-friendly error messages

### 3. Pagination

-   Use pagination parameters for large result sets
-   Default page size is 20, adjust as needed
-   Use sorting to get relevant results first

### 4. Date/Time Formats

-   Always use ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`
-   Example: `2025-11-10T14:30:00`
-   Include timezone information if needed

### 5. Testing

-   Test with invalid data to understand error responses
-   Test authorization with different user roles
-   Verify all required fields are provided

---

## Additional Resources

-   **OpenAPI Specification**: `/v3/api-docs`
-   **Application Documentation**: See `/docs` folder for additional guides
-   **Developer Guide**: `docs/Developer_Guide.md`
-   **User Guide**: `docs/User_Guide.md`

---

## Support

For issues or questions:

1. Check the application logs for detailed error messages
2. Review this guide's Troubleshooting section
3. Contact your system administrator
4. Review the Developer Guide for technical details

---

**Version**: 1.0  
**Last Updated**: November 10, 2025  
**Author**: Dean Ammons
