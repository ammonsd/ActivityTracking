# Docker Credential Security Guide

**Description:** Instructions for securing Docker credentials in Jenkins to eliminate unencrypted credential storage warnings.

**Author:** Dean Ammons  
**Date:** January 2026

---

## Problem Statement

Jenkins is displaying this warning during ECR push operations:

```
WARNING! Your credentials are stored unencrypted in '/var/lib/jenkins/.docker/config.json'.
Configure a credential helper to remove this warning. See
https://docs.docker.com/go/credential-store/
```

This occurs because Docker stores authentication credentials in plain text after running `docker login`. This is a **security risk** in production environments.

---

## Understanding the Issue

### What Causes This Warning?

When you run:

```bash
aws ecr get-login-password --region us-east-1 | \
docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com
```

Docker stores the credentials in **plain text** at:

-   Linux/Jenkins: `/var/lib/jenkins/.docker/config.json`
-   Windows: `C:\Users\<username>\.docker\config.json`

### Why Is This a Problem?

1. **Security Risk**: Anyone with file system access can read credentials
2. **Compliance**: Violates security best practices and compliance requirements
3. **Audit Concerns**: Plain text credentials are flagged in security audits
4. **Container Security**: If Jenkins container is compromised, credentials are exposed

---

## Solution Overview

There are **three** recommended solutions, ranked by security and ease of implementation:

| Solution                                            | Security   | Ease       | Recommended For       |
| --------------------------------------------------- | ---------- | ---------- | --------------------- |
| **1. ECR Credential Helper**                        | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐   | AWS ECR (Recommended) |
| **2. Pass Credential Store**                        | ⭐⭐⭐⭐   | ⭐⭐⭐     | Linux environments    |
| **3. Docker Login with --password-stdin + cleanup** | ⭐⭐⭐     | ⭐⭐⭐⭐⭐ | Quick fix (not ideal) |

We'll implement **Solution 1** as it's AWS-native and most secure for ECR.

---

## Solution 1: AWS ECR Credential Helper (Recommended)

The Amazon ECR credential helper automatically retrieves and refreshes credentials using AWS IAM without storing them on disk.

### How It Works

```
┌─────────────┐         ┌──────────────┐         ┌─────────┐
│   Docker    │────────▶│ ECR Credential│────────▶│   IAM   │
│   Client    │         │    Helper     │         │         │
└─────────────┘         └──────────────┘         └─────────┘
                              │
                              ▼
                        Get temporary
                        credentials
                        (no disk storage)
```

### Step 1: Install ECR Credential Helper

#### On Jenkins Server (Linux/Ubuntu)

```bash
# Install the ECR credential helper
sudo apt-get update
sudo apt-get install amazon-ecr-credential-helper

# Verify installation
docker-credential-ecr-login version
```

#### On Jenkins Docker Container

If Jenkins runs in Docker, you need to install it in the container. Create a custom Dockerfile:

**File: `jenkins/Dockerfile.jenkins-custom`**

```dockerfile
FROM jenkins/jenkins:lts-jdk21

# Switch to root to install packages
USER root

# Install Amazon ECR credential helper
RUN apt-get update && \
    apt-get install -y \
        amazon-ecr-credential-helper \
        awscli && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Switch back to jenkins user
USER jenkins
```

Build and run the custom Jenkins image:

```bash
# Build custom Jenkins image
docker build -t jenkins-custom:latest -f jenkins/Dockerfile.jenkins-custom .

# Stop existing Jenkins container
docker stop jenkins
docker rm jenkins

# Run new Jenkins with credential helper
docker run -d \
  --name jenkins \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins-custom:latest
```

### Step 2: Configure Docker to Use ECR Credential Helper

Create or modify the Docker config file for the Jenkins user:

```bash
# As Jenkins user (or in Jenkins container)
mkdir -p ~/.docker
cat > ~/.docker/config.json <<EOF
{
  "credHelpers": {
    "123456789.dkr.ecr.us-east-1.amazonaws.com": "ecr-login",
    "public.ecr.aws": "ecr-login"
  }
}
EOF

# Set proper permissions
chmod 600 ~/.docker/config.json
```

**Important**: Replace `123456789` with your actual AWS account ID.

