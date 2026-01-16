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

### New API Endpoints

Added two new REST endpoints for deployment notifications:

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

### Email Service Methods

**New Methods:**

-   `sendDeploySuccessNotification()` - Sends success notification to deploy email list
-   `sendDeployFailureNotification()` - Sends failure notification to deploy email list

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

## Configuration Examples

### Local Development (.env)

```env
JENKINS_BUILD_NOTIFICATION_EMAIL=developer@localhost.com
JENKINS_DEPLOY_NOTIFICATION_EMAIL=developer@localhost.com,manager@localhost.com
```

### Staging Environment

```env
JENKINS_BUILD_NOTIFICATION_EMAIL=dev-team@company.com
JENKINS_DEPLOY_NOTIFICATION_EMAIL=dev-team@company.com,qa-team@company.com,ba-team@company.com
```

### Production Environment

```env
JENKINS_BUILD_NOTIFICATION_EMAIL=dev-team@company.com
JENKINS_DEPLOY_NOTIFICATION_EMAIL=dev-team@company.com,ba-team@company.com,operations@company.com,management@company.com
```

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

## Testing

Test the configuration:

1. **Build Notifications:**

    ```bash
    curl -X POST http://localhost:8080/api/jenkins/build-success \
         -H "Content-Type: application/json" \
         -H "Authorization: Bearer YOUR_TOKEN" \
         -d '{"buildNumber":"1","buildUrl":"http://jenkins/job/1/"}'
    ```

2. **Deploy Notifications:**
    ```bash
    curl -X POST http://localhost:8080/api/jenkins/deploy-success \
         -H "Content-Type: application/json" \
         -H "Authorization: Bearer YOUR_TOKEN" \
         -d '{"buildNumber":"1","deployUrl":"http://jenkins/deploy/1/","environment":"production"}'
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
