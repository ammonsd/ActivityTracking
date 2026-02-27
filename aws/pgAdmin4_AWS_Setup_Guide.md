# pgAdmin 4 Setup Guide — AWS RDS AmmoP1DB

**Audience:** New developers  
**Purpose:** Connect pgAdmin 4 to the AWS RDS PostgreSQL database for ad-hoc queries, schema browsing, and data inspection  
**Last Updated:** February 2026

---

## How It Works

The RDS database is **not publicly accessible** — it only accepts connections from within the AWS VPC
(from the running ECS application container). Direct TCP connections from a developer laptop are
blocked at the network level by design.

The solution is an **SSM port-forwarding tunnel**: a PowerShell script relays your local port 15432
through the running ECS container to RDS port 5432. pgAdmin connects to `127.0.0.1:15432` and
everything works exactly like connecting to a local database.

```
Your laptop                  AWS VPC
pgAdmin → localhost:15432 → [ECS Container] → RDS :5432
              (SSM Tunnel via AWS Session Manager)
```

---

## Part 1 — One-Time Setup

Complete these steps once. After setup, connecting daily is just two steps (start tunnel → open pgAdmin).

---

### Step 1 — Install pgAdmin 4

Download and install the latest version for Windows:

**<https://www.pgadmin.org/download/pgadmin-4-windows/>**

Accept all defaults during installation. Launch pgAdmin 4 at least once to complete initial setup
and set your pgAdmin master password when prompted.

---

### Step 2 — Install AWS CLI v2

Check if it is already installed:

```powershell
aws --version
```

If not installed, download from:

**<https://awscli.amazonaws.com/AWSCLIV2.msi>**

After installation, close and reopen PowerShell, then verify:

```powershell
aws --version
# Expected: aws-cli/2.x.x ...
```

---

### Step 3 — Configure AWS CLI credentials

You need AWS access keys for the `DEAN` IAM user (or your own IAM user if one has been created for you).
Obtain your Access Key ID and Secret Access Key from the team administrator.

```powershell
aws configure
```

Enter when prompted:

| Prompt                | Value           |
| --------------------- | --------------- |
| AWS Access Key ID     | _(your key)_    |
| AWS Secret Access Key | _(your secret)_ |
| Default region name   | `us-east-1`     |
| Default output format | `json`          |

Verify it works:

```powershell
aws sts get-caller-identity
```

You should see your account ID and user ARN — no errors.

---

### Step 4 — Install the Session Manager plugin

The SSM plugin is a separate download required for port-forwarding. Download and run the installer:

**<https://s3.amazonaws.com/session-manager-downloads/plugin/latest/windows/SessionManagerPluginSetup.exe>**

After installation, **close and reopen PowerShell**, then verify:

```powershell
session-manager-plugin --version
```

You should see a version number. If the command is not found, close all PowerShell windows and try again.

---

### Step 5 — Verify IAM permissions

The tunnel script requires `ssm:StartSession` permission. Verify your IAM user has the
`TaskActivityDeveloperPolicy` attached, which includes the required `SSMSessionAccess` statement.

To confirm, run:

```powershell
aws ssm describe-sessions --state Active --region us-east-1
```

If this returns a result (even an empty list) rather than an `AccessDenied` error, your permissions
are correct.

If you receive `AccessDenied`, ask the administrator to add the `SSMSessionAccess` block to your
IAM policy. The required actions are listed in `aws/IAM_Permissions_Reference.md` under
**SSM Sessions**.

---

### Step 6 — Obtain the database password

The `taskactivity_readonly` password is not stored in AWS Secrets Manager — it was created
directly in the database. **Ask the team administrator for the `taskactivity_readonly` password.**

Store it securely (e.g., in your password manager). You will enter it in pgAdmin once and pgAdmin
will remember it.

> **Note:** AWS Secrets Manager (`taskactivity/database/credentials`) holds only the `postgres`
> master password. Do not use that here.

---

### Step 7 — Register the server in pgAdmin 4

> **Do this while the tunnel is running** (see Part 2 — Step 1 below). Start the tunnel first,
> then come back and complete this step.

In pgAdmin 4:

1. In the left panel, right-click **Servers → Register → Server**
2. Fill in the tabs as follows:

**General tab**

| Field | Value                           |
| ----- | ------------------------------- |
| Name  | `AWS RDS AmmoP1DB (via tunnel)` |

**Connection tab**

| Field                | Value                                        |
| -------------------- | -------------------------------------------- |
| Host name/address    | `127.0.0.1`                                  |
| Port                 | `15432`                                      |
| Maintenance database | `AmmoP1DB`                                   |
| Username             | `taskactivity_readonly`                      |
| Password             | _(enter the taskactivity_readonly password)_ |
| Save password        | ✅ Yes                                       |

3. Click **Save**

pgAdmin will connect and you will see `AmmoP1DB` appear under Servers in the left panel.
Expand **Databases → AmmoP1DB → Schemas → public → Tables** to browse the schema.

> **SSL**: Leave all SSL settings at the pgAdmin defaults. The SSM tunnel handles encryption
> transparently — no additional SSL configuration is needed in pgAdmin.

---

## Part 2 — Daily Use

After the one-time setup is complete, connecting each day takes about 10 seconds.

---

### Step 1 — Start the tunnel

Open a PowerShell window and run from the project root:

```powershell
.\scripts\Start-RdsTunnel.ps1
```

