# Jenkins Environment Configuration Guide

This guide provides environment-specific configuration files and instructions for deploying to dev, staging, and production environments using Jenkins.

## Table of Contents

-   [Overview](#overview)
-   [Environment Strategy](#environment-strategy)
-   [AWS Resource Naming](#aws-resource-naming)
-   [Configuration Files](#configuration-files)
-   [Environment Setup](#environment-setup)
-   [Deployment Workflow](#deployment-workflow)
-   [Best Practices](#best-practices)

## Overview

The Task Activity application supports three environments:

-   **Dev:** Development/testing environment
-   **Staging:** Pre-production validation environment
-   **Production:** Live production environment

Each environment has isolated AWS resources and can be deployed independently.

## Environment Strategy

### Environment Characteristics

| Aspect               | Dev                | Staging              | Production         |
| -------------------- | ------------------ | -------------------- | ------------------ |
| **Purpose**          | Active development | Pre-prod validation  | Live users         |
| **Deploy Frequency** | Multiple times/day | Daily                | Weekly/on-demand   |
| **Test Coverage**    | Unit tests         | Full test suite      | Full + smoke tests |
| **Approval**         | None               | Optional             | Required           |
| **Rollback**         | Fast/automatic     | Automatic on failure | Manual approval    |
| **Cost**             | Minimal (1 task)   | Moderate (1-2 tasks) | Scaled (2+ tasks)  |
| **Monitoring**       | Basic              | Enhanced             | Full (alerts)      |
| **Data**             | Sample/test data   | Production-like      | Real user data     |

### Deployment Flow

```
main branch
    │
    ├─→ Dev (automatic on merge)
    │
    ├─→ Staging (manual trigger or nightly)
    │
    └─→ Production (manual with approval)
```

## AWS Resource Naming

All AWS resources follow this naming convention: `taskactivity-<resource>-<environment>`

### Dev Environment

```
ECS Cluster:        taskactivity-cluster-dev
ECS Service:        taskactivity-service-dev
Task Definition:    taskactivity-task-dev
ECR Repository:     taskactivity (shared)
Load Balancer:      taskactivity-alb-dev
Target Group:       taskactivity-tg-dev
Security Group:     taskactivity-sg-dev
CloudWatch Logs:    /ecs/taskactivity-dev
Secrets:            taskactivity/dev/*
```

### Staging Environment

```
ECS Cluster:        taskactivity-cluster-staging
ECS Service:        taskactivity-service-staging
Task Definition:    taskactivity-task-staging
ECR Repository:     taskactivity (shared)
Load Balancer:      taskactivity-alb-staging
Target Group:       taskactivity-tg-staging
Security Group:     taskactivity-sg-staging
CloudWatch Logs:    /ecs/taskactivity-staging
Secrets:            taskactivity/staging/*
```

### Production Environment

```
ECS Cluster:        taskactivity-cluster-production
ECS Service:        taskactivity-service-production
Task Definition:    taskactivity-task-production
ECR Repository:     taskactivity (shared)
Load Balancer:      taskactivity-alb-production
Target Group:       taskactivity-tg-production
Security Group:     taskactivity-sg-production
CloudWatch Logs:    /ecs/taskactivity-production
Secrets:            taskactivity/production/*
```

## Configuration Files

### ECS Task Definition Templates

Create environment-specific task definitions in `aws/` directory:

**File:** `aws/taskactivity-task-dev.json`

```json
{
    "family": "taskactivity-task-dev",
    "networkMode": "awsvpc",
    "requiresCompatibilities": ["FARGATE"],
    "cpu": "256",
    "memory": "512",
    "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole",
    "taskRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskRole",
    "containerDefinitions": [
        {
            "name": "taskactivity-dev",
            "image": "ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com/taskactivity:latest-dev",
            "essential": true,
            "portMappings": [
                {
                    "containerPort": 8080,
                    "protocol": "tcp"
                }
            ],
            "environment": [
                {
                    "name": "SPRING_PROFILES_ACTIVE",
                    "value": "aws,dev"
                },
                {
                    "name": "LOGGING_LEVEL_ROOT",
                    "value": "DEBUG"
                }
            ],
            "secrets": [
                {
                    "name": "SPRING_DATASOURCE_URL",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/dev/database/credentials:jdbcUrl::"
                },
                {
                    "name": "SPRING_DATASOURCE_USERNAME",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/dev/database/credentials:username::"
                },
                {
                    "name": "SPRING_DATASOURCE_PASSWORD",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/dev/database/credentials:password::"
                },
                {
                    "name": "ADMIN_DEFAULT_PASSWORD",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/dev/admin/credentials:password::"
                }
            ],
            "logConfiguration": {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "/ecs/taskactivity-dev",
                    "awslogs-region": "REGION",
                    "awslogs-stream-prefix": "ecs"
                }
            },
            "healthCheck": {
                "command": [
                    "CMD-SHELL",
                    "curl -f http://localhost:8081/actuator/health || exit 1"
                ],
                "interval": 30,
                "timeout": 5,
                "retries": 3,
                "startPeriod": 60
            }
        }
    ]
}
```

**File:** `aws/taskactivity-task-staging.json`

```json
{
    "family": "taskactivity-task-staging",
    "networkMode": "awsvpc",
    "requiresCompatibilities": ["FARGATE"],
    "cpu": "512",
    "memory": "1024",
    "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole",
    "taskRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskRole",
    "containerDefinitions": [
        {
            "name": "taskactivity-staging",
            "image": "ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com/taskactivity:latest-staging",
            "essential": true,
            "portMappings": [
                {
                    "containerPort": 8080,
                    "protocol": "tcp"
                }
            ],
            "environment": [
                {
                    "name": "SPRING_PROFILES_ACTIVE",
                    "value": "aws,staging"
                },
                {
                    "name": "LOGGING_LEVEL_ROOT",
                    "value": "INFO"
                }
            ],
            "secrets": [
                {
                    "name": "SPRING_DATASOURCE_URL",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/staging/database/credentials:jdbcUrl::"
                },
                {
                    "name": "SPRING_DATASOURCE_USERNAME",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/staging/database/credentials:username::"
                },
                {
                    "name": "SPRING_DATASOURCE_PASSWORD",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/staging/database/credentials:password::"
                },
                {
                    "name": "ADMIN_DEFAULT_PASSWORD",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/staging/admin/credentials:password::"
                }
            ],
            "logConfiguration": {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "/ecs/taskactivity-staging",
                    "awslogs-region": "REGION",
                    "awslogs-stream-prefix": "ecs"
                }
            },
            "healthCheck": {
                "command": [
                    "CMD-SHELL",
                    "curl -f http://localhost:8081/actuator/health || exit 1"
                ],
                "interval": 30,
                "timeout": 5,
                "retries": 3,
                "startPeriod": 60
            }
        }
    ]
}
```

**File:** `aws/taskactivity-task-production.json`

```json
{
    "family": "taskactivity-task-production",
    "networkMode": "awsvpc",
    "requiresCompatibilities": ["FARGATE"],
    "cpu": "1024",
    "memory": "2048",
    "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole",
    "taskRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskRole",
    "containerDefinitions": [
        {
            "name": "taskactivity-production",
            "image": "ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com/taskactivity:latest-production",
            "essential": true,
            "portMappings": [
                {
                    "containerPort": 8080,
                    "protocol": "tcp"
                }
            ],
            "environment": [
                {
                    "name": "SPRING_PROFILES_ACTIVE",
                    "value": "aws,production"
                },
                {
                    "name": "LOGGING_LEVEL_ROOT",
                    "value": "WARN"
                },
                {
                    "name": "LOGGING_LEVEL_COM_AMMONS",
                    "value": "INFO"
                }
            ],
            "secrets": [
                {
                    "name": "SPRING_DATASOURCE_URL",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/production/database/credentials:jdbcUrl::"
                },
                {
                    "name": "SPRING_DATASOURCE_USERNAME",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/production/database/credentials:username::"
                },
                {
                    "name": "SPRING_DATASOURCE_PASSWORD",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/production/database/credentials:password::"
                },
                {
                    "name": "ADMIN_DEFAULT_PASSWORD",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/production/admin/credentials:password::"
                },
                {
                    "name": "CLOUDFLARE_TUNNEL_CREDENTIALS",
                    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:taskactivity/production/cloudflare/tunnel-credentials::"
                }
            ],
            "logConfiguration": {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "/ecs/taskactivity-production",
                    "awslogs-region": "REGION",
                    "awslogs-stream-prefix": "ecs"
                }
            },
            "healthCheck": {
                "command": [
                    "CMD-SHELL",
                    "curl -f http://localhost:8081/actuator/health || exit 1"
                ],
                "interval": 30,
                "timeout": 5,
                "retries": 3,
                "startPeriod": 90
            }
        }
    ]
}
```

## Environment Setup

### 1. Create AWS Secrets

Run these commands for each environment:

**Dev Environment:**

```powershell
# Database credentials
aws secretsmanager create-secret `
    --name taskactivity/dev/database/credentials `
    --secret-string '{\"username\":\"admin\",\"password\":\"DevPassword123!\",\"jdbcUrl\":\"jdbc:postgresql://dev-db.us-east-1.rds.amazonaws.com:5432/taskactivity_dev\"}'

# Admin credentials
aws secretsmanager create-secret `
    --name taskactivity/dev/admin/credentials `
    --secret-string '{\"password\":\"DevAdmin123!\"}'
```

**Staging Environment:**

```powershell
# Database credentials
aws secretsmanager create-secret `
    --name taskactivity/staging/database/credentials `
    --secret-string '{\"username\":\"admin\",\"password\":\"StagingPassword123!\",\"jdbcUrl\":\"jdbc:postgresql://staging-db.us-east-1.rds.amazonaws.com:5432/taskactivity_staging\"}'

# Admin credentials
aws secretsmanager create-secret `
    --name taskactivity/staging/admin/credentials `
    --secret-string '{\"password\":\"StagingAdmin123!\"}'
```

**Production Environment:**

```powershell
# Database credentials
aws secretsmanager create-secret `
    --name taskactivity/production/database/credentials `
    --secret-string '{\"username\":\"admin\",\"password\":\"ProductionPassword123!\",\"jdbcUrl\":\"jdbc:postgresql://prod-db.us-east-1.rds.amazonaws.com:5432/taskactivity_prod\"}'

# Admin credentials
aws secretsmanager create-secret `
    --name taskactivity/production/admin/credentials `
    --secret-string '{\"password\":\"ProductionAdmin123!\"}'

# Cloudflare credentials (production only)
aws secretsmanager create-secret `
    --name taskactivity/production/cloudflare/tunnel-credentials `
    --secret-string file://cloudflare-credentials.json
```

### 2. Create ECS Clusters

```powershell
# Dev cluster
aws ecs create-cluster --cluster-name taskactivity-cluster-dev

# Staging cluster
aws ecs create-cluster --cluster-name taskactivity-cluster-staging

# Production cluster
aws ecs create-cluster --cluster-name taskactivity-cluster-production
```

### 3. Create CloudWatch Log Groups

```powershell
# Dev logs
aws logs create-log-group --log-group-name /ecs/taskactivity-dev

# Staging logs
aws logs create-log-group --log-group-name /ecs/taskactivity-staging

# Production logs with retention
aws logs create-log-group --log-group-name /ecs/taskactivity-production
aws logs put-retention-policy --log-group-name /ecs/taskactivity-production --retention-in-days 90
```

### 4. Register Task Definitions

```powershell
# Dev task definition
aws ecs register-task-definition --cli-input-json file://aws/taskactivity-task-dev.json

# Staging task definition
aws ecs register-task-definition --cli-input-json file://aws/taskactivity-task-staging.json

# Production task definition
aws ecs register-task-definition --cli-input-json file://aws/taskactivity-task-production.json
```

### 5. Create ECS Services

```powershell
# Dev service (1 task, public IP)
aws ecs create-service `
  --cluster taskactivity-cluster-dev `
  --service-name taskactivity-service-dev `
  --task-definition taskactivity-task-dev:1 `
  --desired-count 1 `
  --launch-type FARGATE `
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"

# Staging service (1 task, ALB)
aws ecs create-service `
  --cluster taskactivity-cluster-staging `
  --service-name taskactivity-service-staging `
  --task-definition taskactivity-task-staging:1 `
  --desired-count 1 `
  --launch-type FARGATE `
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:REGION:ACCOUNT:targetgroup/taskactivity-tg-staging/xxx,containerName=taskactivity-staging,containerPort=8080" `
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx]}"

# Production service (2+ tasks, ALB, health checks)
aws ecs create-service `
  --cluster taskactivity-cluster-production `
  --service-name taskactivity-service-production `
  --task-definition taskactivity-task-production:1 `
  --desired-count 2 `
  --launch-type FARGATE `
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:REGION:ACCOUNT:targetgroup/taskactivity-tg-production/xxx,containerName=taskactivity-production,containerPort=8080" `
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-xxx]}" `
  --health-check-grace-period-seconds 120 `
  --deployment-configuration "maximumPercent=200,minimumHealthyPercent=100"
```

## Deployment Workflow

### Dev Deployment (Automatic)

```groovy
// Triggered automatically on push to main branch
pipeline {
    triggers {
        githubPush()
    }
}
```

**Jenkins Job:** Triggered by GitHub webhook  
**Parameters:** `ENVIRONMENT=dev`, `DEPLOY_ACTION=deploy`, `SKIP_TESTS=false`

### Staging Deployment (Manual or Scheduled)

```groovy
// Triggered manually or nightly at 2 AM
pipeline {
    triggers {
        cron('0 2 * * *')  // Nightly build
    }
}
```

**Jenkins Job:** Manual build or cron trigger  
**Parameters:** `ENVIRONMENT=staging`, `DEPLOY_ACTION=deploy`, `SKIP_TESTS=false`

### Production Deployment (Manual with Approval)

**Option 1: Using Jenkins Input Step**

Add to Jenkinsfile:

```groovy
stage('Approve Production Deploy') {
    when {
        expression { params.ENVIRONMENT == 'production' }
    }
    steps {
        input message: 'Deploy to production?', ok: 'Deploy', submitter: 'admin,lead-dev'
    }
}
```

**Option 2: Separate Jenkins Job**

Create a separate job: `TaskActivity-Deploy-Production`

-   Requires manual trigger
-   Copy artifact from staging build
-   Add approval notification

## Best Practices

### 1. Environment Promotion

Always promote code through environments:

```
Dev → Staging → Production
```

Never deploy directly to production without staging validation.

### 2. Configuration Management

-   Store environment-specific configs in AWS Secrets Manager
-   Use Spring profiles: `dev`, `staging`, `production`
-   Never hardcode environment values in code

### 3. Testing Strategy

-   **Dev:** Unit tests only
-   **Staging:** Full test suite + integration tests
-   **Production:** Smoke tests post-deployment

### 4. Rollback Strategy

-   Keep last 5 task definitions per environment
-   Automated rollback on health check failures
-   Manual rollback for production requires approval

### 5. Monitoring & Alerts

**Dev:**

-   CloudWatch logs (7-day retention)
-   No alerts

**Staging:**

-   CloudWatch logs (30-day retention)
-   Error rate alerts

**Production:**

-   CloudWatch logs (90-day retention)
-   Full metric collection
-   Error rate, latency, availability alerts
-   PagerDuty/SNS integration

### 6. Database Management

-   **Dev:** Can reset/recreate as needed
-   **Staging:** Production-like data (anonymized)
-   **Production:** Regular backups, point-in-time recovery

### 7. Cost Optimization

**Dev:**

-   1 Fargate task (256 CPU, 512 MB)
-   No NAT gateway (public IP)
-   Single AZ

**Staging:**

-   1 Fargate task (512 CPU, 1024 MB)
-   Application Load Balancer
-   Single AZ

**Production:**

-   2+ Fargate tasks (1024 CPU, 2048 MB)
-   Application Load Balancer
-   Multi-AZ deployment
-   Auto-scaling enabled

---

**Last Updated:** October 2025  
**Maintained By:** DevOps Team
