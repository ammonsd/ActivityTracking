# Jenkins Automated Build & Deploy Guide

**Author:** Dean Ammons  
**Date:** January 2026

## Overview

This guide explains the automated Jenkins pipeline configuration for the Task Activity Tracking application, which implements:

1. **Automatic builds with no-cache** whenever the `main` branch is updated
2. **Daily deployments at 4pm**, but only when new builds are available
3. **Smart deployment skipping** when no new builds exist or manual deployments have already been performed

## Pipeline Triggers

### 1. SCM Polling (Main Branch Updates)

**Trigger:** Every 5 minutes on the `main` branch  
**Configuration:** `pollSCM(env.BRANCH_NAME == 'main' ? 'H/5 * * * *' : '')`

**Behavior:**

- Checks for new commits on the `main` branch every 5 minutes
- When changes are detected, automatically triggers a build
- **Automatically enables `NO_CACHE`** for these builds to ensure a clean rebuild
- Performs build-only action (does not deploy)

**Why NO_CACHE?**

- Main branch changes typically include significant updates
- Clean builds ensure no stale dependencies or cached layers
- Prevents subtle bugs from cached intermediate layers
- Guarantees reproducible builds

### Build Batching and Concurrent Builds

**Important:** The pipeline has `disableConcurrentBuilds()` enabled, which affects how multiple commits are handled:

**You do NOT need to wait between PR merges or commits!** Jenkins intelligently batches commits:

#### Scenario 1: Multiple Quick Commits

```
3:00 PM - Merge PR #1 to main
3:01 PM - Merge PR #2 to main  
3:02 PM - Merge PR #3 to main
3:04 PM - SCM poll runs (within 5 min window)
         → Detects all 3 commits
         → Starts ONE build with all changes
         → Builds with NO_CACHE enabled
```

**Result:** All three PRs are included in a single build. No waiting required!

#### Scenario 2: Commit While Build is Running

```
3:00 PM - Build #120 starts (previous commits)
3:01 PM - Merge PR #1 to main
3:02 PM - Merge PR #2 to main
3:04 PM - SCM poll detects new commits
         → Build #121 is QUEUED (not started yet)
3:15 PM - Build #120 completes
         → Build #121 starts automatically
         → Includes both PR #1 and PR #2
```

**Result:** Commits are queued and batched automatically. No manual intervention needed!

#### Why This Works Well

✅ **Efficient Resource Usage** - Prevents multiple builds from competing for resources
✅ **Natural Batching** - 5-minute polling window groups rapid commits
✅ **Automatic Queuing** - Jenkins handles build scheduling automatically  
✅ **Complete Testing** - Each build tests all commits since last successful build
✅ **Clean Builds** - Every build uses NO_CACHE for maximum reliability

#### When You Might Want to Change This

If you need **parallel builds** for different branches or faster feedback:

```groovy
options {
    buildDiscarder(logRotator(numToKeepStr: '30'))
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
    // Remove or comment out the next line to allow concurrent builds
    // disableConcurrentBuilds()
}
```

**Trade-offs of allowing concurrent builds:**
- ✅ Faster feedback for multiple branches
- ✅ Can run main branch builds in parallel with feature branches
- ⚠️ Higher resource usage (CPU, memory, disk)
- ⚠️ Potential Docker conflicts if not configured carefully
- ⚠️ More complex log analysis with multiple simultaneous builds

**Recommendation:** Keep `disableConcurrentBuilds()` enabled for main branch stability.

### 2. Time-Based Deployment (Daily at 4pm)

**Trigger:** Daily at 4:00 PM (with hash-based distribution)  
**Configuration:** `cron('H 16 * * *')`

**Understanding Jenkins H Notation:**

Jenkins uses the "H" notation (for "hash") to distribute scheduled builds evenly across time slots:

