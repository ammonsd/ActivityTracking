# Kubernetes Deployment Guide

Complete guides for deploying the Task Activity Management application to Kubernetes.

---

## 🚀 Deployment Options

### ⭐ AWS EKS (Production - Recommended)

**Complete Guide:** [AWS_EKS_Setup_Guide.md](AWS_EKS_Setup_Guide.md)

For AWS production deployments with:
- AWS Secrets Manager integration
- S3 receipt storage
- SES email notifications
- RDS database
- ALB ingress with SSL/TLS
- IAM Roles for Service Accounts (IRSA)

**Files:**
- `taskactivity-deployment-aws.yaml` - AWS-specific deployment
- `taskactivity-ingress-aws.yaml` - ALB ingress configuration
- `taskactivity-rbac.yaml` - RBAC with IRSA

**Quick Deploy:**
```bash
kubectl apply -f taskactivity-rbac.yaml
kubectl apply -f taskactivity-deployment-aws.yaml
kubectl apply -f taskactivity-ingress-aws.yaml
```

---

### 🏠 Generic Kubernetes (Local/Development)

For local development or non-AWS cloud providers.

**Files:**
- `taskactivity-deployment.yaml` - Generic deployment with PostgreSQL pod
- `taskactivity-rbac.yaml` - Basic RBAC

**Quick Deploy:**
```bash
kubectl apply -f taskactivity-rbac.yaml
kubectl apply -f taskactivity-deployment.yaml
```

**Features:**
- Self-contained PostgreSQL database
- Basic Kubernetes Secrets
- NGINX Ingress support
- Works with Minikube, kind, Docker Desktop

---

## 📁 Files in This Directory

| File | Purpose | Use Case |
|------|---------|----------|
| **AWS_EKS_Setup_Guide.md** | Complete AWS EKS setup guide | AWS production deployment |
| **taskactivity-deployment-aws.yaml** | AWS-specific deployment manifest | AWS EKS with S3, SES, RDS, Secrets Manager |
| **taskactivity-ingress-aws.yaml** | AWS ALB ingress configuration | AWS EKS with Application Load Balancer |
| **taskactivity-deployment.yaml** | Generic Kubernetes deployment | Local/development, non-AWS clouds |
| **taskactivity-rbac.yaml** | RBAC configuration | Both AWS and generic deployments |
| **ReadMe.md** | This file | Navigation and overview |

---

## 🔗 Related Documentation

- [Developer Guide](../docs/Developer_Guide.md) - Application development guide
- [Docker Build Guide](../docs/Docker_Build_Guide.md) - Container build instructions
- [AWS Deployment Guide](../aws/AWS_Deployment.md) - AWS ECS deployment (alternative to EKS)
- [CloudFormation Guide](../cloudformation/README.md) - Infrastructure as Code

---

## 📞 Support

For issues or questions:
- Review the appropriate setup guide
- Check application logs: `kubectl logs -n taskactivity -l app=taskactivity-app`
- Contact: deanammons@gmail.com

---

**Last Updated:** January 27, 2026