For multiple regions:

```json
{
    "credHelpers": {
        "123456789.dkr.ecr.us-east-1.amazonaws.com": "ecr-login",
        "123456789.dkr.ecr.us-west-2.amazonaws.com": "ecr-login",
        "123456789.dkr.ecr.eu-west-1.amazonaws.com": "ecr-login",
        "public.ecr.aws": "ecr-login"
    }
}
```

### Step 3: Verify AWS Credentials Are Available

The credential helper uses AWS credentials from:

1. Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
2. AWS credentials file (`~/.aws/credentials`)
3. IAM role (if Jenkins runs on EC2)
4. Jenkins AWS credentials binding

Verify:

```bash
# Test AWS credentials
aws sts get-caller-identity

# Expected output:
# {
#     "UserId": "AIDAI...",
#     "Account": "123456789",
#     "Arn": "arn:aws:iam::123456789:user/jenkins-user"
# }
```

### Step 4: Update Jenkinsfile (Remove Manual Login)

**BEFORE (Insecure):**

```groovy
stage('Push to ECR') {
    steps {
        script {
            withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                // Manual login - stores credentials unencrypted
                sh """
                    aws ecr get-login-password --region ${AWS_REGION} | \
                    docker login --username AWS --password-stdin ${ECR_REGISTRY}
                """

                sh "docker push ${IMAGE_FULL}"
                sh "docker push ${IMAGE_LATEST}"
            }
        }
    }
}
```

**AFTER (Secure with Credential Helper):**

```groovy
stage('Push to ECR') {
    steps {
        script {
            withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                // No manual login needed - credential helper handles it automatically
                echo "Using ECR credential helper for authentication..."

                // Docker push will automatically authenticate via credential helper
                sh "docker push ${IMAGE_FULL}"
                sh "docker push ${IMAGE_LATEST}"

                echo "Images pushed successfully to ECR"
            }
        }
    }
}
```

### Step 5: Test the Configuration

```bash
# Test pushing to ECR (no login needed)
docker tag nginx:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/test:latest
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/test:latest

# Should work without docker login command!
# Should NOT show credential warning
```

### Step 6: Verify Security

Check that credentials are NOT stored in config.json:

```bash
# Check Docker config
cat ~/.docker/config.json

# Should look like:
# {
#   "credHelpers": {
#     "123456789.dkr.ecr.us-east-1.amazonaws.com": "ecr-login"
#   }
# }

# Should NOT contain "auths" section with base64 encoded credentials!
```

---

## Solution 2: Pass Credential Store (Linux Alternative)

If you're not using ECR or want a general solution for multiple registries:

### Install Pass

```bash
# Install pass and GPG
sudo apt-get install pass gnupg2

# Generate GPG key
gpg --generate-key
# Follow prompts (use jenkins@localhost as email)

# Get GPG key ID
gpg --list-keys
# Note the key ID (e.g., 1234567890ABCDEF)

# Initialize pass with GPG key
pass init <GPG_KEY_ID>
```

### Install Docker Credential Helper

```bash
# Download and install docker-credential-pass
wget https://github.com/docker/docker-credential-helpers/releases/download/v0.7.0/docker-credential-pass-v0.7.0-amd64.tar.gz
tar -xf docker-credential-pass-v0.7.0-amd64.tar.gz
sudo mv docker-credential-pass /usr/local/bin/
sudo chmod +x /usr/local/bin/docker-credential-pass
```

### Configure Docker

```bash
cat > ~/.docker/config.json <<EOF
{
  "credsStore": "pass"
}
EOF
```

### Test

```bash
# Login to Docker registry
echo "password" | docker login --username user --password-stdin registry.example.com

# Verify credentials are encrypted
pass show
```

---

## Solution 3: Clean Up After Login (Quick Fix)

If you can't install credential helpers immediately, add cleanup to your Jenkinsfile:

```groovy
stage('Push to ECR') {
    steps {
        script {
            withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                try {
                    // Login to ECR
                    sh """
                        aws ecr get-login-password --region ${AWS_REGION} | \
                        docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    """

                    // Push images
                    sh "docker push ${IMAGE_FULL}"
                    sh "docker push ${IMAGE_LATEST}"

                    echo "Images pushed successfully to ECR"
                } finally {
                    // IMPORTANT: Clean up credentials after use
                    sh """
                        docker logout ${ECR_REGISTRY}
                        rm -f ~/.docker/config.json
                    """
                    echo "Docker credentials cleaned up"
                }
            }
        }
    }
}
```

