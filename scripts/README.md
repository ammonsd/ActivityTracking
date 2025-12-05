# Scripts Directory

This directory contains utility scripts for managing the Task Activity application.

## Scripts

### set-env-values.ps1

**Purpose:** Loads environment variables from a `.env` file into the current PowerShell session.

**Features:**

-   Supports comments (`#`) and empty lines
-   Handles quoted values (`VARIABLE="value"` or `VARIABLE='value'`)
-   Optional override mode for existing environment variables
-   Error handling for missing files
-   Graceful warnings instead of failures

**Usage:**

```powershell
# Load from default .env file in current directory
.\scripts\set-env-values.ps1

# Load from specific file
.\scripts\set-env-values.ps1 -envFile "path/to/custom.env"

# Override existing environment variables
.\scripts\set-env-values.ps1 -overrideExisting $true
```

**Example .env file:**

```bash
# Email Configuration
MAIL_ENABLED=true
MAIL_USE_AWS_SDK=true
MAIL_FROM=noreply@taskactivitytracker.com
ADMIN_EMAIL=admin@yourdomain.com

# Database (optional - usually from AWS Secrets Manager)
# DB_USERNAME=admin
# DB_PASSWORD=secret
```

**Integration:**

This script is automatically called by `aws/deploy-aws.ps1` at startup to load configuration from the project root `.env` file. This keeps sensitive information out of version control and command history.

**Security:**

⚠️ **Important:** The `.env` file should never be committed to version control. It's already included in `.gitignore`.

## Creating New Scripts

When adding new scripts to this directory:

1. Add descriptive header comments
2. Document parameters and usage
3. Include error handling
4. Update this README
5. Set appropriate file permissions (executable if needed)