- **Syntax:** `H 16 * * *` means "once between 4:00 PM and 4:59 PM"
- **How it works:** Jenkins calculates a consistent time based on the job name's hash
- **Consistency:** The same job will ALWAYS run at the same time each day
- **Example:** "TaskActivity-Pipeline" might hash to 4:51 PM, and will run at 4:51 PM every day
- **Purpose:** Prevents all Jenkins jobs from starting at exactly 4:00 PM, which would overwhelm the server

**Why use H notation instead of `0 16 * * *`?**

- ✅ Spreads load across the hour rather than all at once
- ✅ Reduces resource contention on Jenkins server
- ✅ Still provides predictable, consistent scheduling per job
- ✅ Better performance for multi-tenant Jenkins environments

**Note:** While the time COULD be anywhere from 4:00-4:59 PM for different jobs, YOUR specific job will always run at the SAME time within that window based on its name hash.

**Behavior:**

- Runs every day at the hash-calculated time (consistently the same time daily)
- **Automatically checks** if deployment should proceed
- **Skips deployment** if:
    - No successful builds since last deployment
    - A manual deployment was performed after the last build
- **Proceeds with deployment** if:
    - New builds are available since the last deployment
    - No deployments have been performed yet

## Deployment Decision Logic

The scheduled 4pm build uses intelligent logic to determine if deployment is necessary:

```groovy
1. Get last successful build (DEPLOY_ACTION='build-only')
2. Get last successful deployment (DEPLOY_ACTION='deploy')
3. Compare build numbers:
   - If last_build > last_deploy → Deploy
   - If last_build <= last_deploy → Skip
   - If no builds exist → Skip
   - If no deploys exist but builds exist → Deploy
```

### Edge Cases Handled

| Scenario                  | Last Build | Last Deploy | Action |
| ------------------------- | ---------- | ----------- | ------ |
| Fresh setup               | None       | None        | Skip   |
| New builds available      | #125       | #120        | Deploy |
| Already deployed          | #125       | #125        | Skip   |
| Manual deploy after build | #125       | #127        | Skip   |
| Only builds, no deploys   | #125       | None        | Deploy |

## Build Types and Identification

The pipeline automatically identifies and tags three types of builds:

### 1. SCM-Triggered Build

- **Trigger:** Git commit to main branch
- **Description:** "Auto-build from main branch (no-cache)"
- **Behavior:**
    - Always builds with `NO_CACHE=true`
    - Does not deploy
    - Prepares artifact for later deployment

### 2. Scheduled Deployment

- **Trigger:** Daily cron at 4pm
- **Description:** "Scheduled deployment at 4pm"
- **Behavior:**
    - Checks eligibility first
    - Deploys if new builds available
    - Skips if no new builds or recent manual deploy

### 3. Manual Build/Deploy

- **Trigger:** User-initiated through Jenkins UI
- **Description:** "Manual build-only" or "Manual deploy"
- **Behavior:**
    - Respects all user-selected parameters
    - User controls `NO_CACHE` setting
    - User controls `DEPLOY_ACTION`
    - Sets `MANUAL_TRIGGER=true` for tracking

## Parameters

### Standard Parameters

| Parameter               | Default    | Description                                              |
| ----------------------- | ---------- | -------------------------------------------------------- |
| `ENVIRONMENT`           | dev        | Target environment (dev/staging/production)              |
| `DEPLOY_ACTION`         | build-only | Action to perform (build-only/deploy/rollback)           |
| `SKIP_TESTS`            | false      | Skip unit tests (not allowed for production)             |
| `NO_CACHE`              | false      | Build Docker without cache (auto-enabled for SCM builds) |
| `DEPLOY_INFRASTRUCTURE` | false      | Deploy CloudFormation stack before app                   |
| `INFRASTRUCTURE_ACTION` | update     | CloudFormation action (update/create/preview)            |
| `MANUAL_TRIGGER`        | false      | Indicates manual build (affects scheduled logic)         |

### Auto-Configured Parameters

Some parameters are automatically set based on trigger type:

