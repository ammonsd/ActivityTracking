# AWS RDS Database Creation - Complete Documentation

**Created:** October 13, 2025  
**Last Updated:** February 5, 2026  
**Database Status:** ✅ AVAILABLE  
**AWS Account:** 378010131175  
**AWS Region:** us-east-1

---

## 🎯 What Was Created

### 1. **Security Groups**

#### Database Security Group

- **Name:** taskactivity-db-sg
- **Group ID:** sg-08f4bdf0f4619d2e0
- **VPC:** vpc-0532ba98b7d5e4e52 (Default VPC)
- **Purpose:** Protects the RDS database
- **Inbound Rules:**
    - Port 5432 (PostgreSQL) from ECS security group (sg-03812ec4ea45c473c)

#### ECS Tasks Security Group

- **Name:** taskactivity-ecs-sg
- **Group ID:** sg-03812ec4ea45c473c
- **VPC:** vpc-0532ba98b7d5e4e52 (Default VPC)
- **Purpose:** Protects ECS tasks
- **Inbound Rules:**
    - Port 8080 (Application) from anywhere (0.0.0.0/0)

### 2. **RDS PostgreSQL Database**

#### Database Details

- **Identifier:** taskactivity-db
- **Endpoint:** `taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com`
- **Port:** 5432
- **Engine:** PostgreSQL 15.14
- **Database Name:** AmmoP1DB

#### Instance Configuration

- **Instance Class:** db.t3.micro (1 vCPU, 1 GB RAM)
- **Storage:** 20 GB gp3 (3000 IOPS, 125 MB/s throughput)
- **Storage Encryption:** ✅ Enabled (AWS KMS)
- **Multi-AZ:** No (single availability zone for cost savings)
- **Publicly Accessible:** No (secure, internal only)

#### Credentials

- **Master Username:** postgres
- **Master Password:** TaskActivity2025!SecureDB
- **JDBC URL:** jdbc:postgresql://taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com:5432/AmmoP1DB

#### Backup & Maintenance

- **Backup Retention:** 7 days
- **Backup Window:** 03:00-04:00 UTC (11 PM - 12 AM EST)
- **Maintenance Window:** Monday 04:00-05:00 UTC (12 AM - 1 AM EST Monday)
- **Auto Minor Version Upgrade:** Enabled

#### Security & Monitoring

- **VPC Security Group:** sg-08f4bdf0f4619d2e0
- **IAM Database Authentication:** Disabled
- **Performance Insights:** Disabled
- **Enhanced Monitoring:** Disabled (can enable later)
- **Deletion Protection:** Disabled

---

## 📋 AWS Secrets Manager (Updated)

### Database Credentials Secret

- **Name:** taskactivity/database/credentials
- **ARN:** arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/database/credentials-zH7fA0

**Current Values:**

```json
{
    "username": "postgres",
    "password": "TaskActivity2025!SecureDB",
    "jdbcUrl": "jdbc:postgresql://taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com:5432/AmmoP1DB"
}
```

---

## 🔧 How to Recreate This Setup (If Needed)

### Step 1: Create Security Groups

```powershell
# Get default VPC ID
$VPC_ID = aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text

# Create DB security group
aws ec2 create-security-group `
    --group-name taskactivity-db-sg `
    --description "Security group for TaskActivity RDS PostgreSQL database" `
    --vpc-id $VPC_ID

# Create ECS security group
aws ec2 create-security-group `
    --group-name taskactivity-ecs-sg `
    --description "Security group for TaskActivity ECS tasks" `
    --vpc-id $VPC_ID

# Allow ECS to connect to RDS (replace with actual group IDs)
aws ec2 authorize-security-group-ingress `
    --group-id sg-08f4bdf0f4619d2e0 `
    --protocol tcp `
    --port 5432 `
    --source-group sg-03812ec4ea45c473c

# Allow internet to connect to ECS tasks
aws ec2 authorize-security-group-ingress `
    --group-id sg-03812ec4ea45c473c `
    --protocol tcp `
    --port 8080 `
    --cidr 0.0.0.0/0
```

