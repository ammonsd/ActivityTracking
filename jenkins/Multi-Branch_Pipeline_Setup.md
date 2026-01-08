# Multi-Branch Pipeline Setup for Jenkins

## Overview

This guide explains how to configure Jenkins with a multi-branch pipeline that replicates the Bamboo workflow:

1. **Auto-build on merge** - Triggered when pull requests are merged to develop branch
2. **Batch multiple merges** - Wait 5 minutes to batch multiple merges into one build
3. **Scheduled deployment** - Deploy at 7pm daily, but only if new builds exist since last deployment

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     GitHub Repository                        │
│  ┌──────────┐    ┌──────────┐    ┌──────────────┐          │
│  │  feature │───▶│  develop │───▶│  main        │          │
│  │  branches│    │  branch  │    │  (production)│          │
│  └──────────┘    └──────────┘    └──────────────┘          │
└────────────┬────────────────────────────┬───────────────────┘
             │                            │
             │ PR Merge                   │ Manual Release
             │                            │
┌────────────▼────────────────────────────▼───────────────────┐
│              Jenkins Multi-Branch Pipeline                   │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Develop Branch (Auto Build)                           │  │
│  │ - Webhook trigger on merge                            │  │
│  │ - Quiet period: 5 minutes (batches multiple merges)  │  │
│  │ - Build → Test → Docker → Push to ECR                │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Scheduled Deployment Job (7pm Daily)                  │  │
│  │ - Cron: 0 19 * * * (7pm)                             │  │
│  │ - Check if new build exists since last deployment     │  │
│  │ - If yes: Deploy to Dev → Verify                     │  │
│  │ - If no: Skip deployment                              │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Main Branch (Manual Deploy)                           │  │
│  │ - Manual trigger only                                  │  │
│  │ - Build → Test → Deploy to Staging/Production        │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

## Step 1: Create Multi-Branch Pipeline in Jenkins

### 1.1 Install Required Plugins

Ensure these plugins are installed in Jenkins:

-   **Multi-Branch Pipeline** (usually pre-installed)
-   **GitHub Branch Source** (for GitHub integration)
-   **AWS Steps** (for ECR/ECS deployment)
-   **Docker Pipeline**
-   **Pipeline: Stage View**

**Installation:**

1. Go to: Manage Jenkins → Manage Plugins → Available
2. Search for and install each plugin
3. Restart Jenkins

### 1.2 Create Multi-Branch Pipeline Job

1. **New Item**

    - Name: `taskactivity-multibranch`
    - Type: **Multi-Branch Pipeline**
    - Click OK

2. **Branch Sources**

    - Add source: **GitHub**
    - Credentials: Select or add GitHub credentials
    - Repository HTTPS URL: `https://github.com/ammonsd/ActivityTracking`
    - Behaviors:
        - ✓ Discover branches
        - ✓ Discover pull requests from origin
        - Filter by name: `develop main` (or use regex)

3. **Build Configuration**

    - Mode: **by Jenkinsfile**
    - Script Path: `Jenkinsfile`

4. **Scan Multi-Branch Pipeline Triggers**

    - ✓ Periodically if not otherwise run
    - Interval: **5 minutes** (checks for new branches/changes)

5. **Orphaned Item Strategy**

    - Days to keep old items: **30**
    - Max # of old items: **20**

6. **Save**

Jenkins will automatically scan the repository and create sub-jobs for each branch (develop, main).

## Step 2: Configure Branch-Specific Behavior

### 2.1 Update Jenkinsfile with Branch Logic

Create a `Jenkinsfile` that handles different branches:

