# Jenkins Notification Email Separation

## Overview

Jenkins notification emails have been separated into two distinct categories to allow different recipient lists for build vs deployment notifications.

## Changes Made

### Email Configuration Properties

**Before (Single Property):**

```properties
app.mail.jenkins-notification-email=${JENKINS_NOTIFICATION_EMAIL:deanammons@gmail.com}
```

**After (Separate Properties):**

```properties
# Build notifications - Developers only
app.mail.jenkins-build-notification-email=${JENKINS_BUILD_NOTIFICATION_EMAIL:deanammons@gmail.com}

# Deploy notifications - Developers + Business Analysts
app.mail.jenkins-deploy-notification-email=${JENKINS_DEPLOY_NOTIFICATION_EMAIL:deanammons@gmail.com}

# Deploy skipped check - Enable/disable skipped deployment notifications (optional)
# When true, sends email notification when scheduled deployment is skipped (no new builds)
# Controlled via .env file and update-email-addresses.ps1 script
JENKINS_DEPLOY_SKIPPED_CHECK=${JENKINS_DEPLOY_SKIPPED_CHECK:false}
```

### Environment Variables

**Build Notifications (.env example):**

```env
JENKINS_BUILD_NOTIFICATION_EMAIL=dev-team@company.com
```

**Deploy Notifications (.env example):**

```env
JENKINS_DEPLOY_NOTIFICATION_EMAIL=dev-team@company.com,ba-team@company.com
```

**Deploy Skipped Check (.env example):**

```env
# Enable skipped deployment notifications (set to false to disable)
JENKINS_DEPLOY_SKIPPED_CHECK=true
```

### New API Endpoints

Added three new REST endpoints for deployment notifications:

#### Deploy Success

```
POST /api/jenkins/deploy-success
```

**Request Body:**

```json
{
    "buildNumber": "123",
    "branch": "main",
    "commit": "abc1234",
    "deployUrl": "https://jenkins.example.com/job/deploy/123/",
    "environment": "production"
}
```

#### Deploy Failure

```
POST /api/jenkins/deploy-failure
```

**Request Body:**

```json
{
    "buildNumber": "123",
    "branch": "main",
    "commit": "abc1234",
    "deployUrl": "https://jenkins.example.com/job/deploy/123/",
    "consoleUrl": "https://jenkins.example.com/job/deploy/123/console",
    "environment": "production"
}
```

#### Deploy Skipped

```
POST /api/jenkins/deploy-skipped
```

**Request Body:**

```json
{
    "buildNumber": "123",
    "branch": "main",
    "commit": "abc1234",
    "buildUrl": "https://jenkins.example.com/job/deploy/123/",
    "environment": "production",
    "reason": "No new builds since last deployment",
    "triggeredBy": "scheduled"
}
```

**Note:** This endpoint is only called when `JENKINS_DEPLOY_SKIPPED_CHECK=true` in the ECS task definition. Jenkins automatically reads this value from the running ECS task at runtime.

### Email Service Methods

**New Methods:**

-   `sendDeploySuccessNotification()` - Sends success notification to deploy email list
-   `sendDeployFailureNotification()` - Sends failure notification to deploy email list
-   `sendDeploySkippedNotification()` - Sends skipped notification to deploy email list (when enabled)

**Updated Methods:**

-   `sendBuildSuccessNotification()` - Now uses `jenkinsBuildNotificationEmail`
-   `sendBuildFailureNotification()` - Now uses `jenkinsBuildNotificationEmail`

## Usage in Jenkinsfile

### Build Stage Notifications