**⚠️ Note**: This is a band-aid solution. Credentials are still temporarily stored in plain text during the build.

---

## Comparison Matrix

| Feature                 | ECR Helper     | Pass Store    | Manual + Cleanup |
| ----------------------- | -------------- | ------------- | ---------------- |
| **Credentials on Disk** | ❌ Never       | ✅ Encrypted  | ⚠️ Temporarily   |
| **Auto-Refresh**        | ✅ Yes         | ❌ No         | ❌ No            |
| **AWS Native**          | ✅ Yes         | ❌ No         | ❌ No            |
| **Setup Complexity**    | ⭐⭐ Easy      | ⭐⭐⭐ Medium | ⭐ Very Easy     |
| **Security Rating**     | ⭐⭐⭐⭐⭐     | ⭐⭐⭐⭐      | ⭐⭐⭐           |
| **Maintenance**         | ⭐⭐⭐⭐⭐ Low | ⭐⭐⭐ Medium | ⭐⭐ High        |

---

## Recommended Implementation Plan

### Phase 1: Immediate (Day 1)

✅ Implement Solution 3 (cleanup) to stop warning immediately  
✅ Document the issue and plan

### Phase 2: Short-term (Week 1)

✅ Install ECR credential helper on Jenkins server  
✅ Configure Docker to use credential helper  
✅ Test with non-production builds

### Phase 3: Rollout (Week 2)

✅ Update Jenkinsfile to remove manual login  
✅ Deploy to development environment  
✅ Monitor for issues  
✅ Deploy to staging and production

---

## Troubleshooting

### Issue: "credential helper not found"

```bash
# Verify installation
which docker-credential-ecr-login
# Should output: /usr/bin/docker-credential-ecr-login

# Test credential helper directly
echo "https://123456789.dkr.ecr.us-east-1.amazonaws.com" | docker-credential-ecr-login get
```

### Issue: "no basic auth credentials"

This means AWS credentials are not available. Check:

```bash
# Verify AWS credentials
aws sts get-caller-identity

# Check environment variables
env | grep AWS
```

### Issue: "permission denied"

```bash
# Fix Docker config permissions
chmod 600 ~/.docker/config.json
chown jenkins:jenkins ~/.docker/config.json
```

### Issue: Still seeing warning after configuration

```bash
# Remove old config completely
rm -f ~/.docker/config.json

# Recreate with credential helper only
cat > ~/.docker/config.json <<EOF
{
  "credHelpers": {
    "123456789.dkr.ecr.us-east-1.amazonaws.com": "ecr-login"
  }
}
EOF
```

---

## Security Best Practices

1. **Never** commit Docker config.json to version control
2. **Always** use credential helpers in production
3. **Rotate** AWS credentials regularly (every 90 days)
4. **Use** IAM roles instead of access keys when possible (EC2, ECS)
5. **Audit** Jenkins credential usage regularly
6. **Enable** AWS CloudTrail for ECR access logging
7. **Restrict** ECR access to specific IP ranges if possible

---

## Additional Resources

-   [Docker Credential Helpers Documentation](https://docs.docker.com/engine/reference/commandline/login/#credential-helpers)
-   [Amazon ECR Credential Helper GitHub](https://github.com/awslabs/amazon-ecr-credential-helper)
-   [AWS ECR Authentication Documentation](https://docs.aws.amazon.com/AmazonECR/latest/userguide/registry_auth.html)
-   [Jenkins Security Best Practices](https://www.jenkins.io/doc/book/security/best-practices/)

---

## Next Steps

1. Choose your implementation approach (recommend Solution 1)
2. Schedule maintenance window for Jenkins server updates
3. Install and configure ECR credential helper
4. Update Jenkinsfile to remove manual login
5. Test thoroughly in development first
6. Roll out to production
7. Document the change in your team's runbook

---

**Questions?** Refer to the [Jenkins Setup Guide](./Jenkins_Setup.md) for general Jenkins configuration.