```groovy
pipeline {
    agent any

    // Branch-specific triggers
    triggers {
        // For develop branch: auto-build with quiet period
        script {
            if (env.BRANCH_NAME == 'develop') {
                return [
                    pollSCM('H/5 * * * *')  // Check every 5 minutes
                ]
            }
        }
    }

    options {
        // Quiet period: wait 5 minutes before building
        // If new commits arrive, timer resets
        quietPeriod(300)  // 300 seconds = 5 minutes

        buildDiscarder(logRotator(numToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'production'],
            description: 'Target deployment environment'
        )
        choice(
            name: 'DEPLOY_ACTION',
            choices: ['build-only', 'deploy', 'rollback'],
            description: 'Deployment action to perform'
        )
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    echo "Branch: ${env.BRANCH_NAME}"
                    echo "Build Number: ${env.BUILD_NUMBER}"

                    // Set environment based on branch
                    if (env.BRANCH_NAME == 'develop') {
                        env.TARGET_ENV = 'dev'
                        env.AUTO_DEPLOY = 'false'  // Build only, deploy later
                    } else if (env.BRANCH_NAME == 'main') {
                        env.TARGET_ENV = params.ENVIRONMENT
                        env.AUTO_DEPLOY = 'true'  // Manual deploy
                    }
                }
            }
        }

        stage('Build & Test') {
            steps {
                echo 'Building application with Maven...'
                sh './mvnw clean package -B'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    junit testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}"
                    env.IMAGE_FULL = "${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}"

                    docker.build("${env.IMAGE_FULL}", ".")
                    sh "docker tag ${env.IMAGE_FULL} ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:latest-${env.TARGET_ENV}"
                }
            }
        }

        stage('Push to ECR') {
            when {
                // Always push for develop and main branches
                anyOf {
                    branch 'develop'
                    branch 'main'
                }
            }
            steps {
                script {
                    withAWS(credentials: 'aws-credentials', region: "${env.AWS_REGION}") {
                        sh """
                            aws ecr get-login-password --region ${env.AWS_REGION} | \
                            docker login --username AWS --password-stdin ${env.ECR_REGISTRY}
                        """
                        sh "docker push ${env.IMAGE_FULL}"
                        sh "docker push ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:latest-${env.TARGET_ENV}"
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                allOf {
                    branch 'main'
                    expression { params.DEPLOY_ACTION == 'deploy' }
                }
            }
            steps {
                echo "Deploying to ${env.TARGET_ENV}..."
                // Deployment steps here
            }
        }
    }

    post {
        success {
            script {
                if (env.BRANCH_NAME == 'develop') {
                    echo "✓ Build successful - ready for scheduled deployment"
                }
            }
        }
    }
}
```

## Step 3: Create Scheduled Deployment Job

Create a separate job for the 7pm scheduled deployment.

### 3.1 Create New Pipeline Job

1. **New Item**

    - Name: `taskactivity-scheduled-deploy`
    - Type: **Pipeline**
    - Click OK

2. **Build Triggers**

    - ✓ Build periodically
    - Schedule: `0 19 * * *` (7pm daily)

3. **Pipeline Script:**