### Step 2: Create RDS Database

```powershell
aws rds create-db-instance `
    --db-instance-identifier taskactivity-db `
    --db-instance-class db.t3.micro `
    --engine postgres `
    --engine-version 15.14 `
    --master-username postgres `
    --master-user-password "TaskActivity2025!SecureDB" `
    --allocated-storage 20 `
    --storage-type gp3 `
    --storage-encrypted `
    --db-name AmmoP1DB `
    --vpc-security-group-ids sg-08f4bdf0f4619d2e0 `
    --backup-retention-period 7 `
    --preferred-backup-window "03:00-04:00" `
    --preferred-maintenance-window "mon:04:00-mon:05:00" `
    --no-publicly-accessible `
    --region us-east-1 `
    --tags Key=Application,Value=TaskActivity Key=Environment,Value=Production
```

### Step 3: Wait for Database to be Available

```powershell
aws rds wait db-instance-available --db-instance-identifier taskactivity-db
```

### Step 4: Get Database Endpoint

```powershell
$DB_ENDPOINT = aws rds describe-db-instances `
    --db-instance-identifier taskactivity-db `
    --query "DBInstances[0].Endpoint.Address" `
    --output text
```

### Step 5: Update Secrets Manager

```powershell
aws secretsmanager update-secret `
    --secret-id taskactivity/database/credentials `
    --secret-string "{`"username`":`"postgres`",`"password`":`"TaskActivity2025!SecureDB`",`"jdbcUrl`":`"jdbc:postgresql://$DB_ENDPOINT:5432/AmmoP1DB`"}" `
    --region us-east-1
```

---

## 🔍 Useful Management Commands

### Check Database Status

```powershell
aws rds describe-db-instances --db-instance-identifier taskactivity-db
```

### Get Database Endpoint

```powershell
aws rds describe-db-instances `
    --db-instance-identifier taskactivity-db `
    --query "DBInstances[0].Endpoint.Address" `
    --output text
```

### View Database Configuration

```powershell
aws rds describe-db-instances `
    --db-instance-identifier taskactivity-db `
    --query "DBInstances[0].[DBInstanceStatus,Engine,EngineVersion,DBInstanceClass,AllocatedStorage,StorageEncrypted]" `
    --output table
```

### Modify Database (if needed)

```powershell
# Example: Change instance class
aws rds modify-db-instance `
    --db-instance-identifier taskactivity-db `
    --db-instance-class db.t3.small `
    --apply-immediately
```

### Create Manual Snapshot

```powershell
aws rds create-db-snapshot `
    --db-instance-identifier taskactivity-db `
    --db-snapshot-identifier taskactivity-db-snapshot-$(Get-Date -Format "yyyyMMdd-HHmmss")
```

### List Snapshots

```powershell
aws rds describe-db-snapshots `
    --db-instance-identifier taskactivity-db `
    --query "DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime,Status]" `
    --output table
```

### Stop Database (to save costs when not in use)

```powershell
aws rds stop-db-instance --db-instance-identifier taskactivity-db
```

### Start Database

```powershell
aws rds start-db-instance --db-instance-identifier taskactivity-db
```

### Delete Database (WARNING: This is permanent!)

```powershell
# Create final snapshot before deletion
aws rds delete-db-instance `
    --db-instance-identifier taskactivity-db `
    --final-db-snapshot-identifier taskactivity-db-final-snapshot `
    --skip-final-snapshot  # Remove this line to create final snapshot
```

---

## 🔐 Security Best Practices

### Current Security Configuration ✅

- ✅ Storage encryption enabled
- ✅ Not publicly accessible
- ✅ Security group restricts access to ECS tasks only
- ✅ Strong password used
- ✅ Automatic backups enabled
- ✅ Credentials stored in AWS Secrets Manager

### Additional Security Recommendations

