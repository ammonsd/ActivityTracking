# Concurrency and Horizontal Scaling Guide

## Overview

The Task Activity Management application is designed as a **stateless, horizontally scalable** Spring Boot application that follows 12-Factor App principles. This document explains how the application handles concurrency and how to scale it across multiple deployment environments.

## Table of Contents

-   [Architecture for Concurrency](#architecture-for-concurrency)
-   [Stateless Design](#stateless-design)
-   [Deployment-Specific Scaling](#deployment-specific-scaling)
    -   [AWS ECS Fargate](#aws-ecs-fargate)
    -   [Kubernetes](#kubernetes)
    -   [Docker Compose (Development)](#docker-compose-development)
-   [Load Balancing](#load-balancing)
-   [Session Management](#session-management)
-   [Database Connection Pooling](#database-connection-pooling)
-   [Health Checks and Readiness](#health-checks-and-readiness)
-   [Scaling Best Practices](#scaling-best-practices)
-   [Monitoring and Metrics](#monitoring-and-metrics)

---

## Architecture for Concurrency

The application is built to support horizontal scaling through:

1. **Stateless Application Design** - No local state stored on application servers
2. **Shared Database Backend** - PostgreSQL as the single source of truth
3. **Externalized Configuration** - All configuration via environment variables
4. **Container-Based Deployment** - Docker containers ensure consistency across instances
5. **Health Check Endpoints** - Spring Boot Actuator provides `/actuator/health` for orchestration

### Process Model

Each application instance runs as an independent process that:

-   Handles HTTP requests independently
-   Maintains its own connection pool to PostgreSQL
-   Logs to stdout/stderr (captured by orchestration platform)
-   Shares no state with other instances

---

## Stateless Design

### What Makes This Application Stateless?

✅ **No Local File Storage** - All data persisted to PostgreSQL database  
✅ **No In-Memory Sessions** - Spring Security session data can be externalized  
✅ **Externalized Configuration** - Environment variables for all settings  
✅ **Stateless REST API** - Each API request is independent  
✅ **JWT Authentication** - Token-based authentication (no server-side session required)

### Session Management

**Current Implementation:**

```properties
# Session stored in-memory (suitable for single instance or sticky sessions)
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
```

**For Multi-Instance Without Sticky Sessions:**

To enable true stateless operation across multiple instances, externalize session storage:

1. **Redis Session Store** (Recommended):

```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

2. **Configuration**:

```properties
spring.session.store-type=redis
spring.redis.host=${REDIS_HOST:localhost}
spring.redis.port=${REDIS_PORT:6379}
```

3. **JDBC Session Store** (Alternative):

```properties
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always
```

**Note:** For current deployments with load balancer sticky sessions enabled, in-memory sessions work fine.

---

## Deployment-Specific Scaling

### AWS ECS Fargate

**Current Configuration:**

-   **Development:** 1 task instance
-   **Staging:** 1 task instance
-   **Production:** 2 task instances (recommended)

#### Manual Scaling

**Via CloudFormation:**

Edit `cloudformation/parameters/production.json`:

```json
{
    "ParameterKey": "DesiredCount",
    "ParameterValue": "3" // Change from 2 to 3
}
```

Deploy the updated stack:

```powershell
aws cloudformation update-stack `
    --stack-name taskactivity-prod `
    --template-body file://cloudformation/templates/infrastructure.yaml `
    --parameters file://cloudformation/parameters/production.json
```

**Via AWS CLI:**

```powershell
# Scale to 3 tasks
aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity-service `
    --desired-count 3 `
    --region us-east-1
```

**Via AWS Console:**

1. Navigate to ECS → Clusters → taskactivity-cluster
2. Select taskactivity-service
3. Click "Update Service"
4. Change "Number of tasks" (Desired Count)
5. Click "Update"

#### Auto Scaling (Recommended for Production)

To implement ECS Service Auto Scaling, add to your CloudFormation template:

```yaml
# Auto Scaling Target
ServiceScalingTarget:
    Type: AWS::ApplicationAutoScaling::ScalableTarget
    Properties:
        MaxCapacity: 10
        MinCapacity: 2
        ResourceId: !Sub service/${ECSCluster}/${ECSService.Name}
        RoleARN: !GetAtt AutoScalingRole.Arn
        ScalableDimension: ecs:service:DesiredCount
        ServiceNamespace: ecs

# CPU-Based Scaling Policy
ServiceScalingPolicyCPU:
    Type: AWS::ApplicationAutoScaling::ScalingPolicy
    Properties:
        PolicyName: cpu-scaling
        PolicyType: TargetTrackingScaling
        ScalingTargetId: !Ref ServiceScalingTarget
        TargetTrackingScalingPolicyConfiguration:
            PredefinedMetricSpecification:
                PredefinedMetricType: ECSServiceAverageCPUUtilization
            TargetValue: 70.0
            ScaleInCooldown: 300
            ScaleOutCooldown: 60

# Request Count Scaling Policy
ServiceScalingPolicyRequests:
    Type: AWS::ApplicationAutoScaling::ScalingPolicy
    Properties:
        PolicyName: request-count-scaling
        PolicyType: TargetTrackingScaling
        ScalingTargetId: !Ref ServiceScalingTarget
        TargetTrackingScalingPolicyConfiguration:
            PredefinedMetricSpecification:
                PredefinedMetricType: ALBRequestCountPerTarget
                ResourceLabel: !Sub
                    - ${LoadBalancerName}/${TargetGroupName}
                    - LoadBalancerName: !GetAtt LoadBalancer.LoadBalancerFullName
                      TargetGroupName: !GetAtt TargetGroup.TargetGroupFullName
            TargetValue: 1000
            ScaleInCooldown: 300
            ScaleOutCooldown: 60
```

**Scaling Triggers:**

-   **CPU > 70%** for 3 minutes → Scale out (add 1 task)
-   **CPU < 50%** for 5 minutes → Scale in (remove 1 task)
-   **Request count > 1000/task** → Scale out
-   **Min:** 2 tasks, **Max:** 10 tasks

---

### Kubernetes

**Current Configuration:**

-   **Application Pods:** 2 replicas (default in `k8s/taskactivity-deployment.yaml`)
-   **Database Pod:** 1 replica (stateful, should not scale)

#### Manual Scaling

**Via kubectl:**

```bash
# Scale to 5 replicas
kubectl scale deployment taskactivity-app -n taskactivity --replicas=5

# Verify scaling
kubectl get pods -n taskactivity -l app=taskactivity-app
```

**Via YAML:**

Edit `k8s/taskactivity-deployment.yaml`:

```yaml
spec:
    replicas: 5 # Change from 2 to 5
```

Apply the change:

```bash
kubectl apply -f k8s/taskactivity-deployment.yaml
```

#### Horizontal Pod Autoscaler (HPA)

Create `k8s/taskactivity-hpa.yaml`:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
    name: taskactivity-hpa
    namespace: taskactivity
spec:
    scaleTargetRef:
        apiVersion: apps/v1
        kind: Deployment
        name: taskactivity-app
    minReplicas: 2
    maxReplicas: 10
    metrics:
        # CPU-based scaling
        - type: Resource
          resource:
              name: cpu
              target:
                  type: Utilization
                  averageUtilization: 70
        # Memory-based scaling
        - type: Resource
          resource:
              name: memory
              target:
                  type: Utilization
                  averageUtilization: 80
    behavior:
        scaleDown:
            stabilizationWindowSeconds: 300
            policies:
                - type: Percent
                  value: 50
                  periodSeconds: 60
        scaleUp:
            stabilizationWindowSeconds: 60
            policies:
                - type: Percent
                  value: 100
                  periodSeconds: 60
```

Apply the autoscaler:

```bash
kubectl apply -f k8s/taskactivity-hpa.yaml
```

**Monitor HPA:**

```bash
# Watch autoscaler status
kubectl get hpa -n taskactivity -w

# View scaling events
kubectl describe hpa taskactivity-hpa -n taskactivity
```

---

### Docker Compose (Development)

Docker Compose is designed for single-instance development and **does not support horizontal scaling** in the same way as orchestration platforms.

**For Testing Multiple Instances:**

Modify `docker-compose.yml`:

```yaml
services:
    app:
        profiles: ["host-db"]
        build: .
        ports:
            - "8080:8080" # First instance
        # ... environment variables ...

    app-replica:
        profiles: ["host-db"]
        build: .
        ports:
            - "8081:8080" # Second instance on different port
        environment:
            - SPRING_PROFILES_ACTIVE=docker
            - DB_USERNAME=${DB_USERNAME}
            - DB_PASSWORD=${DB_PASSWORD}
            # Same database, different app instances
```

**Note:** This is for testing only. Use Kubernetes or Docker Swarm for production multi-instance deployments.

---

## Load Balancing

### AWS Application Load Balancer (ALB)

**Configuration:**

-   **Target Group:** ECS tasks registered automatically
-   **Health Check:** `GET /actuator/health` every 30 seconds
-   **Healthy Threshold:** 2 consecutive successes
-   **Unhealthy Threshold:** 3 consecutive failures
-   **Timeout:** 5 seconds
-   **Stickiness:** Optional (enabled by default for session continuity)

**Sticky Sessions:**

```yaml
# CloudFormation - TargetGroup
TargetGroupAttributes:
    - Key: stickiness.enabled
      Value: "true"
    - Key: stickiness.type
      Value: "lb_cookie"
    - Key: stickiness.lb_cookie.duration_seconds
      Value: "1800" # 30 minutes
```

**Without Sticky Sessions:**  
Disable sticky sessions and implement Redis session storage (see [Session Management](#session-management)).

### Kubernetes Service Load Balancing

**Configuration:**

```yaml
# k8s/taskactivity-deployment.yaml
apiVersion: v1
kind: Service
metadata:
    name: taskactivity-service
    namespace: taskactivity
spec:
    type: LoadBalancer # Or ClusterIP + Ingress
    selector:
        app: taskactivity-app
    ports:
        - port: 80
          targetPort: 8080
          protocol: TCP
    sessionAffinity: ClientIP # Optional sticky sessions
    sessionAffinityConfig:
        clientIP:
            timeoutSeconds: 1800 # 30 minutes
```

**Ingress Controller (Recommended):**

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
    name: taskactivity-ingress
    namespace: taskactivity
    annotations:
        nginx.ingress.kubernetes.io/affinity: "cookie"
        nginx.ingress.kubernetes.io/session-cookie-name: "taskactivity-session"
        nginx.ingress.kubernetes.io/session-cookie-expires: "1800"
spec:
    rules:
        - host: taskactivity.example.com
          http:
              paths:
                  - path: /
                    pathType: Prefix
                    backend:
                        service:
                            name: taskactivity-service
                            port:
                                number: 80
```

---

## Database Connection Pooling

### HikariCP Configuration

The application uses HikariCP for efficient connection pooling across multiple instances.

**Production Settings:**

```properties
# Connection pool size per instance
spring.datasource.hikari.maximum-pool-size=${DB_POOL_SIZE:20}
spring.datasource.hikari.minimum-idle=${DB_POOL_MIN:5}
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1200000  # 20 minutes
spring.datasource.hikari.leak-detection-threshold=60000
```

### Scaling Considerations

**PostgreSQL Connection Limits:**

```sql
-- Check current connections
SELECT count(*) FROM pg_stat_activity;

-- Check max connections
SHOW max_connections;  -- Default: 100
```

**Calculate Total Connections:**

```
Total Connections = (Number of Instances) × (Maximum Pool Size)
```

**Example:**

-   3 ECS tasks × 20 connections/task = **60 connections**
-   Leave headroom: PostgreSQL `max_connections = 100` ✅

**If Scaling Beyond 5 Instances:**

Increase PostgreSQL max connections:

```sql
-- RDS Parameter Group
max_connections = 200
```

Or reduce per-instance pool size:

```properties
spring.datasource.hikari.maximum-pool-size=10  # 10 instances × 10 = 100
```

---

## Health Checks and Readiness

### Spring Boot Actuator

**Endpoints:**

-   **Liveness:** `/actuator/health/liveness` - Is the app running?
-   **Readiness:** `/actuator/health/readiness` - Is the app ready to serve traffic?
-   **Full Health:** `/actuator/health` - Comprehensive health status

**Configuration:**

```properties
# Production settings (application-aws.properties)
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
management.endpoints.web.exposure.include=health,info,metrics
```

### ECS Health Check

```json
{
    "healthCheck": {
        "command": [
            "CMD-SHELL",
            "curl -f http://localhost:8080/actuator/health || exit 1"
        ],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
    }
}
```

### Kubernetes Health Check

```yaml
livenessProbe:
    httpGet:
        path: /actuator/health/liveness
        port: 8080
    initialDelaySeconds: 60
    periodSeconds: 10
    timeoutSeconds: 5
    failureThreshold: 3

readinessProbe:
    httpGet:
        path: /actuator/health/readiness
        port: 8080
    initialDelaySeconds: 30
    periodSeconds: 5
    timeoutSeconds: 3
    failureThreshold: 3
```

**Behavior:**

-   **Liveness Fails:** Pod/task is restarted
-   **Readiness Fails:** Pod/task removed from load balancer (no new traffic)

---

## Scaling Best Practices

### When to Scale Horizontally

✅ **Scale Out When:**

-   CPU utilization > 70% sustained
-   Memory utilization > 80% sustained
-   Request latency increases (p95 > 500ms)
-   Request rate exceeds 1000 req/min per instance
-   Database connection pool frequently at max

❌ **Don't Scale Out If:**

-   Single instance CPU < 50%
-   Database is the bottleneck (scale database instead)
-   Memory leak detected (fix the leak, don't scale)

### Scaling Recommendations by Environment

| Environment | Min Instances | Max Instances | Notes                          |
| ----------- | ------------- | ------------- | ------------------------------ |
| Development | 1             | 1             | Single instance sufficient     |
| Staging     | 1             | 2             | Test multi-instance behavior   |
| Production  | 2             | 10            | Start with 2, auto-scale to 10 |

### Graceful Shutdown

The application supports graceful shutdown to avoid dropping active requests:

```properties
# Allow 30 seconds for in-flight requests to complete
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

**During Scale-In:**

1. Load balancer stops sending new requests to the instance
2. Instance waits up to 30 seconds for active requests to complete
3. Instance shuts down cleanly

---

## Monitoring and Metrics

### Key Metrics to Monitor

**Application Metrics:**

-   **Request Rate:** Requests per second per instance
-   **Response Time:** p50, p95, p99 latency
-   **Error Rate:** 4xx and 5xx responses
-   **JVM Memory:** Heap usage, GC frequency
-   **Thread Pool:** Active threads (Tomcat)

**Infrastructure Metrics:**

-   **CPU Utilization:** Per instance and aggregate
-   **Memory Utilization:** Per instance
-   **Network I/O:** Bytes in/out
-   **Database Connections:** Active, idle, waiting

### AWS CloudWatch

**Custom Metrics (Optional):**

```java
@Component
public class MetricsPublisher {
    @Autowired
    private MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 60000)
    public void publishMetrics() {
        // Micrometer automatically publishes to CloudWatch
        // via cloudwatch-micrometer dependency
    }
}
```

**CloudWatch Dashboards:**

-   ECS Service: CPU, Memory, Task Count
-   ALB: Request Count, Target Response Time, Healthy Hosts
-   RDS: CPU, Connections, Read/Write IOPS

### Kubernetes Metrics

**Prometheus + Grafana:**

```yaml
# Add annotations to deployment
metadata:
    annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
```

**Spring Boot Actuator with Prometheus:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

---

## Troubleshooting Scaling Issues

### Symptom: New Instances Fail Health Checks

**Possible Causes:**

1. Database connection limit reached
2. Slow application startup
3. Missing environment variables

**Solutions:**

```bash
# Check ECS task logs
aws ecs describe-tasks --cluster taskactivity-cluster --tasks <task-id>

# Check Kubernetes pod logs
kubectl logs -n taskactivity <pod-name>

# Verify database connections
SELECT * FROM pg_stat_activity WHERE datname = 'AmmoP1DB';
```

### Symptom: Uneven Load Distribution

**Possible Causes:**

1. Sticky sessions with uneven client distribution
2. Some instances slower (cold start)
3. Connection draining taking too long

**Solutions:**

-   Disable sticky sessions (implement Redis session store)
-   Increase health check frequency
-   Tune connection draining timeout

### Symptom: Database Connection Pool Exhausted

**Solution:**

```properties
# Reduce per-instance pool size
spring.datasource.hikari.maximum-pool-size=10

# Or increase database max_connections
# RDS: Modify parameter group
```

---

## Quick Reference

### Scale ECS Service (AWS CLI)

```bash
aws ecs update-service \
  --cluster taskactivity-cluster \
  --service taskactivity-service \
  --desired-count 3
```

### Scale Kubernetes Deployment

```bash
kubectl scale deployment taskactivity-app -n taskactivity --replicas=5
```

### Monitor ECS Service

```bash
aws ecs describe-services \
  --cluster taskactivity-cluster \
  --services taskactivity-service
```

### Monitor Kubernetes Pods

```bash
kubectl get pods -n taskactivity -w
kubectl top pods -n taskactivity
```

### Check Database Connections

```sql
SELECT
    application_name,
    client_addr,
    state,
    COUNT(*) as connections
FROM pg_stat_activity
WHERE datname = 'AmmoP1DB'
GROUP BY application_name, client_addr, state;
```

---

## Conclusion

The Task Activity Management application is designed for horizontal scalability following 12-Factor App principles:

✅ **Stateless processes** - No local state, shared database  
✅ **Concurrency via process model** - Scale by adding instances  
✅ **Disposability** - Fast startup, graceful shutdown  
✅ **Dev/Prod parity** - Same architecture across environments  
✅ **Logs as event streams** - stdout/stderr captured by platform

**Production Recommendation:**

-   Start with 2 instances (high availability)
-   Enable auto-scaling based on CPU/request metrics
-   Monitor database connection usage
-   Implement Redis session storage for true stateless operation

For questions or issues, refer to the [Developer Guide](Developer_Guide.md) or [AWS Deployment Guide](../aws/AWS_Deployment.md).
