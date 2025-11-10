# Kubernetes Deployment Guide

This guide provides instructions for deploying the Task Activity Management application to a Kubernetes cluster.

## Overview

The Kubernetes deployment includes:

-   ✅ **Complete application stack** (PostgreSQL + Spring Boot app)
-   🔐 **Secrets management** for database credentials
-   ⚙️ **ConfigMaps** for non-sensitive configuration
-   🛡️ **RBAC** (Role-Based Access Control) with minimal permissions
-   📊 **Health probes** (liveness and readiness)
-   💾 **Persistent storage** for PostgreSQL
-   🌐 **Ingress** for external access
-   📈 **Resource limits** for predictable performance
-   🔄 **High availability** with 2 app replicas

---

## Prerequisites

### Required

-   Kubernetes cluster (1.21+)
    -   Local: Minikube, kind, Docker Desktop, or k3s
    -   Cloud: EKS, GKE, AKS, or any managed Kubernetes
-   `kubectl` CLI tool installed and configured
-   Docker image built and available
    -   Build locally: `docker build -t taskactivity:v1.0.0 .`
    -   Or push to registry: Docker Hub, ECR, GCR, etc.

### Optional (for Ingress)

-   Ingress controller (nginx, Traefik, etc.)
-   cert-manager (for automatic TLS certificates)
-   DNS configured to point to your cluster

---

## Quick Start

### 1. Update Configuration

Edit the manifests to match your environment:

**Update database credentials** in `taskactivity-deployment.yaml`:

```bash
# Generate base64-encoded values
echo -n 'your_username' | base64
echo -n 'your_password' | base64
```

Replace the values in the Secret:

```yaml
data:
    username: <base64_username>
    password: <base64_password>
```

**Update image name** if using a registry:

```yaml
image: your-registry/taskactivity:v1.0.0
```

**Update domain** in Ingress (if using):

```yaml
host: taskactivity.yourdomain.com
```

### 2. Deploy to Kubernetes

```bash
# Apply RBAC configuration first
kubectl apply -f taskactivity-rbac.yaml

# Deploy the application stack
kubectl apply -f taskactivity-deployment.yaml

# Verify deployment
kubectl get all -n taskactivity
```

### 3. Check Status

```bash
# Check pods
kubectl get pods -n taskactivity

# Check services
kubectl get services -n taskactivity

# Check logs
kubectl logs -n taskactivity -l app=taskactivity-app --tail=50 -f
```

### 4. Access the Application

**Port Forward (for testing):**

```bash
kubectl port-forward -n taskactivity service/taskactivity-service 8080:8080
```

Access at: http://localhost:8080

**Via Ingress (if configured):**
Access at: https://taskactivity.yourdomain.com

---

## Architecture

### Components

```
┌─────────────────────────────────────────────────┐
│                   Ingress                        │
│            (taskactivity-ingress)               │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│              LoadBalancer Service                │
│           (taskactivity-service)                │
└─────────────────┬───────────────────────────────┘
                  │
      ┌───────────┴───────────┐
      ▼                       ▼
┌─────────────┐         ┌─────────────┐
│   Pod 1     │         │   Pod 2     │
│ TaskActivity│         │ TaskActivity│
│   (8080)    │         │   (8080)    │
└──────┬──────┘         └──────┬──────┘
       │                       │
       └───────────┬───────────┘
                   ▼
         ┌─────────────────┐
         │  PostgreSQL     │
         │   Service       │
         │ (postgres-svc)  │
         └────────┬────────┘
                  ▼
         ┌─────────────────┐
         │  PostgreSQL     │
         │     Pod         │
         │   (5432)        │
         └────────┬────────┘
                  ▼
         ┌─────────────────┐
         │ Persistent      │
         │ Volume          │
         │ (5Gi)           │
         └─────────────────┘
```

### Namespacing

All resources are deployed in the `taskactivity` namespace for isolation.

### Security

-   **RBAC**: ServiceAccount with minimal permissions (get/list pods, configmaps, secrets)
-   **Secrets**: Database credentials stored as Kubernetes Secrets
-   **ConfigMaps**: Non-sensitive configuration separated from code
-   **Resource Limits**: CPU and memory limits to prevent resource exhaustion
-   **No Token Mounting**: `automountServiceAccountToken: false` unless needed

---

## Configuration Details

### Secrets Management

Database credentials are stored in a Kubernetes Secret:

```yaml
apiVersion: v1
kind: Secret
metadata:
    name: taskactivity-db-credentials
    namespace: taskactivity
type: Opaque
data:
    username: <base64-encoded>
    password: <base64-encoded>
```

**Best Practice**: Use external secrets management:

-   **AWS**: AWS Secrets Manager + External Secrets Operator
-   **Azure**: Azure Key Vault + Secrets Store CSI Driver
-   **GCP**: Secret Manager + External Secrets Operator
-   **HashiCorp**: Vault + Vault Agent Injector

### ConfigMap

Non-sensitive configuration:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
    name: taskactivity-config
    namespace: taskactivity
data:
    SPRING_PROFILES_ACTIVE: "docker"
    SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres-service:5432/AmmoP1DB"
    JAVA_OPTS: "-Xmx512m -Xms256m"