```groovy
pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        ECS_CLUSTER = "taskactivity-cluster"
        ECS_SERVICE = "taskactivity-service-dev"
        DEPLOY_ENV = "dev"
    }

    stages {
        stage('Check for New Build') {
            steps {
                script {
                    echo "Checking for new builds since last deployment..."

                    // Get timestamp of last successful deployment
                    def lastDeployTime = 0
                    def lastDeploy = currentBuild.getPreviousSuccessfulBuild()
                    if (lastDeploy) {
                        lastDeployTime = lastDeploy.startTimeInMillis
                        echo "Last deployment: ${new Date(lastDeployTime)}"
                    } else {
                        echo "No previous deployment found - will deploy"
                    }

                    // Get timestamp of latest develop build
                    def buildJob = Jenkins.instance.getItemByFullName('taskactivity-multibranch/develop')
                    if (!buildJob) {
                        error("Cannot find develop branch job")
                    }

                    def latestBuild = buildJob.lastSuccessfulBuild
                    if (!latestBuild) {
                        echo "No successful builds found. Skipping deployment."
                        currentBuild.result = 'NOT_BUILT'
                        return
                    }

                    def latestBuildTime = latestBuild.startTimeInMillis
                    echo "Latest build: ${new Date(latestBuildTime)} (Build #${latestBuild.number})"

                    // Compare timestamps
                    if (latestBuildTime <= lastDeployTime) {
                        echo "No new builds since last deployment. Skipping."
                        currentBuild.result = 'NOT_BUILT'
                        env.SKIP_DEPLOY = 'true'
                    } else {
                        echo "New build detected! Proceeding with deployment."
                        env.SKIP_DEPLOY = 'false'
                        env.BUILD_TO_DEPLOY = latestBuild.number
                    }
                }
            }
        }

        stage('Deploy to Dev') {
            when {
                expression { env.SKIP_DEPLOY == 'false' }
            }
            steps {
                script {
                    echo "Deploying build #${env.BUILD_TO_DEPLOY} to Dev environment..."

                    withAWS(credentials: 'aws-credentials', region: "${env.AWS_REGION}") {
                        // Get current task definition
                        sh """
                            aws ecs describe-task-definition \
                                --task-definition taskactivity \
                                --query 'taskDefinition' > task-def.json
                        """

                        // Update image tag
                        sh """
                            jq --arg IMAGE "${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:latest-dev" \
                            '.containerDefinitions[0].image = \$IMAGE |
                             del(.taskDefinitionArn, .revision, .status, .requiresAttributes,
                                 .compatibilities, .registeredAt, .registeredBy)' \
                            task-def.json > updated-task-def.json
                        """

                        // Register new task definition
                        def newTaskDefArn = sh(
                            script: """
                                aws ecs register-task-definition \
                                    --cli-input-json file://updated-task-def.json \
                                    --query 'taskDefinition.taskDefinitionArn' \
                                    --output text
                            """,
                            returnStdout: true
                        ).trim()

                        echo "New task definition: ${newTaskDefArn}"

                        // Update ECS service
                        sh """
                            aws ecs update-service \
                                --cluster ${env.ECS_CLUSTER} \
                                --service ${env.ECS_SERVICE} \
                                --task-definition ${newTaskDefArn} \
                                --force-new-deployment
                        """

                        // Wait for deployment
                        echo "Waiting for deployment to stabilize..."
                        sh """
                            aws ecs wait services-stable \
                                --cluster ${env.ECS_CLUSTER} \
                                --services ${env.ECS_SERVICE}
                        """
                    }
                }
            }
        }

        stage('Verify Deployment') {
            when {
                expression { env.SKIP_DEPLOY == 'false' }
            }
            steps {
                script {
                    withAWS(credentials: 'aws-credentials', region: "${env.AWS_REGION}") {
                        def serviceInfo = sh(
                            script: """
                                aws ecs describe-services \
                                    --cluster ${env.ECS_CLUSTER} \
                                    --services ${env.ECS_SERVICE} \
                                    --query 'services[0]' \
                                    --output json
                            """,
                            returnStdout: true
                        ).trim()

                        def runningCount = sh(
                            script: "echo '${serviceInfo}' | jq -r '.runningCount'",
                            returnStdout: true
                        ).trim().toInteger()

                        def desiredCount = sh(
                            script: "echo '${serviceInfo}' | jq -r '.desiredCount'",
                            returnStdout: true
                        ).trim().toInteger()

                        if (runningCount != desiredCount) {
                            error("Deployment verification failed: running=${runningCount}, desired=${desiredCount}")
                        }

                        echo "✓ Deployment verified: ${runningCount}/${desiredCount} tasks running"
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                if (env.SKIP_DEPLOY == 'false') {
                    echo "✓ Deployment completed successfully at ${new Date()}"
                } else {
                    echo "ℹ No deployment needed - no new builds"
                }
            }
        }
        failure {
            echo "✗ Deployment failed - check logs for details"
        }
    }
}
```

## Step 4: Configure GitHub Webhook (Optional but Recommended)

Instead of polling, use webhooks for instant triggers.

