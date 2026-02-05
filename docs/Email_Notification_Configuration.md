# Email Notification Configuration Guide

## Overview

The Task Activity Management System now includes email notification functionality that sends alerts to administrators when user accounts are locked due to excessive failed login attempts.

## Features

- **Account Lockout Notifications**: Automatic email alerts when a user account is locked after 5 failed login attempts (configurable)
- **Expense Submission Notifications**: Automatic email alerts to configured approvers when an expense is submitted for approval
- **Expense Status Change Notifications**: Email alerts to expense owners when their expense status changes (Approved, Rejected, Reimbursed)
- **Detailed Information**: Emails include username, failed attempt count, timestamp, and IP address (for lockout) or expense details
- **Multiple Approvers**: Support for comma-separated list of expense approver email addresses
- **Graceful Error Handling**: Email failures do not prevent security or expense functionality
- **Test Email Capability**: Built-in test email function to verify configuration

## Configuration

### 1. Enable Email Notifications

Email notifications are **disabled by default** for security. To enable them:

**Local Development / Docker:**
```bash
MAIL_ENABLED=true
```

**AWS ECS Deployment:**
Already configured in `aws/taskactivity-task-definition.json` with `MAIL_ENABLED=true`

### 2. Configure SMTP Settings

#### For Local Development / Docker

Set the following environment variables with your SMTP server details:

```bash
# SMTP Server Configuration
MAIL_HOST=smtp.gmail.com              # Your SMTP server hostname
MAIL_PORT=587                          # SMTP port (usually 587 for TLS)
MAIL_USERNAME=your-email@gmail.com     # SMTP username
MAIL_PASSWORD=your-app-password        # SMTP password or app-specific password

# Email Addresses
MAIL_FROM=noreply@taskactivity.com                           # From address for emails
ADMIN_EMAIL=3783a389b4e645a99f84af59f0e2a59d@domainsbyproxy.com  # Admin email to receive alerts
EXPENSE_APPROVERS=deanammons@gmail.com,deanammons48@gmail.com     # Comma-separated list of expense approver emails
```

#### For AWS ECS Deployment

The email configuration is defined in `aws/taskactivity-task-definition.json`:

1. **Non-sensitive settings** (already configured in task definition):
   - `MAIL_ENABLED=true`
   - `MAIL_HOST=smtp.gmail.com`
   - `MAIL_PORT=587`
   - `MAIL_FROM=noreply@taskactivity.com`
   - `ADMIN_EMAIL=3783a389b4e645a99f84af59f0e2a59d@domainsbyproxy.com`
   - `EXPENSE_APPROVERS=deanammons@gmail.com,deanammons48@gmail.com`

2. **Sensitive credentials** (stored in AWS Secrets Manager):

   You need to create a secret named `taskactivity/email/credentials` in AWS Secrets Manager with:
   
   ```bash
   # Create the secret using AWS CLI:
   aws secretsmanager create-secret \
     --name taskactivity/email/credentials \
     --description "SMTP credentials for email notifications" \
     --secret-string '{"username":"your-email@gmail.com","password":"your-app-password"}' \
     --region us-east-1
   ```

   Or via AWS Console:
   - Navigate to AWS Secrets Manager
   - Click "Store a new secret"
   - Select "Other type of secret"
   - Add key/value pairs:
     - Key: `username`, Value: `your-email@gmail.com`
     - Key: `password`, Value: `your-app-password`
   - Name: `taskactivity/email/credentials`
   - Click "Store"

3. **Update IAM Policy** (if needed):

   Ensure your ECS task role has permission to read the email secret. Add this to your task role policy:

   ```json
   {
     "Effect": "Allow",
     "Action": [
       "secretsmanager:GetSecretValue"
     ],
     "Resource": [
       "arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/email/credentials*"
     ]
   }
   ```

4. **Deploy the updated task definition**:

   ```bash
   aws ecs register-task-definition \
     --cli-input-json file://aws/taskactivity-task-definition.json \
     --region us-east-1
   ```

### 2. Configure SMTP Settings (Legacy - use AWS instructions above for production)

