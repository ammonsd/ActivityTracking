# Jenkins Build Notifications

## Overview

This document describes the Jenkins build notification feature that sends email notifications to administrators when CI/CD builds succeed or fail.

## Implementation

### Components

1. **EmailService**: Added two new notification methods

    - `sendBuildSuccessNotification(buildNumber, branch, commit, buildUrl)`
    - `sendBuildFailureNotification(buildNumber, branch, commit, buildUrl, consoleUrl)`

2. **JenkinsBuildNotificationController**: REST API endpoints for Jenkins webhooks

    - `POST /api/jenkins/build-success` - Notify build success
    - `POST /api/jenkins/build-failure` - Notify build failure

3. **Database Permission**: Added `JENKINS:NOTIFY` permission to control access

### Configuration

#### Email Recipients

Build notifications are sent to the admin email addresses configured in `application.properties`:

```properties
# Admin email (comma-separated for multiple recipients)
app.mail.admin-email=admin@example.com,devops@example.com
```

#### Jenkinsfile Integration

Update your Jenkinsfile to call the notification endpoints:

```groovy
pipeline {
    agent any

    environment {
        APP_URL = 'https://taskactivity.example.com'
        JENKINS_API_TOKEN = credentials('jenkins-api-token')  // JWT token with JENKINS:NOTIFY permission
    }

    stages {
        // ... your build stages ...
    }

    post {
        success {
            script {
                sh """
                    curl -X POST ${APP_URL}/api/jenkins/build-success \\
                         -H "Content-Type: application/json" \\
                         -H "Authorization: Bearer ${JENKINS_API_TOKEN}" \\
                         -d '{
                             "buildNumber": "${BUILD_NUMBER}",
                             "branch": "${GIT_BRANCH}",
                             "commit": "${GIT_COMMIT}",
                             "buildUrl": "${BUILD_URL}"
                         }'
                """
            }
        }

        failure {
            script {
                sh """
                    curl -X POST ${APP_URL}/api/jenkins/build-failure \\
                         -H "Content-Type: application/json" \\
                         -H "Authorization: Bearer ${JENKINS_API_TOKEN}" \\
                         -d '{
                             "buildNumber": "${BUILD_NUMBER}",
                             "branch": "${GIT_BRANCH}",
                             "commit": "${GIT_COMMIT}",
                             "buildUrl": "${BUILD_URL}",
                             "consoleUrl": "${BUILD_URL}console"
                         }'
                """
            }
        }
    }
}
```

### Authentication Setup

Follow these steps to configure Jenkins to send build notifications.

#### Step 1: Create Jenkins Service Account (Option A - Use Existing Admin)

**Using AWS Console:**

1. **Log in to Task Activity Application**:

    - URL: `https://taskactivitytracker.com`
    - Use your existing ADMIN account credentials
    - The ADMIN role automatically has `JENKINS:NOTIFY` permission

2. **Skip to Step 2** - Your admin account can be used for Jenkins notifications

#### Step 1: Create Jenkins Service Account (Option B - Dedicated Account)

**If you want a separate service account, create user through the Task Activity UI:**

1. **Log in as ADMIN**:

    - URL: `https://taskactivitytracker.com`
    - Use your existing ADMIN account

2. **Navigate to User Management**:

    - Click on **User Management** in the application menu

3. **Create New User**:

    - Click **Add User** or **Create User** button
    - Fill in the form:
        - **Username**: `jenkins-service`
        - **Password**: `SecureJenkinsP@ssw0rd2026!` (generate a strong password and save it securely)
        - **Email**: `jenkins-service@taskactivity.com`
        - **First Name**: `Jenkins`
        - **Last Name**: `Service Account`
        - **Role**: Select **ADMIN** (ADMIN role already has JENKINS:NOTIFY permission)
    - Click **Save** or **Create**

4. **Verify User Created**:
    - You should see `jenkins-service` in the user list
    - Verify the role is set to ADMIN

**Note**: Since ADMIN role already has the `JENKINS:NOTIFY` permission (along with all other permissions), you don't need to create a custom role unless you want to restrict this account to only Jenkins notifications.

#### Step 1: Create Jenkins Service Account (Option C - Maximum Security with Custom Role)

**For strict least-privilege access, create a custom role with only JENKINS:NOTIFY permission:**

1. **Create custom JENKINS_SERVICE role via AWS RDS Query Editor**:

    - Go to AWS Console → RDS → Query Editor
    - Connect to your PostgreSQL database
    - Run this SQL:

    ```sql
    -- Create a dedicated role for Jenkins service
    INSERT INTO roles (name, description) VALUES
        ('JENKINS_SERVICE', 'Service account role for Jenkins CI/CD integration')
    ON CONFLICT (name) DO NOTHING;

    -- Assign only the JENKINS:NOTIFY permission to this role
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id
    FROM roles r
    JOIN permissions p ON p.resource = 'JENKINS' AND p.action = 'NOTIFY'
    WHERE r.name = 'JENKINS_SERVICE'
    ON CONFLICT (role_id, permission_id) DO NOTHING;
    ```