- **SCM Builds:** `NO_CACHE` → `true`
- **Scheduled Builds:** `DEPLOY_ACTION` → `deploy` (if eligible)

## Environment Variables

The pipeline sets several environment variables to track build state:

| Variable                  | Values      | Description                   |
| ------------------------- | ----------- | ----------------------------- |
| `IS_SCHEDULED_BUILD`      | true/false  | Build triggered by cron       |
| `IS_SCM_BUILD`            | true/false  | Build triggered by Git commit |
| `IS_MANUAL_BUILD`         | true/false  | Build triggered manually      |
| `BUILD_NO_CACHE`          | true/false  | Actual NO_CACHE setting used  |
| `SHOULD_DEPLOY`           | true/false  | Scheduled build should deploy |
| `SCHEDULED_DEPLOY_ACTION` | deploy/skip | Action for scheduled build    |

## Pipeline Stages

All stages include conditional logic to skip when scheduled deployment is not eligible:

```groovy
when {
    allOf {
        expression { params.DEPLOY_ACTION != 'rollback' }
        expression {
            !(env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false')
        }
    }
}
```

### Stage Execution Matrix

| Stage          | SCM Build | Scheduled (Deploy) | Scheduled (Skip) | Manual Build | Manual Deploy |
| -------------- | --------- | ------------------ | ---------------- | ------------ | ------------- |
| Initialize     | ✓         | ✓                  | ✓                | ✓            | ✓             |
| Skip Scheduled | ✗         | ✗                  | ✓                | ✗            | ✗             |
| Build & Test   | ✓         | ✓                  | ✗                | ✓            | ✗             |
| Docker Build   | ✓         | ✓                  | ✗                | ✓            | ✗             |
| Push to ECR    | ✗         | ✓                  | ✗                | ✗            | ✓             |
| Deploy to ECS  | ✗         | ✓                  | ✗                | ✗            | ✓             |
| Verify         | ✗         | ✓                  | ✗                | ✗            | ✓             |

## Helper Functions

### getLastSuccessfulBuildNumber(actionType)

Searches through the last 50 builds to find the most recent successful build of a specific type.

**Parameters:**

- `actionType`: Either `'build-only'` or `'deploy'`

**Returns:**

- Build number of last successful build, or `null` if none found

**Logic:**

1. Iterates through last 50 builds in reverse order
2. Skips current build and failed builds
3. Checks `DEPLOY_ACTION` parameter
4. Also checks for scheduled deployments by examining environment variables
5. Returns first match found

**Implementation Notes - Jenkins Script Security:**

The function uses `currentBuild.rawBuild.parent` to access the job object rather than `Jenkins.instance.getItem()`:

```groovy
// ✅ CORRECT - Uses approved API
def job = currentBuild.rawBuild.parent

// ❌ INCORRECT - Requires admin approval in Jenkins Script Security
def job = Jenkins.instance.getItem(env.JOB_NAME)
```

**Why this matters:**

- Jenkins Script Security blocks unapproved method signatures by default
- `Jenkins.instance` requires `staticMethod jenkins.model.Jenkins getInstance` to be approved
- `currentBuild.rawBuild.parent` is an approved API that provides direct access to the job
- Using approved APIs avoids security exceptions and "Scripts not permitted" errors

**Troubleshooting:**

If you see this error in console output:
```
Scripts not permitted to use staticMethod jenkins.model.Jenkins getInstance.
Administrators can decide whether to approve or reject this signature.
```

This means the function is trying to use `Jenkins.instance`. Update to use `currentBuild.rawBuild.parent` instead.

**Usage:**

```groovy
def lastBuildNumber = getLastSuccessfulBuildNumber('build-only')
def lastDeployNumber = getLastSuccessfulBuildNumber('deploy')

if (lastBuildNumber > lastDeployNumber) {
    // New builds available, proceed with deployment
}
```

## Notifications

The pipeline sends notifications to the Task Activity application with additional context:

