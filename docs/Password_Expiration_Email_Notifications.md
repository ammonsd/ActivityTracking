# Password Expiration Email Notifications

## Overview

The system automatically sends email notifications to users about their password status. There are two types of notifications:

1. **Password Expiring Soon Warning**: Sent when passwords will expire within 7 days
2. **Password Has Expired Notice**: Sent once when a password actually expires

This feature helps ensure users change their passwords proactively and are informed when their password has expired.

## How It Works

### Automatic Daily Check

The system runs an automated check **every day at 8:00 AM** server time to:
- Identify users whose passwords are expiring soon and send warning emails
- Identify users whose passwords have expired and send expired notifications (once only)

### Notification Types

#### 1. Password Expiring Soon (Warning)

Email warnings are sent when passwords will expire within the next **7 days**.

These warnings are sent **daily** during the 7-day window, so users may receive multiple reminders as the expiration date approaches.

**Email Content - Expiring Warning:**

-   **Subject**: `[Task Activity Management System] Password Expiration Warning`
-   **Urgency level** based on days remaining:
    -   1 day: "⚠️ URGENT: Your password expires in 1 day!"
    -   2-3 days: "⚠️ IMPORTANT: Your password expires in X days!"
    -   4-7 days: "Your password will expire in X days."
-   **Instructions** on how to change the password
-   **Consequences** if the password expires

#### 2. Password Has Expired (Expired Notice)

When a password expires, the system sends **one notification** on the **first day after expiration**. 

**How It Works:**
- If password expires on 01/17/2026, the expired email is sent on 01/18/2026 (when expiration_date = yesterday)
- On 01/19/2026 and beyond, no additional emails are sent
- No database tracking needed - the logic uses the expiration date itself

**Email Content - Expired Notice:**

-   **Subject**: `[Task Activity Management System] Password Has Expired - Action Required`
-   **Message**: "🔒 YOUR PASSWORD HAS EXPIRED"
-   **Instructions** on how to log in and change the expired password
-   **Important notes**:
    -   User cannot access the system until password is changed
    -   Expired password still works ONE TIME to log in and change it
    -   New password will be valid for 90 days

**Why Only Once:**

The system checks if `expiration_date = yesterday`. This condition is only true on the first day after expiration, ensuring the notification is sent exactly once without requiring database flags or tracking.

### Who Gets Notified

Notifications are sent to users who:

-   ✅ Have a password expiration date set
-   ✅ Have an email address on file
-   ✅ Have USER or ADMIN role
-   ✅ Are enabled and not locked
-   ✅ Have passwords expiring within 7 days

### Who Is Skipped

Notifications are **not** sent to:

-   ❌ GUEST users (they cannot change their own passwords)
-   ❌ Users without email addresses
-   ❌ Disabled or locked accounts
-   ❌ Users with no expiration date set (legacy accounts)
-   ❌ Users whose passwords expire beyond 7 days

## Configuration

### Email Settings

The feature respects the global email configuration:

```properties
# Enable/disable email notifications
spring.mail.enabled=true

# Email sender address
app.mail.from=noreply@taskactivity.com

# Application name (appears in subject line)
app.name=Task Activity Management System
```

If `spring.mail.enabled=false`, the scheduled check still runs but no emails are sent (logs only).

### Schedule Configuration

The check runs daily at 8:00 AM server time. This is configured in `PasswordExpirationNotificationService.java`:

```java
@Scheduled(cron = "0 0 8 * * *") // Daily at 8 AM
public void checkExpiringPasswordsAndNotify()
```