The script will:

1. Verify the Session Manager plugin is installed
2. Confirm local port 15432 is free
3. Find the currently running ECS task
4. Print your pgAdmin connection settings
5. Start the tunnel and wait

**Leave this window open** the entire time you are using pgAdmin. You will see output like:

```
========================================
  Starting tunnel: localhost:15432  ->  taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com:5432
========================================

Starting session with SessionId: Dean-...
Port 15432 opened for sessionId Dean-...
Waiting for connections...
```

---

### Step 2 — Open pgAdmin and connect

Open pgAdmin 4. In the left panel, click the server you registered (`AWS RDS AmmoP1DB (via tunnel)`).
It will connect immediately.

From here you can:

- **Browse the schema** — Expand Databases → AmmoP1DB → Schemas → public → Tables
- **View table data** — Right-click any table → View/Edit Data → All Rows
- **Run queries** — Tools → Query Tool, then write and execute SQL
- **Inspect columns** — Right-click any table → Properties → Columns tab
- **View indexes and constraints** — Expand the table node in the tree

---

### Step 3 — Close the tunnel when done

Return to the PowerShell tunnel window and press **Ctrl+C**.

pgAdmin will show a disconnected indicator on the server icon. The server definition is saved —
next time just start the tunnel again and click Connect.

---

## Access Levels

Two database users are available. Use the minimum access required for your task.

| User                    | Access                   | When to use                                                      |
| ----------------------- | ------------------------ | ---------------------------------------------------------------- |
| `taskactivity_readonly` | SELECT only (all tables) | Ad-hoc queries, data inspection, reporting — use this by default |
| `postgres`              | Full read/write          | Only when data corrections are needed — requires admin approval  |

Permissions are enforced at the PostgreSQL engine level. Even if you type a write statement in a
read-only session, the database will reject it — it is not just a convention.

To register a second pgAdmin server using the `postgres` admin account, repeat Step 7 of Part 1
with username `postgres` and the admin password. Run the tunnel with `-AdminUser` to see the
admin credentials displayed in the terminal output:

```powershell
.\scripts\Start-RdsTunnel.ps1 -AdminUser
```

---

## Troubleshooting

### "session-manager-plugin not found"

The Session Manager plugin is not installed, or PowerShell has not been restarted since installation.

1. Install from the link in Step 4
2. Close **all** PowerShell windows
3. Open a new PowerShell window and run the script again

---

### "Port 15432 is already in use"

Another tunnel session is already running (or something else is using that port).

Option A — Find and close the existing tunnel PowerShell window.

Option B — Use a different local port:

```powershell
.\scripts\Start-RdsTunnel.ps1 -LocalPort 15433
```

Then update the pgAdmin server's port to `15433` under **Properties → Connection**.

---

### "No running ECS tasks found"

The application ECS service is not running. Either the service has been stopped to save costs
or a deployment is in progress.

Check status:

```powershell
aws ecs describe-services `
    --cluster taskactivity-cluster `
    --services taskactivity-service `
    --region us-east-1 `
    --query "services[0].[status,runningCount,desiredCount]" `
    --output table
```

Contact the administrator if the service is stopped.

---

### "TargetNotConnected"

The ECS task ID stored in the tunnel script's session no longer matches a running task — the task
was replaced by a new deployment or a restart.

Close the tunnel window and run `.\scripts\Start-RdsTunnel.ps1` again. The script resolves the
current task ID fresh on each run.

---

### pgAdmin shows "connection refused" or times out

The tunnel is not running or has been closed. Start it:

```powershell
.\scripts\Start-RdsTunnel.ps1
```

Then click the server in pgAdmin to reconnect.

---

### "AccessDenied" when starting the tunnel

Your IAM user is missing the `ssm:StartSession` permission. Ask the administrator to add the
`SSMSessionAccess` statement to your `TaskActivityDeveloperPolicy`. The required JSON block
is in `aws/taskactivity-developer-policy.json`.

---

## Quick-Reference Card

Print or bookmark this table for daily use.

| Task                   | Command / Setting                               |
| ---------------------- | ----------------------------------------------- |
| Start tunnel           | `.\scripts\Start-RdsTunnel.ps1`                 |
| Stop tunnel            | Ctrl+C in the tunnel window                     |
| pgAdmin host           | `127.0.0.1`                                     |
| pgAdmin port           | `15432`                                         |
| Database               | `AmmoP1DB`                                      |
| Read-only user         | `taskactivity_readonly`                         |
| Admin user             | `postgres`                                      |
| Different local port   | `.\scripts\Start-RdsTunnel.ps1 -LocalPort 5433` |
| Show admin credentials | `.\scripts\Start-RdsTunnel.ps1 -AdminUser`      |

---

## Related Files

| File                                     | Purpose                                                   |
| ---------------------------------------- | --------------------------------------------------------- |
| `scripts/Start-RdsTunnel.ps1`            | The tunnel script                                         |
| `scripts/connect-to-rds.ps1`             | CLI-based `psql` shell access (alternative to pgAdmin)    |
| `aws/RDS_Database_Documentation.md`      | Full RDS instance details and configuration               |
| `aws/IAM_Permissions_Reference.md`       | IAM permissions reference including SSMSessionAccess      |
| `aws/taskactivity-developer-policy.json` | Complete IAM policy JSON                                  |
| `docs/Administrator_User_Guide.md`       | Full admin guide including Direct Database Access section |