### Success Notification

```json
{
    "buildNumber": "125",
    "branch": "main",
    "commit": "abc1234",
    "buildUrl": "http://jenkins/job/TaskActivity-Pipeline/125/",
    "environment": "production",
    "triggeredBy": "scheduled|scm|manual",
    "noCache": "true"
}
```

### Failure Notification

```json
{
    "buildNumber": "125",
    "branch": "main",
    "commit": "abc1234",
    "buildUrl": "http://jenkins/job/TaskActivity-Pipeline/125/",
    "consoleUrl": "http://jenkins/job/TaskActivity-Pipeline/125/console",
    "environment": "production",
    "triggeredBy": "scheduled|scm|manual"
}
```

**Notification Endpoints:**

- Build success: `POST /api/jenkins/build-success`
- Build failure: `POST /api/jenkins/build-failure`
- Deploy success: `POST /api/jenkins/deploy-success`
- Deploy failure: `POST /api/jenkins/deploy-failure`

## Daily Workflow Example

### Scenario: Typical Development Day

**9:00 AM** - Developer commits to main branch

- SCM poll detects change within 5 minutes
- Build #120 starts automatically
- NO_CACHE is enabled automatically
- Build completes, artifact stored
- Description: "Auto-build from main branch (no-cache)"

**11:30 AM** - Another developer commits

- Build #121 starts automatically
- NO_CACHE is enabled automatically
- Build completes, artifact stored

**4:00 PM** - Scheduled deployment trigger

- Initialize stage runs deployment eligibility check
- Finds last build: #121
- Finds last deploy: #119
- Determines: last_build (121) > last_deploy (119)
- **Decision:** Proceed with deployment
- Build #122 starts
- Deploys build #121 to production
- Description: "Scheduled deployment at 4pm"

**Next Day, 4:00 PM** - Scheduled deployment trigger

- Initialize stage runs deployment eligibility check
- Finds last build: #121
- Finds last deploy: #122
- Determines: last_build (121) ≤ last_deploy (122)
- **Decision:** Skip deployment
- Build #123 marked as NOT_BUILT
- Description: "Skipped: No new builds since last deployment"

### Scenario: Manual Deployment

**2:00 PM** - Operations team needs immediate deploy

- User triggers manual build
- Sets `DEPLOY_ACTION=deploy`
- Sets `MANUAL_TRIGGER=true`
- Build #124 starts and deploys
- Description: "Manual deploy"

**4:00 PM** - Scheduled deployment trigger

- Initialize stage runs deployment eligibility check
- Finds last build: #121
- Finds last deploy: #124
- Determines: last_build (121) ≤ last_deploy (124)
- **Decision:** Skip deployment (manual deploy already done)
- Build #125 marked as NOT_BUILT

## Jenkins Configuration Requirements

### Required Plugins

- Pipeline
- Git
- Docker Pipeline
- AWS Steps
- Maven Integration

### Required Credentials

- `aws-credentials`: AWS access key and secret
- `jenkins-api-token`: JWT token for application notifications

### Required Tools

- JDK 21 (configured as 'JDK-21')
- Maven 3.9+ (configured as 'Maven-3.9')
- Docker

### Pipeline Configuration

**In Jenkins Job Configuration:**

1. **Pipeline → Definition:** Pipeline script from SCM
2. **Pipeline → SCM:** Git
3. **Pipeline → Repository URL:** Your Git repository
4. **Pipeline → Branch:** `*/main` (or your default branch)
5. **⚠️ CRITICAL: Do NOT check "Lightweight checkout"** (required for SCM polling to detect changes)

**Why this matters:** Lightweight checkout only downloads the Jenkinsfile, not the full repository history, which prevents SCM polling from detecting changes. If polling doesn't trigger builds after commits, this is the most common cause.

**Build Triggers:**

- Do NOT manually configure triggers in Jenkins UI
- Triggers are defined in the Jenkinsfile itself
- Jenkins will automatically register triggers when pipeline is loaded