This single scheduled job checks for **both**:
1. Passwords expiring within 7 days → sends warnings (can repeat daily)
2. Passwords expired yesterday → sends expired notification (once only, because today it's no longer "yesterday")

To change the schedule, modify the cron expression:

-   `0 0 8 * * *` = 8:00 AM daily
-   `0 0 */6 * * *` = Every 6 hours
-   `0 0 8 * * MON-FRI` = 8:00 AM weekdays only

## Manual Testing

Administrators can manually trigger the password expiration check for testing purposes.

### Using the API Endpoint

**Endpoint**: `POST /task-activity/manage-users/trigger-password-expiration-check`  
**Authorization**: Requires ADMIN role with `USER_MANAGEMENT:ADMIN` permission  
**Method**: POST

**Example using curl**:

```bash
curl -X POST http://localhost:8080/task-activity/manage-users/trigger-password-expiration-check \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Example Response**:

```json
{
    "success": true,
    "message": "Password expiration check completed successfully"
}
```

### Testing Checklist

To test the notification system:

1. **Create a test user** with an email address
2. **Set password expiration** to 5 days from now:
    ```sql
    UPDATE users
    SET expiration_date = CURRENT_DATE + INTERVAL '5 days'
    WHERE username = 'testuser';
    ```
3. **Trigger manual check** using the API endpoint above
4. **Verify email** was sent to the test user's email address
5. **Check logs** for confirmation:
    ```
    INFO  c.a.t.s.PasswordExpirationNotificationService - Sent password expiration warning to user: testuser (5 days remaining)
    ```

## Logging

The service logs detailed information about the notification process:

```
INFO  - Starting daily password expiration check...
INFO  - Sent password expiration warning to user: testuser (5 days remaining)
DEBUG - Skipping GUEST user: guest
DEBUG - Skipping user john - no email address
INFO  - Password expiration check complete. Notifications sent: 3, Skipped users: 5, Total checked: 8
```

## Monitoring

### Log Locations

**Local Development**:

-   Console output during application run
-   Check Spring Boot logs for `PasswordExpirationNotificationService` entries

**AWS Deployment (ECS)**:

-   CloudWatch Logs: `/ecs/taskactivity` log group
-   Filter pattern: `PasswordExpirationNotificationService`

### Key Metrics to Monitor

1. **Notifications sent**: Should increase as passwords approach expiration
2. **Skipped users**: Validate these are appropriate (GUEST, no email, etc.)
3. **Email failures**: Check for `Failed to send password expiration warning` errors
4. **Schedule execution**: Verify daily runs occur at 8:00 AM

### Example CloudWatch Query

```
fields @timestamp, @message
| filter @message like /PasswordExpirationNotificationService/
| filter @message like /complete/
| sort @timestamp desc
| limit 20
```

## Troubleshooting

### Issue: No Emails Being Sent

**Check**:

1. Is `spring.mail.enabled=true` in configuration?
2. Are SMTP settings or AWS SES configured correctly?
3. Do users have valid email addresses?
4. Are passwords actually within the 7-day window?

**Verify**:

```bash
# Check user expiration dates
SELECT username, email, expiration_date,
       expiration_date - CURRENT_DATE as days_until_expiration
FROM users
WHERE expiration_date IS NOT NULL
  AND expiration_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
  AND role_id != (SELECT id FROM roles WHERE name = 'GUEST')
ORDER BY expiration_date;
```

### Issue: GUEST Users Receiving Emails

This should not happen - GUEST users are explicitly skipped. If it occurs:

1. Check the user's role: `SELECT username, role_id FROM users WHERE username = 'guest';`
2. Verify role name is exactly "GUEST" (case-insensitive check in code)
3. Review logs for the specific user

### Issue: Scheduled Task Not Running

**Check**:

1. Is `@EnableScheduling` present in `TaskactivityApplication.java`?
2. Are there any errors on application startup?
3. Check server timezone vs. expected schedule time

**Verify Scheduling**:

```bash
# Check Spring Boot logs for scheduling confirmation
grep -i "scheduled tasks" application.log
```

### Issue: Emails Sent Multiple Times for Expired Passwords

**This should not happen** - expired notifications are sent only when `expiration_date = yesterday`.

**Possible causes**:

1. Multiple application instances running (e.g., multiple ECS tasks) - each checks independently
2. Manual trigger endpoint being called multiple times
3. Server timezone issues causing "yesterday" calculation to be inconsistent

**Solution**:

-   For multiple instances, consider using distributed locking (e.g., AWS DynamoDB, Redis)
-   Ensure all application instances use the same timezone (UTC recommended)
-   Check logs to see if multiple instances are sending notifications

**Note**: Expiring warnings (1-7 days) are sent daily by design, but expired notifications should only occur once.

## Future Enhancements

Potential improvements to consider:

1. **Configurable thresholds**: Allow admins to set 7-day, 14-day, 30-day warnings
2. **Escalation**: Send additional notifications at 3 days, 1 day
3. **Admin digest**: Daily summary email to admins showing all expiring passwords
4. **UI indicator**: Add visual indicator on user management page for expiring passwords
5. **Notification preferences**: Allow users to opt-in/opt-out of email reminders
6. **SMS notifications**: Support SMS for critical (1-day) warnings
7. **Distributed locking**: Prevent duplicate notifications in multi-instance deployments

## Technical Details

### Files Involved

1. **Service**: `com.ammons.taskactivity.service.PasswordExpirationNotificationService`

    - Scheduled task that checks for expiring passwords
    - Sends email notifications via EmailService

2. **Email Service**: `com.ammons.taskactivity.service.EmailService`

    - `sendPasswordExpirationWarning()` - Send warning email (1-7 days)
    - `sendPasswordExpiredNotification()` - Send expired notification (once only)
    - `buildPasswordExpirationWarningEmailBody()` - Format warning email
    - `buildPasswordExpiredEmailBody()` - Format expired notification

3. **Controller**: `com.ammons.taskactivity.controller.UserManagementController`

    - Manual trigger endpoint for testing

4. **Tests**: `com.ammons.taskactivity.service.PasswordExpirationNotificationServiceTest`
    - Unit tests for notification logic

### Dependencies

No additional dependencies required - uses existing:

-   Spring Boot Scheduling (`@Scheduled`)
-   Spring Mail or AWS SES (already configured)
-   User repository access

### Database Schema

Uses existing `users` table columns - **no schema changes required**:

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    expiration_date DATE,          -- Used to determine if password expired yesterday
    role_id BIGINT NOT NULL REFERENCES roles(id),
    enabled BOOLEAN DEFAULT TRUE,
    account_locked BOOLEAN DEFAULT FALSE,
    ...
);
```

**Key Point**: The `expiration_date = yesterday` logic eliminates the need for additional tracking columns.

## Security Considerations

1. **Email content**: Does not include passwords or sensitive data
2. **Rate limiting**: One email per user per day (during scheduled run)
3. **GUEST protection**: GUEST users are explicitly excluded from notifications
4. **Manual trigger**: Requires ADMIN role and `USER_MANAGEMENT:ADMIN` permission
5. **Logging**: User emails are logged for audit purposes but not passwords

## Support

For questions or issues:

1. Check application logs for detailed error messages
2. Verify email configuration is correct
3. Test with a single user before relying on automated notifications
4. Contact system administrator if emails are not being delivered

---

**Last Updated**: January 17, 2026  
**Version**: 1.1  
**Author**: Dean Ammons