- 🔒 Enable IAM database authentication for passwordless access
- 🔒 Enable Performance Insights for query monitoring
- 🔒 Enable Enhanced Monitoring for detailed metrics
- 🔒 Rotate database password regularly
- 🔒 Enable deletion protection for production
- 🔒 Consider Multi-AZ deployment for high availability

### Enable Deletion Protection (Recommended for Production)

```powershell
aws rds modify-db-instance `
    --db-instance-identifier taskactivity-db `
    --deletion-protection `
    --apply-immediately
```

---

## 💰 Cost Information

### Current Configuration Cost Estimate (Monthly)

- **db.t3.micro instance:** ~$13-15/month
- **20 GB gp3 storage:** ~$2.30/month
- **Backup storage (7 days):** ~$1-2/month (first 20GB free)
- **Data transfer:** Variable (usually minimal)

**Total Estimated:** ~$16-20/month

### Cost Optimization Tips

1. **Stop database when not in use** (saves ~60% of instance costs)
2. **Use Reserved Instances** for production (save up to 40%)
3. **Adjust backup retention** if 7 days is too much
4. **Monitor and adjust storage** as needed
5. **Use RDS Proxy** for connection pooling (reduces instance load)

---

## 🚀 Deploying Your Application

Now that the database is ready, you can deploy your application:

```powershell
cd C:\Users\deana\GitHub\ActivityTracking
.\aws\deploy-aws.ps1 -Environment production
```

The deployment script will:

1. Build your Spring Boot application
2. Create Docker image
3. Push image to ECR
4. Register ECS task definition with updated security group
5. Create/update ECS service
6. Your application will automatically connect to the database using Secrets Manager

### Update Task Definition with ECS Security Group

Before deploying, you need to update your task definition or deployment script to use the ECS security group:

- **ECS Security Group ID:** sg-03812ec4ea45c473c

This should be specified when creating the ECS service.

---

## 📊 Monitoring & Troubleshooting

### View RDS Logs

```powershell
# List available log files
aws rds describe-db-log-files --db-instance-identifier taskactivity-db

# Download a log file
aws rds download-db-log-file-portion `
    --db-instance-identifier taskactivity-db `
    --log-file-name error/postgresql.log.2025-10-13-21 `
    --output text
```

### CloudWatch Metrics

View in AWS Console: CloudWatch → Metrics → RDS → Per-Database Metrics

Key metrics to monitor:

- CPU Utilization
- Database Connections
- Free Storage Space
- Read/Write IOPS
- Network Throughput

### Connection Test from Local Machine

```powershell
# Note: This won't work directly because database is not publicly accessible
# You would need a bastion host or VPN to connect from outside AWS

# From within AWS (EC2 or ECS):
psql -h taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com -U postgres -d AmmoP1DB
```

---

## 📝 Important Information Summary

### Critical Information to Save

**Database Endpoint:**

```
taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com:5432
```

**Database Credentials:**

- Username: postgres
- Password: TaskActivity2025!SecureDB
- Database: AmmoP1DB

**Security Groups:**

- RDS: sg-08f4bdf0f4619d2e0
- ECS: sg-03812ec4ea45c473c

**VPC:**

- vpc-0532ba98b7d5e4e52 (Default VPC)

**Secrets Manager:**

- Database credentials: arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/database/credentials-zH7fA0
- Admin credentials: arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/admin/credentials-4nNh6X

---

## 🎓 What You Learned

This setup demonstrates:

1. **VPC Networking** - Using default VPC with multiple availability zones
2. **Security Groups** - Controlling network access between services
3. **RDS Management** - Creating and configuring PostgreSQL databases
4. **Secrets Management** - Storing credentials securely in AWS Secrets Manager
5. **High Availability** - Automated backups and point-in-time recovery
6. **Cost Optimization** - Right-sizing resources and using free tier when possible

---

## 📚 Additional Resources

- [Amazon RDS User Guide](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/)
- [PostgreSQL on RDS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html)
- [RDS Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)

---

**Remember:** This documentation is your reference guide. Keep it safe! 🔖