## Temporarily Suspending Triggers

There may be times when you need to temporarily disable the automatic triggers (e.g., during maintenance, troubleshooting, or extended development work).

### Method 1: Disable in Jenkins UI (Recommended for Temporary Suspension)

This is the easiest method for temporarily pausing builds without modifying code:

1. **Navigate to Job Configuration:**
   - Go to Jenkins dashboard
   - Click on your pipeline job (e.g., "TaskActivity-Pipeline")
   - Click "Configure" in the left menu

2. **Disable the Project:**
   - Check the box "Disable this project"
   - Click "Save"
   - **Result:** All triggers (SCM and cron) are disabled
   - The job can still be triggered manually if needed

3. **Re-enable When Ready:**
   - Uncheck "Disable this project"
   - Click "Save"
   - Triggers will resume automatically

**Use this method when:**
- You need quick temporary suspension
- You're doing maintenance or testing
- You want to prevent both SCM and scheduled builds
- You might still need to run manual builds

### Method 2: Comment Out Triggers in Jenkinsfile (For Extended Periods)

For longer-term suspension or selective disabling, modify the Jenkinsfile:

**To disable both triggers:**

```groovy
pipeline {
    agent any
    
    // triggers {
    //     // Build on main branch commits (auto-build with no-cache)
    //     pollSCM(env.BRANCH_NAME == 'main' ? 'H/5 * * * *' : '')
    //     
    //     // Daily deployment at 4pm (only if there are new builds)
    //     cron('0 16 * * *')
    // }
```

**To disable only SCM polling (keep scheduled deployments):**

```groovy
triggers {
    // Build on main branch commits (auto-build with no-cache)
    // pollSCM(env.BRANCH_NAME == 'main' ? 'H/5 * * * *' : '')
    
    // Daily deployment at 4pm (only if there are new builds)
    cron('0 16 * * *')
}
```

**To disable only scheduled deployments (keep SCM builds):**

```groovy
triggers {
    // Build on main branch commits (auto-build with no-cache)
    pollSCM(env.BRANCH_NAME == 'main' ? 'H/5 * * * *' : '')
    
    // Daily deployment at 4pm (only if there are new builds)
    // cron('0 16 * * *')
}
```

**Steps:**
1. Edit the Jenkinsfile in your repository
2. Comment out the triggers you want to disable
3. Commit and push the changes
4. Jenkins will reload the configuration automatically
5. Uncomment and push again when you want to re-enable

**Use this method when:**
- You need to disable triggers for an extended period (weeks/months)
- You want version-controlled documentation of why triggers were disabled
- You need different team members to see that triggers are disabled
- You want to disable only specific triggers (SCM or cron, not both)

### Method 3: Use Branch-Specific Logic (Selective Triggering)

You can modify the trigger conditions to be more selective:

**Example: Only trigger on specific branch:**

```groovy
triggers {
    // Only trigger on 'release' branch, not 'main'
    pollSCM(env.BRANCH_NAME == 'release' ? 'H/5 * * * *' : '')
    
    // Keep scheduled deployment
    cron('0 16 * * *')
}
```

**Example: Only trigger on weekdays:**

```groovy
triggers {
    pollSCM(env.BRANCH_NAME == 'main' ? 'H/5 * * * *' : '')
    
    // Only deploy Monday-Friday at 4pm
    cron('0 16 * * 1-5')
}
```

### Comparison of Methods

| Method | Speed | Reversible | Version Controlled | Selective |
|--------|-------|------------|-------------------|-----------|
| Disable in UI | Instant | ✓ Easy | ✗ No | ✗ All or nothing |
| Comment in Jenkinsfile | Next commit | ✓ Easy | ✓ Yes | ✓ Per-trigger |
| Branch-specific logic | Next commit | ✓ Moderate | ✓ Yes | ✓ Very flexible |

### Important Notes