2. **Create user via Task Activity UI**:

    - Follow the UI steps from **Option B** above
    - When selecting role, choose **JENKINS_SERVICE** instead of ADMIN

3. **Alternative - Create user via API** (if UI is not accessible):

    ```bash
    # First, get admin JWT token
    ADMIN_TOKEN=$(curl -s -X POST https://taskactivitytracker.com/api/auth/login \
         -H "Content-Type: application/json" \
         -d '{"username": "admin", "password": "YOUR_ADMIN_PASSWORD"}' \
         | jq -r '.accessToken')

    # Create jenkins-service user (creates with default USER role)
    curl -X POST https://taskactivitytracker.com/api/user-management/users \
         -H "Content-Type: application/json" \
         -H "Authorization: Bearer $ADMIN_TOKEN" \
         -d '{
             "username": "jenkins-service",
             "password": "SecureJenkinsP@ssw0rd2026!",
             "email": "jenkins-service@taskactivity.com",
             "firstName": "Jenkins",
             "lastName": "Service Account"
         }'
    ```

4. **Update user role to JENKINS_SERVICE** (via AWS RDS Query Editor):
    ```sql
    UPDATE users
    SET role_id = (SELECT id FROM roles WHERE name = 'JENKINS_SERVICE')
    WHERE username = 'jenkins-service';
    ```

**Recommendation**: Use **Option A** (existing ADMIN account) for simplicity, or **Option B** (dedicated account with ADMIN role) for better separation. Only use **Option C** if your security policy requires strict least-privilege access control.

#### Step 2: Generate JWT Access Token

1. **Log in to get JWT token**:

    ```bash
    curl -X POST https://taskactivitytracker.com/api/auth/login \
         -H "Content-Type: application/json" \
         -d '{
             "username": "admin",
             "password": "YOUR_PASSWORD"
         }'
    ```

2. **Extract the access token** from the response:

    ```json
    {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTY0...",
        "refreshToken": "...",
        "expiresIn": 86400000
    }
    ```

3. **Copy the `accessToken` value** - this is your JWT token

**IMPORTANT NOTES:**

-   JWT tokens expire after 24 hours by default (configurable via `jwt.expiration`)
-   For long-term use, consider using refresh tokens or extending token expiration
-   Store tokens securely - they provide full access to the endpoints

#### Step 3: Store JWT Token in Jenkins Credentials

**Using Jenkins UI:**

1. **Navigate to Jenkins Credentials**:

    - Jenkins Dashboard → Manage Jenkins → Manage Credentials
    - Click on **(global)** domain

2. **Add New Credential**:

    - Click **Add Credentials**
    - **Kind**: Select `Secret text`
    - **Scope**: `Global`
    - **Secret**: Paste your JWT token (the long string starting with `eyJ...`)
    - **ID**: `jenkins-api-token`
    - **Description**: `Task Activity JWT token for build notifications`
    - Click **OK**

3. **Verify Credential**:
    - You should see `jenkins-api-token` in the credentials list
    - The secret will be masked as `******`

#### Step 4: Update Jenkinsfile

Add the notification calls to your Jenkinsfile's `post` section. See the **Jenkinsfile Integration** section above for the complete example.

**Key Points:**

-   Use `credentials('jenkins-api-token')` to reference the stored token
-   Set `APP_URL` environment variable to your application URL
-   Add notification calls in both `success` and `failure` post blocks

#### Step 5: Configure Email Settings (AWS Console)

**If email is not already configured:**

1. **Update Environment Variables in ECS Task Definition** (via AWS Console):

    - Go to AWS Console → ECS → Task Definitions → Your task definition
    - Click **Create new revision**
    - Scroll to **Environment variables** section
    - Add/Update these variables:
        ```
        MAIL_ENABLED=true
        ADMIN_EMAIL=deanammons@gmail.com
        MAIL_FROM=noreply@taskactivity.com
        ```
    - Click **Create** to save new revision

2. **Update ECS Service to use new task definition revision**:

    - Go to ECS → Clusters → Your cluster → Your service
    - Click **Update**
    - Select the new task definition revision
    - Click **Update Service**

3. **Verify AWS SES is configured**:
    - The application uses `spring.mail.use-aws-sdk=true` for AWS deployments
    - Ensure your ECS task role has SES permissions
    - Verify sender email is verified in AWS SES

#### Step 6: Test the Integration

1. **Trigger a Jenkins build**
2. **Check Jenkins console output** for the curl command execution
3. **Verify email received** at `deanammons@gmail.com`
4. **Check application logs** in CloudWatch:
    ```
    Build success notification sent to deanammons@gmail.com for build: 123
    ```

#### Troubleshooting Token Expiration

**If your JWT token expires:**

1. **Generate new token** (repeat Step 2)
2. **Update Jenkins credential**:
    - Go to Manage Jenkins → Manage Credentials
    - Click on `jenkins-api-token`
    - Click **Update**
    - Paste new token in **Secret** field
    - Click **Save**

**To extend token lifetime** (requires AWS Console):