### 3. Gmail Configuration Example

If using Gmail, you'll need to:

1. **Enable 2-Factor Authentication** on your Google account
2. **Generate an App Password**:
   - Go to: https://myaccount.google.com/security
   - Select "2-Step Verification"
   - At the bottom, select "App passwords"
   - Generate a new app password for "Mail"
   - Use this 16-character password as `MAIL_PASSWORD`

Example Gmail configuration:

```bash
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=yourname@gmail.com
MAIL_PASSWORD=abcd efgh ijkl mnop  # Your 16-char app password
MAIL_FROM=yourname@gmail.com
ADMIN_EMAIL=3783a389b4e645a99f84af59f0e2a59d@domainsbyproxy.com
```

### 4. Other SMTP Providers

#### Microsoft Outlook/Office 365
```bash
MAIL_HOST=smtp-mail.outlook.com
MAIL_PORT=587
```

#### AWS SES
```bash
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=<your-ses-smtp-username>
MAIL_PASSWORD=<your-ses-smtp-password>
```

#### SendGrid
```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=<your-sendgrid-api-key>
```

## Docker Configuration

When running with Docker, add the email configuration to your docker-compose.yml or docker run command:

### Docker Compose Example

```yaml
services:
  app:
    image: taskactivity:latest
    environment:
      - MAIL_ENABLED=true
      - MAIL_HOST=smtp.gmail.com
      - MAIL_PORT=587
      - MAIL_USERNAME=yourname@gmail.com
      - MAIL_PASSWORD=your-app-password
      - MAIL_FROM=yourname@gmail.com
      - ADMIN_EMAIL=3783a389b4e645a99f84af59f0e2a59d@domainsbyproxy.com
```

### Docker Run Example

```bash
docker run -d \
  -e MAIL_ENABLED=true \
  -e MAIL_HOST=smtp.gmail.com \
  -e MAIL_PORT=587 \
  -e MAIL_USERNAME=yourname@gmail.com \
  -e MAIL_PASSWORD=your-app-password \
  -e MAIL_FROM=yourname@gmail.com \
  -e ADMIN_EMAIL=3783a389b4e645a99f84af59f0e2a59d@domainsbyproxy.com \
  taskactivity:latest
```

## Email Content Examples

### Account Lockout Notification

When an account is locked, the administrator will receive an email similar to this:

```
Subject: [Task Activity Management System] Account Locked: johndoe

ACCOUNT LOCKOUT ALERT

A user account has been locked due to excessive failed login attempts.

Details:
----------------------------------------
Username: johndoe
Failed Login Attempts: 5
Lockout Timestamp: 2025-11-15 18:30:45
IP Address: 192.168.1.100
----------------------------------------

Action Required:
Please investigate this potential security incident. The account can be unlocked 
via the User Management interface if the activity is determined to be legitimate.

To unlock the account:
1. Log in to the Task Activity Management System
2. Navigate to User Management
3. Find the user 'johndoe'
4. Edit the user and uncheck 'Account is locked'
5. Save changes

This is an automated message from Task Activity Management System.
```

### Expense Submission Notification

When an expense is submitted, configured approvers will receive an email similar to this:

```
Subject: [Task Activity Management System] New Expense Submitted - 123

NEW EXPENSE SUBMITTED FOR APPROVAL

A new expense has been submitted and is awaiting your review.

Expense Details:
----------------------------------------
Submitted By:       John Doe (jdoe)
Expense ID:         123
Description:        Client meeting lunch
Amount:             45.50 USD
Expense Date:       2025-12-07
Submitted:          2025-12-07 14:30:00
----------------------------------------

Action Required:
Please review and approve or reject this expense via the Expense Approval Queue.

To review the expense:
1. Log in to the Task Activity Management System
2. Navigate to Expense Management > Approval Queue
3. Find expense ID 123
4. Review details and approve or reject with notes

This is an automated notification from Task Activity Management System.
Do not reply to this email. This email is sent from an unattended mailbox.
```

### Expense Status Change Notification

When an expense status changes, the expense owner will receive an email similar to this:

