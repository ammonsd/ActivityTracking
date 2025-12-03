# AWS SES (Simple Email Service) Setup Guide

## Overview

This guide walks through setting up AWS Simple Email Service (SES) for the Task Activity & Expense Management System. SES provides reliable, cost-effective email delivery for application notifications.

## Table of Contents

1. [Why Use AWS SES](#why-use-aws-ses)
2. [Prerequisites](#prerequisites)
3. [Initial SES Setup](#initial-ses-setup)
4. [SMTP Credentials Method](#smtp-credentials-method)
5. [AWS SDK Method (Recommended)](#aws-sdk-method-recommended)
6. [Testing Email Delivery](#testing-email-delivery)
7. [Production Considerations](#production-considerations)
8. [Troubleshooting](#troubleshooting)

---

## Why Use AWS SES

**Benefits:**

-   ✅ **Cost-effective**: First 62,000 emails/month FREE when sending from ECS
-   ✅ **Reliable**: 99.9% uptime SLA
-   ✅ **Scalable**: Handles any volume
-   ✅ **Integrated**: Works seamlessly with ECS IAM roles
-   ✅ **Compliant**: Built-in reputation monitoring and bounce handling
-   ✅ **Secure**: No SMTP credentials needed when using IAM roles

**Current Application Use Cases:**

-   Account lockout notifications to administrators
-   Password reset emails (future feature)
-   Expense approval notifications (future feature)
-   System alerts and monitoring

---

## Prerequisites

-   AWS Account with appropriate permissions
-   Access to AWS Console
-   Domain name (optional, but recommended for production)
-   Application deployed on AWS ECS (for IAM role method)

---

## Initial SES Setup

### Step 1: Access AWS SES Console

**Method 1: Search Bar (Easiest)**

1. Log into AWS Console
2. In the search bar at the top, type **"SES"** or **"Simple Email Service"**
3. Click on **Simple Email Service** from the search results
4. Select your preferred region from the dropdown in the top-right (e.g., `us-east-1`)

**Method 2: Services Menu**

1. Log into AWS Console
2. Click **Services** (top-left menu)
3. Under **"All services"**, scroll to **"Application Integration"** section
4. Click **Simple Email Service**
5. Select your preferred region (e.g., `us-east-1`)

**Method 3: Direct URL**

Navigate directly to: `https://console.aws.amazon.com/ses/home?region=us-east-1`

> **Note**: Choose the same region as your ECS cluster for lowest latency. AWS has reorganized the console, so SES is now under "Application Integration" rather than "Customer Engagement".

### Step 2: Verify Your Email Identity

AWS SES now uses a streamlined "Get set up" wizard. Here's how to navigate it:

**Option A: Using the Get Set Up Wizard (New Interface)**

1. In the SES Console home page, you should see **"Get set up"** section
2. Click **"Add sending domain"** or **"Add verified email identity"**
3. Choose which you want to verify first:

**For Testing (Single Email Address):**

1. Click **"Add verified email identity"** (or similar option)
2. Enter your email address: `noreply@taskactivitytracker.com`
3. Click **"Verify email address"** or **"Create identity"**
4. AWS will send a verification email to that address
5. Check your inbox and click the verification link
6. Return to SES console to confirm status shows **"Verified"**

**For Production (Domain - Recommended):**

1. Click **"Add sending domain"**
2. **Sending Domain**: Enter your domain: `taskactivitytracker.com` (without http:// or www)
3. **MAIL FROM domain** (Custom MAIL FROM):
    - Enter a subdomain like: `noreply` (will become `noreply.taskactivitytracker.com`)
    - Or leave blank to use default Amazon SES domain
    - **Recommended**: Use `mail` or `noreply` for better deliverability
4. **Behavior on MX failure**:
    - Select **"Use default MAIL FROM domain"** (Recommended)
    - This ensures emails are sent even if your custom MAIL FROM domain has DNS issues
    - Alternative: "Reject message" (only use if you require strict MAIL FROM control)
5. Click **"Next"** to proceed to account-level settings
6. **Virtual Deliverability Manager (VDM)**:
    - **Recommendation**: Leave **disabled** (adds additional costs: $0.40 per 10,000 emails)
    - This is an optional paid feature that provides deliverability insights
    - **Note**: When disabled, the "Track opens and clicks" and "Optimized shared delivery" options will disappear (they're VDM features)
    - You can enable VDM later if needed
7. Click **"Next"** to continue
8. **Create your Dedicated IP pool**:
    - **Recommendation**: Leave **disabled** (default)
    - Dedicated IPs cost $24.95/month per IP and are only needed for high-volume senders (100K+ emails/day)
    - AWS's shared IP pool is perfectly fine for most applications
    - You can request a dedicated IP later if needed
9. Click **"Next"** to continue
10. **Add tenant management**:
    - **Recommendation**: Leave **disabled** or skip (not needed for single-tenant applications)
    - Tenant management is for multi-tenant SaaS applications that need to isolate email sending per customer
    - Your application doesn't need this feature
11. Click **"Next"** to continue to DNS configuration
12. **Review your settings** page will appear
    - Review all your selections
    - Domain, MAIL FROM settings, DKIM enabled, etc.
13. Click **"Get Started"** to create the identity and generate DNS records
14. You'll arrive at the **"Get Started"** dashboard page
    - This page shows cards with steps to complete SES setup
    - You should see cards like: "Verify email address", "Verify sending domain", "Request production access"
15. **Next steps**:
    - Click on **"Verify sending domain"** card (Task #3 on Get Started page)
    - You'll see "Add DNS records to your DNS provider" with **"Get DNS Records"** button
    - Click **"Get DNS Records"** button
    - This will open a panel showing all the DNS records you need to add
16. **View and Copy DNS Records**:
    - After clicking "Get DNS Records", you'll see 5-6 records:
        - **Domain verification** (1 CNAME record for `_amazonses`)
        - **DKIM authentication** (3 CNAME records for `_domainkey`)
        - **Custom MAIL FROM** (1 MX record and 1 TXT record for your `noreply` subdomain)
    - **Copy each record** - you'll add these to your DNS provider next
17. **Add DNS Records to Your Domain Provider**:
    - Go to your domain registrar (GoDaddy, Namecheap, Cloudflare, etc.)
    - Navigate to DNS management for your domain
    - Add all the CNAME, MX, and TXT records shown by AWS
    - Save changes
18. **Wait for Verification**:
    - DNS propagation typically takes 10-30 minutes
    - Can take up to 72 hours in rare cases
    - Return to SES Console → Verified identities to check status
    - Status will change from "Pending verification" to "Verified" (green checkmark)

> **Tip**: You can use `nslookup` or online DNS checkers to verify your records are published before AWS checks them.

**Option B: Using Verified Identities Menu (Alternative)**

If you don't see the "Get set up" wizard:

1. In the left sidebar, click **"Verified identities"** (under Configuration)
2. Click **"Create identity"** button (orange button, top-right)
3. Select identity type:
    - **Email address** - For testing (verify single email)
    - **Domain** - For production (verify entire domain)
4. Enter your email or domain
5. For domains: Enable **"Easy DKIM"** and **"DKIM signatures"**
6. Click **"Create identity"**

**DNS Records for Domain Verification:**

After creating a domain identity, AWS will show you DNS records like:

```
Type: CNAME
Name: _amazonses.taskactivitytracker.com
Value: [unique-verification-token].amazonses.com

Type: CNAME (DKIM 1)
Name: abc123._domainkey.taskactivitytracker.com
Value: abc123.dkim.amazonses.com

Type: CNAME (DKIM 2)
Name: def456._domainkey.taskactivitytracker.com
Value: def456.dkim.amazonses.com

Type: CNAME (DKIM 3)
Name: ghi789._domainkey.taskactivitytracker.com
Value: ghi789.dkim.amazonses.com
```

**Important**: Copy these records and add them to your DNS provider (GoDaddy, Cloudflare, Route53, etc.)

> **Note**: Verification can take a few minutes to 72 hours depending on DNS propagation. Usually completes within 10-30 minutes.

### Step 3: Request Production Access (Remove Sandbox Restrictions)

**SES Sandbox Limitations:**

-   ❌ Can only send to verified email addresses
-   ❌ Limited to 200 emails per 24 hours
-   ❌ Maximum 1 email per second

**To Request Production Access:**

1. In SES Console, go to **Account dashboard**
2. Look for the banner: "Your account is in the Amazon SES sandbox"
3. Click **Request production access**
4. Fill out the request form:

    **Mail type**: Transactional

    **Website URL**: https://taskactivitytracker.com

    **Use case description** (example):

    ```
    This application sends automated transactional notifications to users:
    - Account security alerts (lockout notifications)
    - Password reset confirmations
    - System notifications

    All emails are triggered by user actions or system events. We do not
    send marketing emails. Expected volume: 500-1000 emails per month.

    We have processes in place to handle bounces and complaints:
    - Monitor SES metrics and reputation dashboard
    - Process bounce notifications
    - Maintain suppression lists
    ```

    **Compliance acknowledgment**: Check all boxes

5. Click **Submit request**
6. Wait for approval (typically 24-48 hours)
7. You'll receive email notification when approved

---

## SMTP Credentials Method

### Overview

Use SMTP when:

-   Testing locally on your development machine
-   Simple integration needed
-   Using standard Spring Boot mail configuration

### Step 1: Generate SMTP Credentials

1. In SES Console, go to **SMTP settings**
2. Note the **SMTP endpoint** for your region:
    - `us-east-1`: `email-smtp.us-east-1.amazonaws.com`
    - `us-west-2`: `email-smtp.us-west-2.amazonaws.com`
    - `eu-west-1`: `email-smtp.eu-west-1.amazonaws.com`
3. Click **Create SMTP credentials**
4. Enter IAM user name: `ses-smtp-user-taskactivity`
5. Click **Create user**
6. **IMPORTANT**: Copy the credentials immediately (shown only once):
    - **SMTP Username**: `AKIAIOSFODNN7EXAMPLE` (example)
    - **SMTP Password**: `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` (example)
7. Click **Download credentials** (saves as CSV file)
8. Store credentials securely (AWS Secrets Manager recommended)

### Step 2: Configure Application (Local Development)

**Edit `application.properties`** (or create `application-local.properties`):

```properties
# Enable email notifications
spring.mail.enabled=true

# SMTP Configuration for AWS SES
spring.mail.host=email-smtp.us-east-1.amazonaws.com
spring.mail.port=587
spring.mail.username=AKIAIOSFODNN7EXAMPLE
spring.mail.password=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# SMTP Properties
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Application Email Settings
app.mail.from=noreply@taskactivitytracker.com
app.mail.admin-email=admin@yourcompany.com
```

**Using Environment Variables** (recommended):

```bash
export MAIL_ENABLED=true
export MAIL_HOST=email-smtp.us-east-1.amazonaws.com
export MAIL_PORT=587
export MAIL_USERNAME=AKIAIOSFODNN7EXAMPLE
export MAIL_PASSWORD=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
export MAIL_FROM=noreply@taskactivitytracker.com
export ADMIN_EMAIL=admin@yourcompany.com
```

### Step 3: Configure for AWS ECS Deployment

**Option A: Environment Variables in Task Definition**

Update `aws/taskactivity-task-definition.json`:

```json
{
    "containerDefinitions": [
        {
            "environment": [
                {
                    "name": "MAIL_ENABLED",
                    "value": "true"
                },
                {
                    "name": "MAIL_HOST",
                    "value": "email-smtp.us-east-1.amazonaws.com"
                },
                {
                    "name": "MAIL_PORT",
                    "value": "587"
                },
                {
                    "name": "MAIL_FROM",
                    "value": "noreply@taskactivitytracker.com"
                },
                {
                    "name": "ADMIN_EMAIL",
                    "value": "admin@yourcompany.com"
                }
            ],
            "secrets": [
                {
                    "name": "MAIL_USERNAME",
                    "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:ses-smtp-username-abc123"
                },
                {
                    "name": "MAIL_PASSWORD",
                    "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:ses-smtp-password-xyz789"
                }
            ]
        }
    ]
}
```

**Option B: Using AWS Secrets Manager** (Recommended)

1. Store SMTP credentials in Secrets Manager:

```bash
# Create secret for SMTP username
aws secretsmanager create-secret \
  --name taskactivity/ses/smtp-username \
  --description "SES SMTP Username for TaskActivity" \
  --secret-string "AKIAIOSFODNN7EXAMPLE" \
  --region us-east-1

# Create secret for SMTP password
aws secretsmanager create-secret \
  --name taskactivity/ses/smtp-password \
  --description "SES SMTP Password for TaskActivity" \
  --secret-string "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY" \
  --region us-east-1
```

2. Add Secrets Manager permissions to ECS task role:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": ["secretsmanager:GetSecretValue"],
            "Resource": [
                "arn:aws:secretsmanager:us-east-1:123456789012:secret:taskactivity/ses/*"
            ]
        }
    ]
}
```

3. Reference secrets in task definition (see Option A above)

---

## AWS SDK Method (Recommended)

### Overview

Use AWS SDK when:

-   ✅ Deploying on AWS ECS (uses IAM role, no credentials needed)
-   ✅ Want better AWS integration
-   ✅ Need advanced SES features (templates, configuration sets)
-   ✅ Prefer no SMTP credentials to manage

### Step 1: Add AWS SDK Dependencies

Update `pom.xml`:

```xml
<!-- AWS SES SDK -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>ses</artifactId>
    <version>2.21.0</version>
</dependency>

<!-- AWS SDK BOM for version management -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>2.21.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Step 2: Update EmailService to Use AWS SDK

The application includes an updated `EmailService` that supports both SMTP and AWS SDK methods. The SDK method is automatically used when `spring.mail.use-aws-sdk=true`.

**Key changes:**

-   Uses `SesClient` instead of `JavaMailSender` when SDK enabled
-   Automatically uses ECS task IAM role credentials
-   No username/password needed
-   Better AWS integration

### Step 3: Configure Application

**Update `application-aws.properties`:**

```properties
# Enable email notifications
spring.mail.enabled=true

# Use AWS SDK instead of SMTP
spring.mail.use-aws-sdk=true

# AWS Region (should match your ECS cluster region)
aws.region=us-east-1

# Application Email Settings
app.mail.from=noreply@taskactivitytracker.com
app.mail.admin-email=admin@yourcompany.com
app.name=Task Activity Management System
```

**Environment variables for ECS:**

```json
{
    "environment": [
        {
            "name": "MAIL_ENABLED",
            "value": "true"
        },
        {
            "name": "MAIL_USE_AWS_SDK",
            "value": "true"
        },
        {
            "name": "AWS_REGION",
            "value": "us-east-1"
        },
        {
            "name": "MAIL_FROM",
            "value": "noreply@taskactivitytracker.com"
        },
        {
            "name": "ADMIN_EMAIL",
            "value": "admin@yourcompany.com"
        }
    ]
}
```

### Step 4: Update ECS Task IAM Role

Add SES permissions to your ECS task role:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": ["ses:SendEmail", "ses:SendRawEmail"],
            "Resource": "*"
        }
    ]
}
```

**Using AWS CLI:**

```bash
# Get your ECS task role name
TASK_ROLE_NAME="taskactivity-ecs-task-role"

# Create SES policy
cat > ses-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    }
  ]
}
EOF

# Attach policy to role
aws iam put-role-policy \
  --role-name "$TASK_ROLE_NAME" \
  --policy-name TaskActivitySESPolicy \
  --policy-document file://ses-policy.json
```

---

## Testing Email Delivery

### Test 1: Local Development Test

**Trigger account lockout** (easiest way to test):

1. Start your application locally
2. Try logging in with wrong password 5 times
3. Account should lock
4. Check admin email inbox for lockout notification

**Expected Email:**

```
From: noreply@taskactivitytracker.com
To: admin@yourcompany.com
Subject: [Task Activity Management System] Account Locked - testuser

Account Lockout Alert

An account has been locked due to excessive failed login attempts.

Account Details:
- Username: testuser
- Failed Attempts: 5
- Last Attempt IP: 127.0.0.1
- Lockout Time: 2025-12-02 14:30:15

Action Required:
Please review this activity and unlock the account if appropriate.
```

### Test 2: Check Application Logs

```bash
# Look for email sending confirmation
grep -i "email" logs/application.log

# Expected output (success):
INFO  c.a.t.service.EmailService : Sending account lockout notification for user: testuser
INFO  c.a.t.service.EmailService : Email sent successfully to: admin@yourcompany.com

# Expected output (disabled):
INFO  c.a.t.service.EmailService : Email notifications are disabled. Would have sent...
```

### Test 3: AWS SES Console Monitoring

1. Go to SES Console → **Account dashboard**
2. View metrics:

    - **Sends**: Should increment after test
    - **Deliveries**: Confirms successful delivery
    - **Bounces**: Should be 0
    - **Complaints**: Should be 0

3. Check **Reputation metrics**:
    - Bounce rate: < 5% (good)
    - Complaint rate: < 0.1% (good)

### Test 4: Send Test Email via AWS CLI

```bash
# Quick test of SES configuration
aws ses send-email \
  --from noreply@taskactivitytracker.com \
  --destination ToAddresses=admin@yourcompany.com \
  --message "Subject={Data='SES Test',Charset=utf-8},Body={Text={Data='This is a test email from AWS SES.',Charset=utf-8}}" \
  --region us-east-1
```

---

## Production Considerations

### 1. Email Reputation Management

**Monitor Key Metrics:**

-   **Bounce Rate**: Keep below 5%
-   **Complaint Rate**: Keep below 0.1%
-   **Reputation Score**: Maintain "High" status

**Best Practices:**

-   ✅ Use verified domain (better than single email)
-   ✅ Enable DKIM signing
-   ✅ Configure SPF and DMARC records
-   ✅ Monitor bounce and complaint notifications
-   ✅ Maintain suppression lists
-   ✅ Use descriptive, professional email content

### 2. Handling Bounces and Complaints

**Set up SNS notifications:**

1. Create SNS topics for bounces and complaints:

```bash
# Create bounce notification topic
aws sns create-topic \
  --name taskactivity-ses-bounces \
  --region us-east-1

# Create complaint notification topic
aws sns create-topic \
  --name taskactivity-ses-complaints \
  --region us-east-1
```

2. Subscribe to topics (email or Lambda):

```bash
# Subscribe admin email to bounce notifications
aws sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:123456789012:taskactivity-ses-bounces \
  --protocol email \
  --notification-endpoint admin@yourcompany.com
```

3. Configure SES to publish to SNS:

```bash
# Set identity notification topic
aws ses set-identity-notification-topic \
  --identity taskactivitytracker.com \
  --notification-type Bounce \
  --sns-topic arn:aws:sns:us-east-1:123456789012:taskactivity-ses-bounces

aws ses set-identity-notification-topic \
  --identity taskactivitytracker.com \
  --notification-type Complaint \
  --sns-topic arn:aws:sns:us-east-1:123456789012:taskactivity-ses-complaints
```

### 3. Rate Limiting

**Default SES Limits (Production):**

-   **Sending Rate**: 14 emails/second (can request increase)
-   **Daily Quota**: 50,000 emails/day (can request increase)

**To request limit increase:**

1. Go to SES Console → Account dashboard
2. Click **Request sending quota increase**
3. Provide justification

### 4. Cost Optimization

**SES Pricing (US East):**

-   First 62,000 emails/month: **$0.00** (from EC2/ECS)
-   After that: **$0.10 per 1,000 emails**
-   Data transfer: Standard EC2 rates

**Example Monthly Costs:**

-   1,000 emails/month: **FREE**
-   100,000 emails/month: **$3.80**
-   1,000,000 emails/month: **$93.80**

### 5. Security Best Practices

**IAM Permissions:**

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": ["ses:SendEmail", "ses:SendRawEmail"],
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "ses:FromAddress": "noreply@taskactivitytracker.com"
                }
            }
        }
    ]
}
```

**Additional Security:**

-   ✅ Use IAM roles (not access keys) when possible
-   ✅ Implement rate limiting in application
-   ✅ Validate email addresses before sending
-   ✅ Use TLS for SMTP connections
-   ✅ Rotate SMTP credentials regularly (if using SMTP method)
-   ✅ Monitor CloudWatch logs for suspicious activity

### 6. Monitoring and Alerting

**CloudWatch Metrics to Monitor:**

-   `Bounce`
-   `Complaint`
-   `Delivery`
-   `Reject`
-   `Send`

**Create CloudWatch Alarms:**

```bash
# Alert on high bounce rate
aws cloudwatch put-metric-alarm \
  --alarm-name ses-high-bounce-rate \
  --alarm-description "Alert when SES bounce rate exceeds 5%" \
  --metric-name Reputation.BounceRate \
  --namespace AWS/SES \
  --statistic Average \
  --period 3600 \
  --evaluation-periods 1 \
  --threshold 0.05 \
  --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:admin-alerts
```

---

## Troubleshooting

### Issue: Emails Not Sending

**Check 1: Verify Configuration**

```bash
# Check application logs
tail -f logs/application.log | grep -i email

# Expected if disabled:
INFO c.a.t.service.EmailService : Email notifications are disabled

# Expected if enabled:
INFO c.a.t.service.EmailService : Sending account lockout notification
```

**Check 2: Verify SES Identity**

```bash
# List verified identities
aws ses list-identities --region us-east-1

# Check identity verification status
aws ses get-identity-verification-attributes \
  --identities noreply@taskactivitytracker.com \
  --region us-east-1
```

**Check 3: Test Credentials (SMTP method)**

```bash
# Test SMTP authentication
openssl s_client -starttls smtp \
  -connect email-smtp.us-east-1.amazonaws.com:587
# Then try: EHLO localhost
# Should see: 250-AUTH PLAIN LOGIN
```

### Issue: Emails Going to Spam

**Solutions:**

1. ✅ Verify domain (not just email address)
2. ✅ Enable DKIM signing
3. ✅ Add SPF record: `v=spf1 include:amazonses.com ~all`
4. ✅ Add DMARC record: `v=DMARC1; p=quarantine; rua=mailto:dmarc@yourdomain.com`
5. ✅ Use professional email content (no spam trigger words)
6. ✅ Include unsubscribe link (if applicable)
7. ✅ Warm up IP address (gradually increase volume)

### Issue: Rate Limiting Errors

**Error Message:**

```
Maximum sending rate exceeded
```

**Solutions:**

1. Check current limits:
    ```bash
    aws ses get-send-quota --region us-east-1
    ```
2. Request increase via AWS Support
3. Implement application-level rate limiting
4. Use exponential backoff for retries

### Issue: AccessDeniedException (AWS SDK method)

**Error Message:**

```
User: arn:aws:sts::123456789012:assumed-role/taskactivity-ecs-task-role/xxx
is not authorized to perform: ses:SendEmail
```

**Solution:**
Verify ECS task role has SES permissions (see Step 4 of AWS SDK Method)

### Issue: MessageRejected Error

**Possible Causes:**

-   Sending from unverified email address
-   Account still in sandbox mode (trying to send to unverified recipient)
-   Email content triggered spam filters
-   Invalid recipient email address

**Solutions:**

1. Verify sender identity
2. Request production access
3. Check recipient email validity
4. Review email content

### Issue: Bounces

**Hard Bounce** (permanent):

-   Invalid email address
-   Domain doesn't exist
-   Mailbox doesn't exist

**Soft Bounce** (temporary):

-   Mailbox full
-   Server temporarily unavailable
-   Message too large

**Action:**

-   Hard bounces: Remove from mailing list
-   Soft bounces: Retry up to 3 times, then remove

---

## Additional Resources

**AWS Documentation:**

-   [AWS SES Developer Guide](https://docs.aws.amazon.com/ses/latest/dg/)
-   [SES SMTP Interface](https://docs.aws.amazon.com/ses/latest/dg/send-email-smtp.html)
-   [SES API Reference](https://docs.aws.amazon.com/ses/latest/APIReference/)

**Email Best Practices:**

-   [RFC 5321 - SMTP](https://tools.ietf.org/html/rfc5321)
-   [RFC 5322 - Internet Message Format](https://tools.ietf.org/html/rfc5322)
-   [DKIM Specification](https://tools.ietf.org/html/rfc6376)
-   [SPF Specification](https://tools.ietf.org/html/rfc7208)

**Application Documentation:**

-   See `EmailService.java` for implementation details
-   See `application.properties` for configuration options
-   See `aws/deploy-aws.ps1` for deployment with email enabled

---

## Quick Reference

**Environment Variables:**

```bash
MAIL_ENABLED=true                                    # Enable email
MAIL_HOST=email-smtp.us-east-1.amazonaws.com        # SMTP host (SMTP method)
MAIL_PORT=587                                        # SMTP port (SMTP method)
MAIL_USERNAME=AKIAEXAMPLE                           # SMTP username (SMTP method)
MAIL_PASSWORD=secretpassword                         # SMTP password (SMTP method)
MAIL_USE_AWS_SDK=true                               # Use AWS SDK (SDK method)
AWS_REGION=us-east-1                                # AWS region (SDK method)
MAIL_FROM=noreply@taskactivitytracker.com           # Sender email
ADMIN_EMAIL=admin@yourcompany.com                   # Admin notification email
```

**Testing Commands:**

```bash
# Send test email via CLI
aws ses send-email \
  --from noreply@taskactivitytracker.com \
  --destination ToAddresses=test@example.com \
  --message "Subject={Data='Test'},Body={Text={Data='Test body'}}"

# Check send quota
aws ses get-send-quota

# List verified identities
aws ses list-identities

# Check identity status
aws ses get-identity-verification-attributes --identities yourdomain.com
```

---

## Support

For questions or issues:

1. Check application logs: `logs/application.log`
2. Review this guide's Troubleshooting section
3. Check AWS SES service health: https://status.aws.amazon.com/
4. Contact your AWS support team