```

### Resource Limits

**Application Pods:**

-   Requests: 256m CPU, 512Mi memory
-   Limits: 500m CPU, 1Gi memory

**PostgreSQL Pod:**

-   Requests: 250m CPU, 512Mi memory
-   Limits: 500m CPU, 1Gi memory

**Storage:**

-   PostgreSQL: 5Gi persistent volume

### Health Probes

**Liveness Probe:**

-   Endpoint: `/actuator/health`
-   Initial delay: 60 seconds
-   Period: 30 seconds

**Readiness Probe:**

-   Endpoint: `/actuator/health`
-   Initial delay: 30 seconds
-   Period: 10 seconds

---

## Deployment Scenarios

### Local Development (Minikube)

```bash
# Start Minikube
minikube start

# Build image in Minikube's Docker
eval $(minikube docker-env)
docker build -t taskactivity:v1.0.0 .

# Deploy
kubectl apply -f taskactivity-rbac.yaml
kubectl apply -f taskactivity-deployment.yaml

# Access via port-forward
kubectl port-forward -n taskactivity service/taskactivity-service 8080:8080
```

### AWS EKS

```bash
# Build and push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com
docker build -t taskactivity:v1.0.0 .
docker tag taskactivity:v1.0.0 <account>.dkr.ecr.us-east-1.amazonaws.com/taskactivity:v1.0.0
docker push <account>.dkr.ecr.us-east-1.amazonaws.com/taskactivity:v1.0.0

# Update image in deployment YAML
# Deploy
kubectl apply -f taskactivity-rbac.yaml
kubectl apply -f taskactivity-deployment.yaml

# Setup ALB Ingress Controller (if using)
# Configure Ingress with ALB annotations
```

### Google GKE

```bash
# Build and push to GCR
gcloud auth configure-docker
docker build -t taskactivity:v1.0.0 .
docker tag taskactivity:v1.0.0 gcr.io/<project-id>/taskactivity:v1.0.0
docker push gcr.io/<project-id>/taskactivity:v1.0.0

# Update image in deployment YAML
# Deploy
kubectl apply -f taskactivity-rbac.yaml
kubectl apply -f taskactivity-deployment.yaml

# GKE automatically provisions LoadBalancer
```

### Azure AKS

```bash
# Build and push to ACR
az acr login --name <registry-name>
docker build -t taskactivity:v1.0.0 .
docker tag taskactivity:v1.0.0 <registry-name>.azurecr.io/taskactivity:v1.0.0
docker push <registry-name>.azurecr.io/taskactivity:v1.0.0

# Update image in deployment YAML
# Deploy
kubectl apply -f taskactivity-rbac.yaml
kubectl apply -f taskactivity-deployment.yaml
```

---

## Ingress Configuration

### NGINX Ingress Controller

Install NGINX Ingress:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
```

The Ingress manifest includes NGINX-specific annotations:

```yaml
annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
```

### TLS/SSL with cert-manager

Install cert-manager:

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
```

Create ClusterIssuer:

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
    name: letsencrypt-prod
spec:
    acme:
        server: https://acme-v02.api.letsencrypt.org/directory
        email: your-email@example.com
        privateKeySecretRef:
            name: letsencrypt-prod
        solvers:
            - http01:
                  ingress:
                      class: nginx
```

cert-manager will automatically provision TLS certificates.

---

## Monitoring and Logging

### View Logs

```bash
# All app logs
kubectl logs -n taskactivity -l app=taskactivity-app --tail=100 -f

# Specific pod
kubectl logs -n taskactivity <pod-name> --tail=100 -f

# Database logs
kubectl logs -n taskactivity -l app=postgres --tail=100 -f
```

### Check Events

```bash
kubectl get events -n taskactivity --sort-by='.lastTimestamp'
```

### Describe Resources

```bash
kubectl describe pod -n taskactivity <pod-name>
kubectl describe service -n taskactivity taskactivity-service
kubectl describe ingress -n taskactivity taskactivity-ingress
```

### Resource Usage

```bash
kubectl top pods -n taskactivity
kubectl top nodes
```

---

## Scaling

### Manual Scaling

```bash
# Scale application pods
kubectl scale deployment taskactivity-app -n taskactivity --replicas=5

# Verify
kubectl get pods -n taskactivity -l app=taskactivity-app
```

### Horizontal Pod Autoscaler (HPA)

Create HPA based on CPU:

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
        - type: Resource
          resource:
              name: cpu
              target:
                  type: Utilization
                  averageUtilization: 70
```

Apply:

```bash
kubectl apply -f taskactivity-hpa.yaml
kubectl get hpa -n taskactivity
```

---

## Backup and Restore

### Database Backup

```bash
# Execute pg_dump in PostgreSQL pod
kubectl exec -n taskactivity <postgres-pod-name> -- pg_dump -U postgres AmmoP1DB > backup.sql

# Or with credentials from secret
kubectl exec -n taskactivity <postgres-pod-name> -- sh -c 'pg_dump -U $POSTGRES_USER $POSTGRES_DB' > backup.sql
```

### Database Restore

```bash
# Copy backup to pod
kubectl cp backup.sql taskactivity/<postgres-pod-name>:/tmp/backup.sql

