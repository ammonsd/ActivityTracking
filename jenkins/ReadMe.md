# Jenkins Helper Scripts

Utility scripts for managing Jenkins deployments, troubleshooting builds, and monitoring the CI/CD pipeline.

## Available Scripts

### check-deployment.sh

Check the status of an ECS deployment.

**Usage:**

```bash
./check-deployment.sh <environment>
```

**Example:**

```bash
./check-deployment.sh production
```

**What it checks:**

-   ECS cluster and service status
-   Running task count vs desired count
-   Task definition version
-   Deployment status and events
-   Task health status
-   Recent CloudWatch logs

---

### trigger-build.sh

Trigger a Jenkins build via CLI.

**Usage:**

```bash
./trigger-build.sh <environment> <action>
```

**Example:**

```bash
export JENKINS_URL="http://your-jenkins:8081"
export JENKINS_USER="admin"
export JENKINS_TOKEN="your-api-token"
./trigger-build.sh production deploy
```

**Parameters:**

-   `environment`: dev, staging, or production
-   `action`: deploy, build-only, or rollback

**Prerequisites:**

-   Jenkins CLI jar downloaded
-   `JENKINS_URL` environment variable set
-   `JENKINS_USER` and `JENKINS_TOKEN` for authentication

---

### verify-environment.sh

Verify that all required AWS resources are properly configured.

**Usage:**

```bash
./verify-environment.sh <environment>
```

**Example:**

```bash
./verify-environment.sh production
```

**What it verifies:**

-   ECS Cluster exists
-   ECS Service exists
-   Task Definition is registered
-   ECR Repository exists
-   Secrets Manager secrets are configured
-   CloudWatch log groups exist
-   IAM roles are configured
-   Production-specific checks (ALB, scaling)

---

### cleanup-old-images.sh

Clean up old Docker images from ECR to save storage costs.

**Usage:**

```bash
./cleanup-old-images.sh [--dry-run] [--keep N]
```

**Examples:**

```bash
# Dry run to see what would be deleted
./cleanup-old-images.sh --dry-run

# Keep only the latest 5 images
./cleanup-old-images.sh --keep 5

# Default: keep latest 10 images
./cleanup-old-images.sh
```

**Options:**

-   `--dry-run` - Show what would be deleted without actually deleting
-   `--keep N` - Number of recent images to keep (default: 10)

---

## Quick Reference

### Check Deployment Status

```bash
cd jenkins
./check-deployment.sh dev
./check-deployment.sh staging
./check-deployment.sh production
```

### Trigger Builds

```bash
# Set up credentials first
export JENKINS_URL="http://your-jenkins:8081"
export JENKINS_USER="your-username"
export JENKINS_TOKEN="your-api-token"

# Deploy to environments
./trigger-build.sh dev deploy
./trigger-build.sh staging deploy
./trigger-build.sh production deploy
```

### Verify Environments

```bash
# Verify all environments are properly configured
./verify-environment.sh dev
./verify-environment.sh staging
./verify-environment.sh production
```

### Clean Up ECR

```bash
# See what would be deleted
./cleanup-old-images.sh --dry-run

# Actually clean up (keeps latest 10)
./cleanup-old-images.sh
```

## Prerequisites

### AWS CLI

All scripts require AWS CLI to be installed and configured:

```bash
# Check AWS CLI
aws --version

# Configure credentials
aws configure
```

### Jenkins CLI (for trigger-build.sh)

```bash
# Download Jenkins CLI
curl -o jenkins-cli.jar http://your-jenkins:8081/jnlpJars/jenkins-cli.jar

# Test connection
java -jar jenkins-cli.jar -s http://your-jenkins:8081/ who-am-i
```

### Required Environment Variables

For `trigger-build.sh`:

-   `JENKINS_URL` - Your Jenkins server URL
-   `JENKINS_USER` - Your Jenkins username
-   `JENKINS_TOKEN` - Your Jenkins API token

For all scripts:

-   `AWS_REGION` - AWS region (default: us-east-1)

## Common Workflows

### Complete Deployment Check

```bash
# 1. Verify environment setup
./verify-environment.sh production

# 2. Trigger deployment
export JENKINS_URL="http://your-jenkins:8081"
export JENKINS_USER="admin"
export JENKINS_TOKEN="your-token"
./trigger-build.sh production deploy

# 3. Monitor deployment
./check-deployment.sh production

# 4. Check logs
aws logs tail /ecs/taskactivity-production --follow
```

### Weekly Maintenance

```bash
# Clean up old images from all environments
./cleanup-old-images.sh --keep 5

# Verify all environments
for env in dev staging production; do
    echo "Checking ${env}..."
    ./verify-environment.sh ${env}
done
```

### Troubleshooting Deployment

```bash
# 1. Check current deployment status
./check-deployment.sh production

# 2. View service events
aws ecs describe-services \
  --cluster taskactivity-cluster-production \
  --services taskactivity-service-production \
  --query 'services[0].events[0:10]'

# 3. Check stopped tasks
aws ecs describe-tasks \
  --cluster taskactivity-cluster-production \
  --tasks $(aws ecs list-tasks \
    --cluster taskactivity-cluster-production \
    --desired-status STOPPED \
    --query 'taskArns[0]' --output text)

# 4. View application logs
aws logs tail /ecs/taskactivity-production --since 1h
```

## Script Permissions

Make scripts executable (Linux/Mac/WSL):

```bash
chmod +x *.sh
```

For Windows PowerShell, run scripts using bash (WSL or Git Bash):

```powershell
# Using WSL
wsl bash -c "./check-deployment.sh production"

# Using Git Bash
"C:\Program Files\Git\bin\bash.exe" ./check-deployment.sh production
```

## Exit Codes

All scripts follow standard exit code conventions:

-   `0` - Success
-   `1` - Error or failure
-   `130` - Script interrupted (Ctrl+C)

Use in automation:

```bash
if ./verify-environment.sh production; then
    echo "Environment verified, deploying..."
    ./trigger-build.sh production deploy
else
    echo "Environment verification failed!"
    exit 1
fi
```

## Logging

Scripts output colored status messages:

-   🟢 **Green** - Success
-   🟡 **Yellow** - Warning or in-progress
-   🔴 **Red** - Error or failure
-   🔵 **Blue** - Information

## Support

For issues with these scripts:

1. Check AWS credentials are configured
2. Verify AWS resources exist (use `verify-environment.sh`)
3. Ensure AWS CLI is up to date
4. Check script permissions
5. Review [JENKINS_QUICK_REFERENCE.md](../docs/JENKINS_QUICK_REFERENCE.md) for troubleshooting

## Additional Resources

-   [Jenkins Setup Guide](Jenkins_Setup.md)
-   [Docker Credential Security Guide](Docker_Credential_Security_Guide.md) - Secure Docker credential storage
-   [Environment Configuration](Jenkins_Environments.md)
-   [Quick Reference](Jenkins_Quick_Reference.md)
-   [AWS CLI Documentation](https://docs.aws.amazon.com/cli/)
-   [Jenkins CLI Guide](https://www.jenkins.io/doc/book/managing/cli/)

---

**Last Updated:** January 2026  
**Maintained By:** DevOps Team
