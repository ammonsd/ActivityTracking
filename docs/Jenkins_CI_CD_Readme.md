# Jenkins CI/CD Implementation for Task Activity Tracking

This directory contains all files and documentation for implementing Jenkins-based CI/CD for the Task Activity Tracking application.

## 📚 Documentation

### Getting Started

-   **[JENKINS_QUICK_REFERENCE.md](JENKINS_QUICK_REFERENCE.md)** - Quick reference for common operations
-   **[JENKINS_SETUP.md](JENKINS_SETUP.md)** - Complete setup guide with step-by-step instructions
-   **[JENKINS_ENVIRONMENTS.md](JENKINS_ENVIRONMENTS.md)** - Environment-specific configuration

### Implementation Files

-   **[Jenkinsfile](../Jenkinsfile)** - Main Jenkins pipeline definition
-   **[jenkins/](../jenkins/)** - Helper scripts for deployment management

## 🚀 Quick Start

### 1. Install Jenkins

**Docker (Recommended for Testing):**

```powershell
docker run -d --name jenkins -p 8080:8080 -p 50000:50000 `
  -v C:\jenkins_home:/var/jenkins_home `
  jenkins/jenkins:lts-jdk21
```

**AWS EC2 (Production):**
See [JENKINS_SETUP.md](JENKINS_SETUP.md#jenkins-installation) for detailed instructions.

### 2. Configure Jenkins

1. Install required plugins (Pipeline, Docker, AWS Steps, Git, Maven)
2. Configure JDK 21 and Maven 3.9
3. Set up AWS credentials
4. Add GitHub credentials (if private repo)

Full guide: [JENKINS_SETUP.md](JENKINS_SETUP.md#plugin-installation)

### 3. Create Pipeline Job

1. New Item → **Multibranch Pipeline**
2. Add Git source: `https://github.com/ammonsd/ActivityTracking.git`
3. Script path: `Jenkinsfile`
4. Save and scan

### 4. Deploy

Click **Build with Parameters** and select:

-   **ENVIRONMENT:** `dev`
-   **DEPLOY_ACTION:** `deploy`
-   Click **Build**

## 📋 What's Included

### Core Pipeline (Jenkinsfile)

```
✅ Multi-stage build pipeline
✅ Maven build and testing
✅ Docker image creation
✅ Amazon ECR push
✅ ECS Fargate deployment
✅ Health check validation
✅ Automated rollback support
✅ Environment-specific deployments
```

### Documentation

```
✅ Complete setup guide
✅ Environment configuration guide
✅ Quick reference guide
✅ Troubleshooting procedures
✅ Security best practices
```

### Helper Scripts

```
✅ check-deployment.sh       - Check ECS deployment status
✅ trigger-build.sh           - Trigger builds via CLI
✅ verify-environment.sh      - Verify AWS resources
✅ cleanup-old-images.sh      - Clean up ECR images
```

## 🏗️ Architecture

### Pipeline Flow

```
Git Push → GitHub Webhook → Jenkins → Maven Build → Docker Build → ECR Push → ECS Deploy
```

### Environments

-   **Dev:** Automatic deployment on git push
-   **Staging:** Manual or scheduled (nightly)
-   **Production:** Manual with approval

### AWS Resources Per Environment

-   ECS Cluster: `taskactivity-cluster-{env}`
-   ECS Service: `taskactivity-service-{env}`
-   Task Definition: `taskactivity-task-{env}`
-   CloudWatch Logs: `/ecs/taskactivity-{env}`
-   Secrets: `taskactivity/{env}/*`

## 🔧 Configuration

### Required AWS Resources

**Create for each environment (dev, staging, production):**

```powershell
# ECS Cluster
aws ecs create-cluster --cluster-name taskactivity-cluster-dev

# CloudWatch Log Group
aws logs create-log-group --log-group-name /ecs/taskactivity-dev

# Secrets Manager
aws secretsmanager create-secret \
  --name taskactivity/dev/database/credentials \
  --secret-string '{"username":"admin","password":"xxx","jdbcUrl":"jdbc:postgresql://..."}'

aws secretsmanager create-secret \
  --name taskactivity/dev/admin/credentials \
  --secret-string '{"password":"xxx"}'
```

**ECR Repository (shared across environments):**

```powershell
aws ecr create-repository --repository-name taskactivity --region us-east-1
```

See [JENKINS_ENVIRONMENTS.md](JENKINS_ENVIRONMENTS.md#environment-setup) for complete setup.

### Jenkins Configuration

**Update Jenkinsfile variables:**

```groovy
environment {
    AWS_REGION = 'us-east-1'           // Your AWS region
    AWS_ACCOUNT_ID = '378010131175'    // Your AWS account ID
}
```

**Add Jenkins credentials:**

-   ID: `aws-credentials` (AWS access key/secret)
-   ID: `github-credentials` (GitHub token for private repos)

## 📊 Monitoring

### Check Deployment Status

```bash
cd jenkins
./check-deployment.sh production
```

### View Logs

```bash
aws logs tail /ecs/taskactivity-production --follow
```

### Check Tasks

```bash
aws ecs describe-services \
  --cluster taskactivity-cluster-production \
  --services taskactivity-service-production
```

## 🔄 Common Operations

### Deploy to Dev

```bash
# Via Jenkins UI
Build with Parameters → ENVIRONMENT=dev → Deploy

# Via CLI
./jenkins/trigger-build.sh dev deploy
```

### Deploy to Production

```bash
# Requires manual approval
Build with Parameters → ENVIRONMENT=production → Deploy
```

### Rollback

```bash
# Via Jenkins
Build with Parameters → DEPLOY_ACTION=rollback

# Via script
./jenkins/trigger-build.sh production rollback
```

### Clean Up Old Images

```bash
./jenkins/cleanup-old-images.sh --keep 10
```

## 🛡️ Security

-   ✅ AWS credentials stored in Jenkins credential store
-   ✅ Secrets managed via AWS Secrets Manager
-   ✅ GitHub webhook signature validation
-   ✅ Production deployments require approval
-   ✅ IAM roles with least-privilege access
-   ✅ Regular security scanning (optional)

## 💰 Cost Optimization

### ECR Lifecycle Policy

```bash
# Automatically remove old images
aws ecr put-lifecycle-policy \
  --repository-name taskactivity \
  --lifecycle-policy-text file://ecr-lifecycle-policy.json
```

### Environment Sizing

-   **Dev:** 1 task (256 CPU, 512 MB) - ~$3/month
-   **Staging:** 1 task (512 CPU, 1024 MB) - ~$6/month
-   **Production:** 2 tasks (1024 CPU, 2048 MB) - ~$25/month

## 🆚 Comparison: Manual vs Jenkins

| Aspect        | Manual Deployment   | Jenkins CI/CD         |
| ------------- | ------------------- | --------------------- |
| Trigger       | Manual script       | Automatic on push     |
| Testing       | Manual              | Automated in pipeline |
| Consistency   | Developer-dependent | Standardized          |
| Rollback      | Manual script       | Automated             |
| Audit Trail   | Git only            | Full build history    |
| Notifications | None                | Email/Slack alerts    |
| Multi-env     | Manual switching    | Parameter-driven      |

## 📖 Additional Resources

-   [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
-   [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
-   [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
-   [Maven on Jenkins](https://www.jenkins.io/doc/book/using/using-maven/)

## 🆘 Troubleshooting

See [JENKINS_QUICK_REFERENCE.md](JENKINS_QUICK_REFERENCE.md#troubleshooting) for common issues and solutions.

**Quick Diagnostics:**

```bash
# Verify environment
./jenkins/verify-environment.sh production

# Check deployment
./jenkins/check-deployment.sh production

# View recent logs
aws logs tail /ecs/taskactivity-production --since 30m
```

## 📝 Next Steps

After implementing Jenkins CI/CD:

1. ✅ Set up GitHub webhooks for automatic builds
2. ✅ Configure email/Slack notifications
3. ✅ Add SonarQube for code quality
4. ✅ Integrate security scanning (Trivy, Snyk)
5. ✅ Set up CloudWatch alarms
6. ✅ Create runbook for incidents
7. ✅ Schedule regular DR tests

---

**Created:** October 2025  
**Maintained By:** DevOps Team  
**Version:** 1.0