```
Subject: [Task Activity Management System] Expense Approved - 123

EXPENSE STATUS UPDATE

Your expense has been approved and is ready for reimbursement processing.

Expense Details:
----------------------------------------
User:               John Doe
Description:        Client meeting lunch
Amount:             45.50 USD
New Status:         Approved
Processed By:       admin
Date:               2025-12-07 15:00:00
----------------------------------------

Notes:
----------------------------------------
Approved for reimbursement. Receipt verified.
----------------------------------------

This is an automated notification from Task Activity Management System.
Do not reply to this email. This email is sent from an unattended mailbox.
```

## Email Triggers Summary

The system sends email notifications in the following scenarios:

1. **Account Lockout** → Sent to `ADMIN_EMAIL`
   - Triggered after 5 consecutive failed login attempts
   - Contains username, IP address, timestamp, and failed attempt count

2. **Expense Submitted** → Sent to `EXPENSE_APPROVERS` (comma-separated list)
   - Triggered when user submits an expense (Draft → Submitted status)
   - Contains expense details, submitter information, and approval instructions

3. **Expense Status Changed** → Sent to expense owner's email address
   - Triggered when expense status changes to Approved, Rejected, or Reimbursed
   - Contains expense details, new status, processor name, and optional notes

## Testing the Configuration

### Method 1: Test Account Lockout (Not Recommended for Production)

1. Create a test user account
2. Attempt to log in with incorrect credentials 5 times
3. The account will be locked
4. Check the admin email inbox for the notification
5. Unlock the account via User Management

### Method 2: Test Expense Submission

1. Log in as a regular user (not admin)
2. Navigate to Expense Management > Add Expense
3. Create and save a new expense (status will be Draft)
4. Submit the expense for approval
5. Check all email addresses configured in `EXPENSE_APPROVERS` for the notification
6. Verify each approver receives:
   - Expense ID, description, amount, date
   - Submitter name and username
   - Instructions to review in approval queue

### Method 3: Test Expense Status Changes

1. Log in as an admin or approver
2. Navigate to Expense Management > Approval Queue
3. Find a submitted expense and approve or reject it
4. Check the expense owner's email address (from User Management profile)
5. Verify they receive:
   - Expense details and new status
   - Your username as the processor
   - Any notes you added during approval/rejection

### Method 4: Use the EmailService Test Method (Recommended)

The `EmailService` includes a `sendTestEmail()` method for testing. This can be invoked via:

1. Spring Boot actuator endpoint (if exposed)
2. Custom admin endpoint (would need to be implemented)
3. Direct service invocation in a test environment

## Troubleshooting

### Email Not Sending

1. **Check logs**: Look for error messages in application logs
   ```
   ERROR com.ammons.taskactivity.service.EmailService -- Failed to send account lockout notification email
   ```

2. **Verify SMTP settings**: Ensure all environment variables are set correctly

3. **Check firewall**: Ensure port 587 (or your SMTP port) is not blocked

4. **Verify credentials**: Make sure username and password are correct

5. **Enable less secure apps** (Gmail): If not using app passwords, you may need to enable "Less secure app access" (not recommended)

### Email Goes to Spam

- Configure SPF, DKIM, and DMARC records for your domain
- Use a verified "From" address
- Consider using a transactional email service (SendGrid, AWS SES, etc.)

### Disable Email for Testing

To disable email notifications without removing configuration:

```bash
MAIL_ENABLED=false
```

## Security Considerations

1. **Never commit credentials** to version control
2. **Use environment variables** for all sensitive configuration
3. **Use app-specific passwords** instead of main account passwords
4. **Monitor email delivery** for potential issues
5. **Regularly rotate credentials** according to your security policy

## Additional Configuration Options

The following properties can be customized in `application.properties` if needed:

```properties
# Maximum failed login attempts before lockout (default: 5)
security.login.max-attempts=5

# Application name (appears in email subject/body)
app.name=Task Activity Management System
```

## Dependencies

This feature requires the following Maven dependency (already added):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## Support

For additional support or to report issues, please contact the system administrator or refer to the main documentation.