⚠️ **When triggers are disabled:**
- Manual builds still work normally
- Parameterized builds still work
- Build history is preserved
- The pipeline configuration remains intact

⚠️ **After re-enabling triggers:**
- SCM polling will resume checking for changes immediately
- Scheduled builds will wait for the next scheduled time
- No "catch-up" builds are performed for missed triggers

⚠️ **Best practices:**
- Document why you're disabling triggers (in commit message or ticket)
- Set a reminder to re-enable triggers if disabled for extended periods
- Consider using Method 2 for transparency to the team
- Test that triggers work correctly after re-enabling

## Monitoring and Troubleshooting

### Check Build History

```bash
# View recent builds
http://jenkins:8081/job/TaskActivity-Pipeline/

# Check scheduled builds
# Look for builds with "TimerTrigger" cause

# Check SCM builds
# Look for builds with "SCMTrigger" cause
```

### Debug Scheduled Deployment Logic

Add to pipeline console output:

```groovy
echo "Last Build Number: ${lastBuildNumber}"
echo "Last Deploy Number: ${lastDeployNumber}"
echo "Should Deploy: ${shouldDeploy}"
```

### Common Issues

**Issue:** Scheduled deployment not running

- **Check:** Cron expression in pipeline
- **Check:** Jenkins server time vs. expected time
- **Fix:** Verify `cron('0 16 * * *')` in pipeline

**Issue:** SCM builds not triggering

- **Check:** "Lightweight checkout" is UNCHECKED in job configuration (most common cause)
- **Check:** Poll SCM configuration in Jenkinsfile
- **Check:** Git repository connectivity
- **Check:** View "Git Polling Log" in Jenkins job menu for errors
- **Fix:** Verify `pollSCM('H/5 * * * *')` in pipeline
- **Fix:** Uncheck "Lightweight checkout" in Pipeline → SCM configuration

**Issue:** Deployment runs when it shouldn't

- **Check:** `getLastSuccessfulBuildNumber()` function logic
- **Check:** Build parameter values in history
- **Fix:** Review last 50 builds for parameter consistency

**Issue:** NO_CACHE not working for SCM builds

- **Check:** `env.BUILD_NO_CACHE` value in console
- **Check:** Branch name detection logic
- **Fix:** Verify `env.GIT_BRANCH?.contains('main')` matches your branch name

## Security Considerations

1. **API Tokens:** Jenkins API token stored in credentials
2. **AWS Credentials:** Stored in Jenkins credentials, never in code
3. **Build Isolation:** Each build runs in isolated workspace
4. **Parameter Validation:** Production builds cannot skip tests
5. **Automated Rollback:** Available through manual trigger

## Best Practices

1. **Monitor Scheduled Builds:** Review skipped deployments to ensure logic is working
2. **Keep Build History:** Jenkins retains last 30 builds for analysis
3. **Use Manual Trigger Flag:** Always set when doing manual deploys
4. **Review Console Output:** Check deployment decision reasoning
5. **Test in Dev First:** Validate pipeline changes in dev environment
6. **Document Changes:** Update this guide when modifying pipeline logic

## Future Enhancements

Potential improvements to consider:

1. **Slack/Email Notifications:** Add when scheduled deployments are skipped
2. **Build Quality Gates:** Integrate SonarQube checks before deployment
3. **Deployment Windows:** Add time-based deployment restrictions
4. **Multi-Environment:** Deploy to dev/staging before production
5. **Canary Deployments:** Gradual rollout with automatic rollback
6. **Blue-Green Deployments:** Zero-downtime deployments
7. **Approval Gates:** Require manual approval for production deploys

## References

- [Jenkinsfile](../Jenkinsfile)
- [CloudFormation Templates](../cloudformation/)
- [Docker Configuration](../Dockerfile)
- [AWS Deployment Guide](../aws/AWS_Deployment.md)

---

**Note:** This pipeline configuration requires Jenkins 2.x or higher with Pipeline support.