### 4.1 In GitHub Repository

1. Go to: **Settings** → **Webhooks** → **Add webhook**
2. Payload URL: `http://[jenkins-url]:8081/github-webhook/`
3. Content type: `application/json`
4. Events: **Just the push event**
5. Active: ✓
6. Add webhook

### 4.2 Update Jenkinsfile

Replace `pollSCM` with GitHub webhook trigger:

```groovy
triggers {
    script {
        if (env.BRANCH_NAME == 'develop') {
            return [
                githubPush()  // Webhook trigger instead of polling
            ]
        }
    }
}
```

## Step 5: Testing the Setup

### 5.1 Test Auto-Build with Quiet Period

1. Merge a PR to develop branch
2. Check Jenkins - build should start after 5 minutes
3. Merge another PR within 5 minutes
4. Verify only ONE build runs (batches both merges)

### 5.2 Test Scheduled Deployment

1. Ensure there's a successful build on develop branch
2. Wait until 7pm (or manually trigger scheduled job)
3. Verify deployment runs
4. Run again without new builds - should skip deployment

### 5.3 Test Manual Production Deploy

1. Merge develop → main
2. Manually trigger main branch build
3. Select "production" environment
4. Verify deployment to production

## Workflow Summary

```
Developer Workflow:
1. Create feature branch → Code → PR to develop
2. PR merged → Jenkins waits 5 min (batches merges) → Auto-build
3. Build succeeds → Docker image pushed to ECR with tag "latest-dev"
4. At 7pm → Scheduled job checks for new builds → Deploys if found
5. After testing → Merge develop → main → Manual deploy to production

Key Behaviors:
- Multiple merges within 5 min = ONE build (quiet period)
- 7pm deployment ONLY if new builds exist since last 7pm
- Production deploys are always manual
- Each branch has independent trigger logic
```

## Configuration Files Location

```
ActivityTracking/
├── Jenkinsfile                          # Multi-branch pipeline config
├── jenkins/
│   ├── Multi-Branch_Pipeline_Setup.md  # This guide
│   ├── scheduled-deploy.groovy         # Scheduled deployment script
│   └── Jenkins_Setup.md                 # General Jenkins setup
└── ...
```

## Troubleshooting

### Build Not Triggering on Merge

**Check:**

1. Webhook configured correctly (GitHub → Settings → Webhooks)
2. Jenkins can receive GitHub webhooks (firewall/network)
3. Branch filter includes "develop" branch
4. Repository credentials are valid

### Quiet Period Not Working

**Symptoms:** Each merge triggers separate build

**Solution:**

-   Ensure `quietPeriod(300)` is in the `options` block
-   Value is in seconds (300 = 5 minutes)
-   Quiet period resets each time a new commit arrives

### Scheduled Deploy Always Skips

**Check:**

1. Verify timestamp comparison logic
2. Ensure develop branch builds are successful
3. Check job name matches: `taskactivity-multibranch/develop`
4. Review console output for timestamp values

### Multiple Jobs Running for Same Branch

**Cause:** Both webhook and polling enabled

**Solution:** Use either webhook OR polling, not both:

```groovy
// Option 1: Webhook only (recommended)
triggers {
    githubPush()
}

// Option 2: Polling only (fallback)
triggers {
    pollSCM('H/5 * * * *')
}
```

## Additional Resources

-   [Jenkins Multi-Branch Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/multibranch/)
-   [Jenkins Webhook Configuration](https://docs.github.com/en/webhooks/using-webhooks)
-   [Pipeline Syntax Reference](https://www.jenkins.io/doc/book/pipeline/syntax/)
-   [AWS ECS Deployment Guide](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-types.html)

## Next Steps

1. Test the multi-branch setup with a test repository first
2. Configure email/Slack notifications for build/deploy status
3. Add deployment approval gates for production
4. Set up blue/green deployments for zero-downtime updates
5. Integrate code quality tools (SonarQube, etc.)
