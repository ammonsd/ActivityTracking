# Jenkins CI/CD Setup Guide

Complete guide for setting up Jenkins CI/CD pipeline for the Task Activity Tracking application.

## Table of Contents

-   [Prerequisites](#prerequisites)
-   [Jenkins Installation](#jenkins-installation)
-   [Plugin Installation](#plugin-installation)
-   [Tool Configuration](#tool-configuration)
-   [Credentials Setup](#credentials-setup)
-   [Pipeline Job Creation](#pipeline-job-creation)
-   [GitHub Webhook Setup](#github-webhook-setup)
-   [Environment Configuration](#environment-configuration)
-   [Testing the Pipeline](#testing-the-pipeline)
-   [Troubleshooting](#troubleshooting)

## Prerequisites

Before setting up Jenkins, ensure you have:

-   ✅ **Jenkins Server** - Running Jenkins 2.400+ (LTS recommended)
-   ✅ **Java 21** - Installed on Jenkins server
-   ✅ **Maven 3.9+** - For building the application
-   ✅ **Docker** - For building container images
-   ✅ **AWS Account** - With ECR and ECS configured
-   ✅ **AWS IAM User** - With appropriate permissions (see [Credentials Setup](#credentials-setup))
-   ✅ **GitHub Repository** - Access to your repository

## Jenkins Installation

### Option 1: Docker-based Jenkins (Recommended for Testing)

```powershell
# Create Jenkins home directory
mkdir C:\jenkins_home

# Run Jenkins in Docker with Docker socket mounted
docker run -d `
  --name jenkins `
  -p 8080:8080 -p 50000:50000 `
  -v C:\jenkins_home:/var/jenkins_home `
  -v /var/run/docker.sock:/var/run/docker.sock `
  jenkins/jenkins:lts-jdk21
```

Access Jenkins at: http://localhost:8080

Get initial admin password:

```powershell
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### Option 2: Windows Native Installation

1. Download Jenkins LTS from: https://www.jenkins.io/download/
2. Install Java 21 (if not already installed)
3. Run the Windows installer
4. Access Jenkins at: http://localhost:8080

### Option 3: AWS EC2 Installation

```bash
# Launch Amazon Linux 2023 EC2 instance (t3.medium recommended)

# Install Java 21
sudo yum install -y java-21-amazon-corretto-devel

# Install Jenkins
sudo wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
sudo yum install -y jenkins

# Install Docker
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker jenkins

# Start Jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins

# Get initial password
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Access Jenkins at: http://<ec2-public-ip>:8080

**Security Group Rules:**

-   Port 8080: Jenkins UI (restrict to your IP)
-   Port 50000: Jenkins agents (if using distributed builds)

## Plugin Installation

After initial Jenkins setup, install required plugins:

### Via Jenkins UI

1. Navigate to **Manage Jenkins** → **Manage Plugins**
2. Go to **Available** tab
3. Install these plugins:

**Essential Plugins:**

-   ✅ **Pipeline** - Core pipeline functionality
-   ✅ **Pipeline: Stage View** - Visualize pipeline stages
-   ✅ **Git** - Git repository integration
-   ✅ **GitHub** - GitHub integration
-   ✅ **Docker Pipeline** - Docker build/push support
-   ✅ **AWS Steps** - AWS service integration
-   ✅ **Amazon ECR** - ECR authentication
-   ✅ **Credentials Binding** - Secure credential management
-   ✅ **Maven Integration** - Maven build support
-   ✅ **JUnit** - Test result publishing
-   ✅ **Timestamper** - Add timestamps to console output
-   ✅ **Workspace Cleanup** - Clean workspace between builds

**Optional but Recommended:**

-   ✅ **Blue Ocean** - Modern UI for pipelines
-   ✅ **Email Extension** - Enhanced email notifications
-   ✅ **Slack Notification** - Slack integration
-   ✅ **SonarQube Scanner** - Code quality analysis
-   ✅ **OWASP Dependency-Check** - Security scanning
-   ✅ **Config File Provider** - Manage configuration files

4. Click **Install without restart**
5. Restart Jenkins when safe

### Via Jenkins CLI (Alternative)

```bash
# Download Jenkins CLI
wget http://localhost:8080/jnlpJars/jenkins-cli.jar

# Install plugins
java -jar jenkins-cli.jar -s http://localhost:8080/ install-plugin \
  workflow-aggregator \
  git \
  github \
  docker-workflow \
  pipeline-aws \
  amazon-ecr \
  credentials-binding \
  maven-plugin \
  junit \
  timestamper \
  ws-cleanup

# Restart Jenkins
java -jar jenkins-cli.jar -s http://localhost:8080/ safe-restart
```

## Tool Configuration

Configure Java, Maven, and other tools used by the pipeline.

### 1. Configure JDK 21

1. Navigate to **Manage Jenkins** → **Global Tool Configuration**
2. Scroll to **JDK** section
3. Click **Add JDK**
    - **Name:** `JDK-21`
    - **JAVA_HOME:** (Path to Java 21 installation)
        - Windows: `C:\Program Files\Java\jdk-21`
        - Linux: `/usr/lib/jvm/java-21-openjdk`
    - Uncheck "Install automatically" if using existing installation
4. Click **Save**

### 2. Configure Maven

1. In **Global Tool Configuration**, scroll to **Maven** section
2. Click **Add Maven**
    - **Name:** `Maven-3.9`
    - **Version:** Select Maven 3.9.x
    - Check "Install automatically"
3. Click **Save**

### 3. Configure Docker (if needed)

For Docker-based Jenkins, Docker is already available. For native installations:

**Windows:**

-   Ensure Docker Desktop is installed and running
-   Jenkins can access Docker socket

**Linux:**

-   Add Jenkins user to docker group:
    ```bash
    sudo usermod -aG docker jenkins
    sudo systemctl restart jenkins
    ```

### 4. Configure Git (Usually pre-configured)

1. In **Global Tool Configuration**, scroll to **Git** section
2. Verify Git is configured
    - **Name:** `Default`
    - **Path:** `git` (or full path like `C:\Program Files\Git\bin\git.exe`)

## Credentials Setup

Configure credentials for AWS, GitHub, and other services.

### 1. AWS Credentials (Required)

**Option A: Access Key/Secret (Simple but less secure)**

1. Navigate to **Manage Jenkins** → **Manage Credentials**
2. Click **(global)** domain
3. Click **Add Credentials**
    - **Kind:** `AWS Credentials`
    - **ID:** `aws-credentials`
    - **Description:** `AWS credentials for ECR and ECS`
    - **Access Key ID:** Your IAM user access key
    - **Secret Access Key:** Your IAM user secret key
4. Click **OK**

**Option B: IAM Role (Recommended for EC2-based Jenkins)**

If Jenkins runs on EC2, attach an IAM role with these policies:

-   `AmazonEC2ContainerRegistryPowerUser`
-   `AmazonECS_FullAccess`
-   Custom policy for task definition management

No credential configuration needed in Jenkins - role is automatically assumed.

**Required IAM Permissions:**

Create IAM user/role with this policy:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:PutImage",
                "ecr:InitiateLayerUpload",
                "ecr:UploadLayerPart",
                "ecr:CompleteLayerUpload"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "ecs:DescribeServices",
                "ecs:DescribeTaskDefinition",
                "ecs:DescribeTasks",
                "ecs:ListTaskDefinitions",
                "ecs:RegisterTaskDefinition",
                "ecs:UpdateService"
            ],
            "Resource": "*"
        }
    ]
}
```

### 2. GitHub Credentials (For Private Repositories)

1. Navigate to **Manage Jenkins** → **Manage Credentials**
2. Click **(global)** domain
3. Click **Add Credentials**
    - **Kind:** `Username with password`
    - **ID:** `github-credentials`
    - **Username:** Your GitHub username
    - **Password:** GitHub Personal Access Token (not your password!)
    - **Description:** `GitHub access token`
4. Click **OK**

**Create GitHub Personal Access Token:**

1. Go to GitHub → Settings → Developer settings → Personal access tokens
2. Click "Generate new token (classic)"
3. Select scopes: `repo`, `admin:repo_hook`
4. Copy the token (you won't see it again!)

### 3. Secrets Manager Credentials (Optional)

If using AWS Secrets Manager for additional secrets:

1. Add credentials of type `Secret text`
2. Use in pipeline with: `credentials('secret-id')`

## Pipeline Job Creation

Create the Jenkins pipeline job to run your CI/CD workflow.

### Method 1: Multibranch Pipeline (Recommended)

1. From Jenkins dashboard, click **New Item**
2. Enter name: `TaskActivity-Pipeline`
3. Select **Multibranch Pipeline**
4. Click **OK**

**Configure Branch Sources:**

1. Click **Add source** → **Git** (or **GitHub** if using GitHub plugin)
2. **Project Repository:** `https://github.com/ammonsd/ActivityTracking.git`
3. **Credentials:** Select `github-credentials` (if private repo)
4. **Behaviors:** Add `Discover branches` → `All branches`

**Build Configuration:**

1. **Script Path:** `Jenkinsfile` (default)
2. **Scan Multibranch Pipeline Triggers:**

    - Check "Periodically if not otherwise run"
    - Interval: `1 day`

3. Click **Save**

The pipeline will automatically scan for branches and discover the Jenkinsfile.

### Method 2: Standard Pipeline (Single Branch)

1. From Jenkins dashboard, click **New Item**
2. Enter name: `TaskActivity-Pipeline`
3. Select **Pipeline**
4. Click **OK**

**Configure Pipeline:**

1. **Description:** `Task Activity CI/CD Pipeline`
2. Check **GitHub project** (optional)

    - **Project url:** `https://github.com/ammonsd/ActivityTracking/`

3. **Build Triggers:**

    - Check "GitHub hook trigger for GITScm polling"
    - Or check "Poll SCM" with schedule: `H/15 * * * *` (every 15 min)

4. **Pipeline:**

    - **Definition:** `Pipeline script from SCM`
    - **SCM:** `Git`
    - **Repository URL:** `https://github.com/ammonsd/ActivityTracking.git`
    - **Credentials:** Select `github-credentials` (if private)
    - **Branch Specifier:** `*/main` (or `*/master`)
    - **Script Path:** `Jenkinsfile`

5. Click **Save**

## GitHub Webhook Setup

Configure GitHub to trigger Jenkins builds automatically on code commits.

### Prerequisites

-   Jenkins must be accessible from the internet (use ngrok for testing locally)
-   GitHub plugin installed in Jenkins

### Setup Steps

1. **Get Jenkins Webhook URL:**

    - URL format: `http://<jenkins-url>/github-webhook/`
    - Example: `http://jenkins.example.com:8080/github-webhook/`

2. **Configure GitHub Webhook:**

    - Go to your GitHub repository
    - Click **Settings** → **Webhooks** → **Add webhook**
    - **Payload URL:** `http://<jenkins-url>/github-webhook/`
    - **Content type:** `application/json`
    - **Which events:** Select "Just the push event"
    - **Active:** Check this box
    - Click **Add webhook**

3. **Test Webhook:**
    - Make a commit to your repository
    - Check GitHub webhook "Recent Deliveries" for success (green checkmark)
    - Verify Jenkins job was triggered

### Using ngrok for Local Testing

If Jenkins is on localhost:

```powershell
# Download and install ngrok from https://ngrok.com/

# Start ngrok tunnel
ngrok http 8080

# Use the HTTPS forwarding URL in GitHub webhook
# Example: https://abc123.ngrok.io/github-webhook/
```

## Environment Configuration

Configure environment-specific settings for dev, staging, and production.

### 1. Update Jenkinsfile Variables

Edit the `Jenkinsfile` and update these values:

```groovy
environment {
    AWS_REGION = 'us-east-1'           // Your AWS region
    AWS_ACCOUNT_ID = '378010131175'    // Your AWS account ID
    ECR_REPOSITORY = 'taskactivity'    // Your ECR repository name

    // Environment-specific (uses parameter)
    ECS_CLUSTER = "taskactivity-cluster-${params.ENVIRONMENT}"
    ECS_SERVICE = "taskactivity-service-${params.ENVIRONMENT}"
}
```

### 2. Create AWS Resources Per Environment

For each environment (dev, staging, production):

**ECS Clusters:**

```powershell
aws ecs create-cluster --cluster-name taskactivity-cluster-dev
aws ecs create-cluster --cluster-name taskactivity-cluster-staging
aws ecs create-cluster --cluster-name taskactivity-cluster-production
```

**ECR Repository** (shared across environments):

```powershell
aws ecr create-repository --repository-name taskactivity --region us-east-1
```

**Task Definitions:**

-   Create separate task definitions for each environment
-   Name format: `taskactivity-task-dev`, `taskactivity-task-staging`, `taskactivity-task-production`

**ECS Services:**

```powershell
# Example for dev environment
aws ecs create-service \
  --cluster taskactivity-cluster-dev \
  --service-name taskactivity-service-dev \
  --task-definition taskactivity-task-dev:1 \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
```

### 3. Environment-Specific Parameters

When running the pipeline, select the target environment:

-   **ENVIRONMENT:** `dev`, `staging`, or `production`
-   **DEPLOY_ACTION:** `deploy`, `build-only`, or `rollback`
-   **SKIP_TESTS:** `false` (always run tests for production)
-   **NO_CACHE:** `false` (rebuild from scratch if needed)

## Testing the Pipeline

### 1. Manual Build

1. Navigate to your pipeline job
2. Click **Build with Parameters**
3. Select:
    - **ENVIRONMENT:** `dev`
    - **DEPLOY_ACTION:** `deploy`
    - **SKIP_TESTS:** `false`
    - **NO_CACHE:** `false`
4. Click **Build**

### 2. Monitor Build Progress

1. Click on the build number (e.g., `#1`)
2. View **Console Output** for detailed logs
3. Check **Pipeline Steps** for stage-by-stage progress

### 3. Verify Deployment

```powershell
# Check ECS service
aws ecs describe-services \
  --cluster taskactivity-cluster-dev \
  --services taskactivity-service-dev

# Check running tasks
aws ecs list-tasks \
  --cluster taskactivity-cluster-dev \
  --service-name taskactivity-service-dev

# View application logs
aws logs tail /ecs/taskactivity-dev --follow
```

### 4. Test Rollback

1. Click **Build with Parameters**
2. Select:
    - **ENVIRONMENT:** `dev`
    - **DEPLOY_ACTION:** `rollback`
3. Click **Build**

## Troubleshooting

### Common Issues

#### 1. AWS Credentials Error

**Error:** `Unable to locate credentials`

**Solution:**

-   Verify AWS credentials are configured in Jenkins
-   Check credential ID matches `aws-credentials` in Jenkinsfile
-   For EC2, verify IAM role is attached

#### 2. Docker Permission Denied

**Error:** `Got permission denied while trying to connect to the Docker daemon`

**Solution:**

```bash
# Add Jenkins user to docker group
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

#### 3. Maven Build Failure

**Error:** `mvnw: command not found`

**Solution:**

-   Use `./mvnw` (Linux) or `mvnw.cmd` (Windows)
-   Ensure mvnw wrapper is committed to repository
-   Check file permissions: `chmod +x mvnw`

#### 4. ECR Login Failure

**Error:** `no basic auth credentials`

**Solution:**

-   Verify AWS credentials have ECR permissions
-   Check AWS region matches ECR repository region
-   Update ECR registry URL in Jenkinsfile

#### 5. ECS Deployment Timeout

**Error:** `Waiter ServicesStable failed: Max attempts exceeded`

**Solution:**

-   Check ECS task logs for application errors
-   Verify health check configuration
-   Increase timeout in Jenkinsfile
-   Check security groups allow traffic

#### 6. GitHub Webhook Not Triggering

**Issue:** Jenkins doesn't build on push

**Solution:**

-   Verify webhook is active in GitHub
-   Check webhook delivery history for errors
-   Ensure Jenkins is publicly accessible
-   Verify "GitHub hook trigger" is enabled in job

### Debug Mode

Enable verbose logging in Jenkinsfile:

```groovy
options {
    // Add this for more detailed output
    timestamps()
    ansiColor('xterm')
}

steps {
    script {
        sh 'set -x'  // Enable bash debug mode
        // Your commands here
    }
}
```

### Useful Jenkins CLI Commands

```powershell
# Restart Jenkins
java -jar jenkins-cli.jar -s http://localhost:8080/ safe-restart

# List plugins
java -jar jenkins-cli.jar -s http://localhost:8080/ list-plugins

# Get job status
java -jar jenkins-cli.jar -s http://localhost:8080/ get-job TaskActivity-Pipeline

# Build job
java -jar jenkins-cli.jar -s http://localhost:8080/ build TaskActivity-Pipeline
```

## Next Steps

After successful setup:

1. ✅ Configure email/Slack notifications
2. ✅ Set up SonarQube for code quality
3. ✅ Add security scanning (Trivy, Snyk)
4. ✅ Configure staging approval gates
5. ✅ Set up automated testing
6. ✅ Create deployment dashboards
7. ✅ Document runbook procedures

## Additional Resources

-   [Jenkins Documentation](https://www.jenkins.io/doc/)
-   [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
-   [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
-   [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)

## Support

For issues specific to this project:

-   Check console output first
-   Review this guide's troubleshooting section
-   Verify AWS resources are properly configured
-   Ensure all prerequisites are met

---

**Last Updated:** October 2025  
**Maintained By:** Development Team
