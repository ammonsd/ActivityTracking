# Jenkins Token Renewal - Quick Start

**When**: Every 30 days (Set reminder for **February 16, 2026**)

**Where**: Run from `C:\Users\deana\GitHub\ActivityTracking`

**Commands** (copy-paste these 3 lines):

```powershell
# Get production secret, generate token, update Jenkins
$secretJson = aws secretsmanager get-secret-value --secret-id "taskactivity/jwt/secret" --region us-east-1 --query 'SecretString' --output text | ConvertFrom-Json; $env:JWT_SECRET = $secretJson.secret; .\scripts\generate-token.ps1
```

**Then**:

1. Copy the yellow token from script output (entire line ~300 characters)
2. Go to http://172.27.85.228:8081
3. Manage Jenkins → Credentials → System → Global → `jenkins-api-token` → Update
4. Paste token → Save
5. Test with a build

**Full Guide**: [jenkins/Jenkins_Token_Maintenance_Guide.md](jenkins/Jenkins_Token_Maintenance_Guide.md)