```groovy
pipeline {
    agent any

    environment {
        APP_URL = 'https://taskactivity.example.com'
        JENKINS_API_TOKEN = credentials('jenkins-api-token')
    }

    stages {
        stage('Build') {
            steps {
                sh './mvnw clean package'
            }
        }
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

### Deploy Stage Notifications

```groovy
pipeline {
    agent any

    environment {
        APP_URL = 'https://taskactivity.example.com'
        JENKINS_API_TOKEN = credentials('jenkins-api-token')
        DEPLOY_ENV = 'production'  // or 'staging', 'development'
    }

    stages {
        stage('Deploy') {
            steps {
                sh './deploy-to-aws.sh'
            }
        }
    }

    post {
        success {
            script {
                sh """
                    curl -X POST ${APP_URL}/api/jenkins/deploy-success \\
                         -H "Content-Type: application/json" \\
                         -H "Authorization: Bearer ${JENKINS_API_TOKEN}" \\
                         -d '{
                             "buildNumber": "${BUILD_NUMBER}",
                             "branch": "${GIT_BRANCH}",
                             "commit": "${GIT_COMMIT}",
                             "deployUrl": "${BUILD_URL}",
                             "environment": "${DEPLOY_ENV}"
                         }'
                """
            }
        }
        failure {
            script {
                sh """
                    curl -X POST ${APP_URL}/api/jenkins/deploy-failure \\
                         -H "Content-Type: application/json" \\
                         -H "Authorization: Bearer ${JENKINS_API_TOKEN}" \\
                         -d '{
                             "buildNumber": "${BUILD_NUMBER}",
                             "branch": "${GIT_BRANCH}",
                             "commit": "${GIT_COMMIT}",
                             "deployUrl": "${BUILD_URL}",
                             "consoleUrl": "${BUILD_URL}console",
                             "environment": "${DEPLOY_ENV}"
                         }'
                """
            }
        }
    }
}
```

## Email Recipients

### Build Notifications

-   **Purpose**: Notify about compilation success/failure
-   **Recipients**: Development team only
-   **Frequency**: Every build
-   **Example**: `dev-team@company.com`

### Deploy Notifications

-   **Purpose**: Notify about deployment success/failure to environments
-   **Recipients**: Development team AND business analysts
-   **Frequency**: Every deployment (staging, production, etc.)
-   **Example**: `dev-team@company.com,ba-team@company.com`

### Deploy Skipped Notifications

-   **Purpose**: Notify when scheduled deployment is skipped (no new builds available)
-   **Recipients**: Same as build notifications (`JENKINS_BUILD_NOTIFICATION_EMAIL`) - support team only, not business users
-   **Frequency**: Only when scheduled deployments are skipped AND `JENKINS_DEPLOY_SKIPPED_CHECK=true`
-   **Control**: Managed via `.env` file - no Jenkinsfile changes needed
-   **Example**: Set `JENKINS_DEPLOY_SKIPPED_CHECK=true` in `.env`, then run `.\aws\update-email-addresses.ps1 -DeployToAws`
-   **Rationale**: Skipped deployments are operational notifications for the support team, not information business users need

## Configuration Examples

### Local Development (.env)

```env
JENKINS_BUILD_NOTIFICATION_EMAIL=developer@localhost.com
JENKINS_DEPLOY_NOTIFICATION_EMAIL=developer@localhost.com,manager@localhost.com
JENKINS_DEPLOY_SKIPPED_CHECK=true
```

### Staging Environment

```env
JENKINS_BUILD_NOTIFICATION_EMAIL=dev-team@company.com
JENKINS_DEPLOY_NOTIFICATION_EMAIL=dev-team@company.com,qa-team@company.com,ba-team@company.com
JENKINS_DEPLOY_SKIPPED_CHECK=true
```

### Production Environment

```env
JENKINS_BUILD_NOTIFICATION_EMAIL=dev-team@company.com
JENKINS_DEPLOY_NOTIFICATION_EMAIL=dev-team@company.com,ba-team@company.com,operations@company.com,management@company.com
JENKINS_DEPLOY_SKIPPED_CHECK=false  # Disable skipped notifications in production if desired
```

## Managing Email Configuration

### Updating Email Addresses and Settings

All Jenkins notification settings (email addresses and skipped deployment check) are managed through the `.env` file and the `update-email-addresses.ps1` script. **You do NOT need to modify the Jenkinsfile.**

**Step-by-step process:**

1. **Update .env file:**

    ```env
    JENKINS_BUILD_NOTIFICATION_EMAIL=new-dev@company.com
    JENKINS_DEPLOY_NOTIFICATION_EMAIL=new-dev@company.com,new-ba@company.com
    JENKINS_DEPLOY_SKIPPED_CHECK=true
    ```

2. **Run the PowerShell script:**

    ```powershell
    cd aws
    .\update-email-addresses.ps1 -DeployToAws
    ```

    This script will:
    - Read all email settings from `.env`
    - Update the ECS task definition JSON file
    - Register the new task definition with AWS
    - Update the ECS service to use the new configuration

3. **Jenkins automatically picks up the new settings:**
    - Jenkins reads `JENKINS_DEPLOY_SKIPPED_CHECK` directly from the running ECS task definition
    - No Jenkinsfile modification required
    - Changes take effect on the next Jenkins build

**What gets updated automatically:**
- ✅ `JENKINS_BUILD_NOTIFICATION_EMAIL`
- ✅ `JENKINS_DEPLOY_NOTIFICATION_EMAIL`  
- ✅ `JENKINS_DEPLOY_SKIPPED_CHECK`
- ✅ `MAIL_FROM`
- ✅ `ADMIN_EMAIL`
- ✅ `EXPENSE_APPROVERS`

**Important:** The script ensures all settings stay in sync between `.env`, the ECS task definition, and what Jenkins uses at runtime.

## Migration from Old Property

If you have existing configuration using `JENKINS_NOTIFICATION_EMAIL`:

1. **Update .env file:**

    ```env
    # OLD (remove this):
    # JENKINS_NOTIFICATION_EMAIL=dev-team@company.com,ba-team@company.com

    # NEW (add these):
    JENKINS_BUILD_NOTIFICATION_EMAIL=dev-team@company.com
    JENKINS_DEPLOY_NOTIFICATION_EMAIL=dev-team@company.com,ba-team@company.com
    ```

2. **Update AWS ECS Task Definition environment variables:**

    - Remove `JENKINS_NOTIFICATION_EMAIL`
    - Add `JENKINS_BUILD_NOTIFICATION_EMAIL`
    - Add `JENKINS_DEPLOY_NOTIFICATION_EMAIL`

3. **Update Jenkinsfile:**
    - Keep existing `/api/jenkins/build-success` and `/api/jenkins/build-failure` endpoints
    - Add new `/api/jenkins/deploy-success` and `/api/jenkins/deploy-failure` for deploy stage

## Benefits

1. **Targeted Communication**: Developers get all build notifications; business stakeholders only get deployment notifications
2. **Reduced Noise**: Business analysts don't get notified about every build failure during development
3. **Better Visibility**: Deployment notifications reach the right audience who needs to know about production changes
4. **Flexibility**: Different email lists for different environments (staging vs production)
5. **Temporary Monitoring**: Enable skipped deployment notifications when needed, disable when not required
6. **Centralized Management**: All notification settings managed through `.env` file - no Jenkinsfile changes needed

## Testing

Test the configuration:

1. **Build Notifications:**

    ```bash
    curl -X POST http://localhost:8081/api/jenkins/build-success \
         -H "Content-Type: application/json" \
         -H "Authorization: Bearer YOUR_TOKEN" \
         -d '{"buildNumber":"1","buildUrl":"http://jenkins/job/1/"}'
    ```

2. **Deploy Notifications:**

    ```bash
    curl -X POST http://localhost:8081/api/jenkins/deploy-success \
         -H "Content-Type: application/json" \
         -H "Authorization: Bearer YOUR_TOKEN" \
         -d '{"buildNumber":"1","deployUrl":"http://jenkins/deploy/1/","environment":"production"}'
    ```

3. **Deploy Skipped Notifications:**
    ```bash
    curl -X POST http://localhost:8081/api/jenkins/deploy-skipped \
         -H "Content-Type: application/json" \
         -H "Authorization: Bearer YOUR_TOKEN" \
         -d '{"buildNumber":"1","buildUrl":"http://jenkins/job/1/","environment":"production","reason":"No new builds since last deployment","triggeredBy":"scheduled"}'
    ```

## Documentation Updates

The following documentation files have been updated:

-   `jenkins/Jenkins_Build_Notifications.md` - Updated with new endpoint information
-   `aws/AWS_Deployment.md` - Updated environment variable sections
-   `src/main/resources/application.properties` - Updated property definitions
-   `.env` - Updated environment variables

## Related Files

### Java Code

-   `src/main/java/com/ammons/taskactivity/service/EmailService.java`
-   `src/main/java/com/ammons/taskactivity/controller/JenkinsBuildNotificationController.java`

### Configuration

-   `src/main/resources/application.properties`
-   `.env`

### Documentation

-   `jenkins/Jenkins_Build_Notifications.md`
-   `aws/AWS_Deployment.md`
-   `jenkins/Jenkins_Notification_Email_Separation.md` (this file)