# Restore
kubectl exec -n taskactivity <postgres-pod-name> -- psql -U postgres AmmoP1DB < /tmp/backup.sql
```

### Persistent Volume Backup

Use volume snapshots (if supported by your storage class):

```bash
kubectl get volumesnapshot -n taskactivity
```

---

## Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl get pods -n taskactivity

# Describe pod for events
kubectl describe pod -n taskactivity <pod-name>

# Check logs
kubectl logs -n taskactivity <pod-name>
```

**Common issues:**

-   Image pull errors: Check image name and registry access
-   CrashLoopBackOff: Check application logs for startup errors
-   Database connection: Verify database pod is running and service is accessible

### Database Connection Issues

```bash
# Test database connectivity from app pod
kubectl exec -n taskactivity <app-pod-name> -- nc -zv postgres-service 5432

# Check database logs
kubectl logs -n taskactivity <postgres-pod-name>

# Verify service
kubectl get service postgres-service -n taskactivity
```

### Ingress Not Working

```bash
# Check Ingress status
kubectl get ingress -n taskactivity
kubectl describe ingress taskactivity-ingress -n taskactivity

# Check Ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx

# Verify service is accessible
kubectl port-forward -n taskactivity service/taskactivity-service 8080:8080
```

### Resource Issues

```bash
# Check resource usage
kubectl top pods -n taskactivity
kubectl top nodes

# Check if pods are being evicted
kubectl get events -n taskactivity | grep Evicted
```

---

## Updates and Rollbacks

### Update Application

```bash
# Update image
kubectl set image deployment/taskactivity-app -n taskactivity taskactivity=taskactivity:v1.1.0

# Or edit deployment
kubectl edit deployment taskactivity-app -n taskactivity

# Check rollout status
kubectl rollout status deployment/taskactivity-app -n taskactivity
```

### Rollback

```bash
# View rollout history
kubectl rollout history deployment/taskactivity-app -n taskactivity

# Rollback to previous version
kubectl rollout undo deployment/taskactivity-app -n taskactivity

# Rollback to specific revision
kubectl rollout undo deployment/taskactivity-app -n taskactivity --to-revision=2
```

---

## Cleanup

### Remove Application

```bash
# Delete all resources in namespace
kubectl delete namespace taskactivity

# Or delete specific resources
kubectl delete -f taskactivity-deployment.yaml
kubectl delete -f taskactivity-rbac.yaml
```

### Delete Persistent Data

```bash
# List PVCs
kubectl get pvc -n taskactivity

# Delete PVC (this deletes data!)
kubectl delete pvc postgres-pvc -n taskactivity
```

---

## Production Considerations

### Security Hardening

-   [ ] Use external secrets management (AWS Secrets Manager, Vault, etc.)
-   [ ] Enable Pod Security Standards
-   [ ] Use Network Policies to restrict pod-to-pod communication
-   [ ] Enable audit logging
-   [ ] Use private container registry
-   [ ] Scan images for vulnerabilities
-   [ ] Implement least-privilege RBAC
-   [ ] Use read-only root filesystem where possible

### High Availability

-   [ ] Run multiple replicas (current: 2)
-   [ ] Use Pod Disruption Budgets
-   [ ] Spread pods across availability zones
-   [ ] Use StatefulSet for PostgreSQL with replication
-   [ ] Consider managed database service (RDS, Cloud SQL, etc.)
-   [ ] Configure HPA for auto-scaling
-   [ ] Set up proper monitoring and alerting

### Performance

-   [ ] Tune JVM settings (JAVA_OPTS)
-   [ ] Configure connection pooling
-   [ ] Set appropriate resource limits
-   [ ] Use persistent volumes with good IOPS
-   [ ] Enable caching where appropriate
-   [ ] Monitor and tune database performance

### Observability

-   [ ] Integrate with Prometheus for metrics
-   [ ] Set up Grafana dashboards
-   [ ] Configure log aggregation (ELK, Loki, CloudWatch)
-   [ ] Enable distributed tracing (Jaeger, Zipkin)
-   [ ] Set up alerts for critical metrics
-   [ ] Monitor health endpoints

---

## Cost Optimization

-   Use node affinity to place pods efficiently
-   Configure cluster autoscaling
-   Use spot/preemptible instances where appropriate
-   Right-size resource requests and limits
-   Use horizontal pod autoscaling
-   Consider managed services for database

---

## Additional Resources

### Files in This Directory

-   `taskactivity-deployment.yaml` - Complete deployment manifest
-   `taskactivity-rbac.yaml` - RBAC configuration

### Related Documentation

-   [Developer Guide](../docs/Developer_Guide.md)
-   [Docker Build Guide](../docs/Docker_Build_Guide.md)
-   [AWS Deployment Guide](../aws/AWS_Deployment.md)
-   [CloudFormation Guide](../cloudformation/README.md)

### External Resources

-   [Kubernetes Documentation](https://kubernetes.io/docs/)
-   [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
-   [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)

---

**Last Updated:** November 10, 2025