1. **Update ECS Task Definition environment variables**:
    ```
    JWT_EXPIRATION=604800000    # 7 days in milliseconds
    ```
2. **Deploy new task definition revision**
3. **Regenerate token** - new tokens will have extended expiration

### Email Content

**Success Email Example:**

```
✅ JENKINS BUILD SUCCESS

Build Details:
----------------------------------------
Build Number:       72
Branch:             main
Commit:             abc1234
Status:             SUCCESS
Timestamp:          2026-01-15 10:30:00
----------------------------------------

Links:
View build: https://jenkins.example.com/job/taskactivity/72/

---
This is an automated notification from Task Activity Management System CI/CD Pipeline.
Do not reply to this email.
```

**Failure Email Example:**

```
❌ JENKINS BUILD FAILURE

Build Details:
----------------------------------------
Build Number:       73
Branch:             develop
Commit:             def5678
Status:             FAILURE
Timestamp:          2026-01-15 11:45:00
----------------------------------------

Links:
View build: https://jenkins.example.com/job/taskactivity/73/
View logs:  https://jenkins.example.com/job/taskactivity/73/console

---
This is an automated notification from Task Activity Management System CI/CD Pipeline.
Do not reply to this email.
```

## API Documentation

### POST /api/jenkins/build-success

**Description**: Send build success notification

**Authentication**: Required (JWT Bearer token with `JENKINS:NOTIFY` permission)

**Request Body**:

```json
{
    "buildNumber": "72",
    "branch": "main",
    "commit": "abc1234",
    "buildUrl": "https://jenkins.example.com/job/taskactivity/72/"
}
```

**Response** (200 OK):

```json
{
    "success": true,
    "message": "Build success notification sent for build: 72",
    "data": null
}
```

### POST /api/jenkins/build-failure

**Description**: Send build failure notification

**Authentication**: Required (JWT Bearer token with `JENKINS:NOTIFY` permission)

**Request Body**:

```json
{
    "buildNumber": "73",
    "branch": "develop",
    "commit": "def5678",
    "buildUrl": "https://jenkins.example.com/job/taskactivity/73/",
    "consoleUrl": "https://jenkins.example.com/job/taskactivity/73/console"
}
```

**Response** (200 OK):

```json
{
    "success": true,
    "message": "Build failure notification sent for build: 73",
    "data": null
}
```

## Testing

### Manual Testing with curl

**Test Success Notification**:

```bash
curl -X POST https://taskactivity.example.com/api/jenkins/build-success \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -d '{
         "buildNumber": "test-1",
         "branch": "main",
         "commit": "abc1234",
         "buildUrl": "https://jenkins.example.com/job/taskactivity/test-1/"
     }'
```

**Test Failure Notification**:

```bash
curl -X POST https://taskactivity.example.com/api/jenkins/build-failure \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -d '{
         "buildNumber": "test-2",
         "branch": "develop",
         "commit": "def5678",
         "buildUrl": "https://jenkins.example.com/job/taskactivity/test-2/",
         "consoleUrl": "https://jenkins.example.com/job/taskactivity/test-2/console"
     }'
```

## Security Considerations

-   **Authentication Required**: All notification endpoints require valid JWT authentication
-   **Permission-Based**: Only users with `JENKINS:NOTIFY` permission can trigger notifications
-   **Service Account**: Use a dedicated service account for Jenkins integration
-   **Token Security**: Store JWT tokens securely in Jenkins credentials store
-   **HTTPS Only**: Always use HTTPS for API communication

## Troubleshooting

### Email Not Received

1. **Check email configuration**:

    - Verify `spring.mail.enabled=true` in application.properties
    - Ensure `app.mail.admin-email` is configured correctly
    - Check AWS SES or SMTP settings

2. **Check application logs**:
    ```
    grep "Build.*notification sent" /var/log/taskactivity.log
    ```

### Authentication Failures

1. **Verify JWT token**:

    - Check token is not expired (15-minute default)
    - Verify token has `JENKINS:NOTIFY` permission
    - Ensure `Authorization: Bearer TOKEN` header format is correct

2. **Check permissions**:
    ```sql
    SELECT * FROM permissions WHERE resource = 'JENKINS' AND action = 'NOTIFY';
    SELECT * FROM role_permissions WHERE permission_id = (SELECT id FROM permissions WHERE resource = 'JENKINS' AND action = 'NOTIFY');
    ```

## Benefits

-   **Immediate Notification**: Team is notified of build failures within seconds
-   **Centralized Emails**: All build notifications go to configured admin addresses
-   **Build Context**: Emails include build number, branch, commit, and links to Jenkins
-   **Integration**: Seamlessly integrates with existing Jenkins pipeline
-   **Security**: Protected by JWT authentication and permission system

## Future Enhancements

-   Slack/Teams integration for instant messaging notifications
-   Build status dashboard in Task Activity UI
-   Configurable notification rules (e.g., only notify on main branch failures)
-   Build metrics tracking (success rate, average build time)
-   Digest emails for daily/weekly build summaries
