# Jenkins Workspace Cleanup - Quick Reference

## Problem Summary

Jenkins deployments were failing with "Failed to exec spawn helper" errors after WSL2 restarts. This was caused by corrupt/stale workspace directories in `/var/lib/jenkins/workspace/TaskActivity-Pipeline@script/` that Jenkins couldn't properly initialize.

## Solution Implemented

### 1. Updated Jenkinsfile

**Added workspace cleanup stage** at the beginning of each pipeline run:

- Cleans up empty or corrupted `@script` directories
- Cleans workspace while preserving `.git` for faster checkouts
- Added `skipDefaultCheckout(true)` option to prevent default checkout before cleanup

**Enhanced final cleanup** in the `always` block:

- Removes all `@script` and `@tmp` directories after build
- Cleans build artifacts (target, .mvn, node_modules)
- Uses `disableDeferredWipeout: true` for immediate cleanup

### 2. Created Automated Cleanup Scripts

**cleanup-jenkins-workspace.sh**

- Removes empty directories in workspace
- Cleans `@script` directories older than 7 days
- Cleans `@tmp` directories older than 7 days
- Provides dry-run mode for testing
- Generates statistics before and after cleanup

**setup-jenkins-cleanup-cron.sh**

- Sets up daily cron job at 2 AM
- Creates log directory at `/var/log/jenkins-cleanup`
- Configures automatic log rotation by date

### 3. Updated .gitignore

Added pattern to exclude cleanup log files: `scripts/*cleanup*.log`

## Manual Commands

### Test cleanup (dry-run mode)

```bash
wsl -u root /mnt/c/Users/deana/GitHub/ActivityTracking/Jenkins/cleanup-jenkins-workspace.sh true
```

### Run cleanup immediately

```bash
wsl -u root /mnt/c/Users/deana/GitHub/ActivityTracking/Jenkins/cleanup-jenkins-workspace.sh
```

### Check Jenkins status

```bash
wsl -u root systemctl status jenkins
```

### Restart Jenkins

```bash
wsl -u root systemctl restart jenkins
```

### View cleanup logs

```bash
wsl -u root ls -lh /var/log/jenkins-cleanup/
wsl -u root cat /var/log/jenkins-cleanup/cleanup-$(date +%Y%m%d).log
```

### View cron jobs

```bash
wsl -u root crontab -l
```

## Prevention Strategy

1. **Automatic cleanup via Jenkinsfile** - Every build cleans up before and after
2. **Daily cron job** - Runs at 2 AM to clean old directories
3. **Workspace preservation** - Keeps `.git` directory to speed up checkouts
4. **Monitoring** - Logs all cleanup operations for troubleshooting

## Troubleshooting

### If Jenkins still fails after restart

1. Check if workspace directories are accessible:
   
   ```bash
   wsl -u root ls -la /var/lib/jenkins/workspace/
   ```

2. Manually remove all @script directories:
   
   ```bash
   wsl -u root rm -rf /var/lib/jenkins/workspace/*@script
   wsl -u root rm -rf /var/lib/jenkins/workspace/*@tmp
   ```

3. Restart Jenkins:
   
   ```bash
   wsl -u root systemctl restart jenkins
   ```

### If cron job isn't running

1. Check if cron service is running:
   
   ```bash
   wsl -u root systemctl status cron
   ```

2. Start cron if needed:
   
   ```bash
   wsl -u root systemctl start cron
   ```

3. Re-run setup script:
   
   ```bash
   wsl -u root /mnt/c/Users/deana/GitHub/ActivityTracking/Jenkins/setup-jenkins-cleanup-cron.sh
   ```

## Impact Assessment

- **Deployment reliability**: Prevents failures after WSL2 restarts
- **Build performance**: Faster checkouts by preserving `.git` directory
- **Disk usage**: Automatic cleanup prevents workspace bloat
- **Maintenance**: Reduced manual intervention required

## Next Steps

1. Monitor the next few scheduled deploys (8am, 12pm, 4pm) to ensure they succeed
2. Review cleanup logs after a week to verify cron job is working
3. Adjust retention period (currently 7 days) if needed

---

**Author**: Dean Ammons  
**Date**: February 2026  
**Related Issue**: #270 - Jenkins deployment failures after WSL2 restart
