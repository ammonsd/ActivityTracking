---
name: csv-bulk-import
description: "Automates CSV bulk import operations for TaskActivity and Expense records using the Import-CsvData.ps1 script with proper authentication and validation"
---

# CSV Bulk Import Skill

This skill helps with bulk CSV import operations for TaskActivity and Expense records.

## When to Use

- User wants to bulk import TaskActivity or Expense records
- User needs help with CSV formatting or templates
- User encounters CSV import errors

## Prerequisites Check

Before importing:

1. ✅ User has ADMIN or MANAGER role
2. ✅ JWT token is available or can be generated
3. ✅ CSV file follows correct template format
4. ✅ Database is accessible

## CSV Templates

**TaskActivity Template** (`docs/taskactivity-import-template.csv`):

```csv
Date,Hours,Client,Project,Phase,Description,Username
2026-01-15,8.0,Acme Corp,Website Redesign,Development,Implemented user authentication,admin
```

**Expense Template** (`docs/expense-import-template.csv`):

```csv
Date,Amount,Description,ExpenseType,Vendor,PaymentMethod,Username
2026-01-15,125.50,Hotel accommodation,Travel,Marriott,CreditCard,admin
```

## Import Process

### Step 1: Validate CSV File

Check for:

- Correct headers (case-sensitive)
- Date format: YYYY-MM-DD
- Hours/Amount: Positive numbers
- Required fields: Not empty
- References: Client, Project, Phase, ExpenseType exist in DropdownValues

### Step 2: Generate/Obtain JWT Token

```powershell
# If needed, generate token
.\scripts\generate-token.ps1 -Username "admin" -Password "password"
```

### Step 3: Run Import Script

```powershell
.\scripts\Import-CsvData.ps1 `
    -CsvFilePath "path\to\import.csv" `
    -ImportType "TaskActivity" `
    -JwtToken "your-jwt-token" `
    -BaseUrl "http://localhost:8080"
```

### Step 4: Verify Import

- Check console output for success/error messages
- Review import log for details
- Query database to confirm records

## Common Issues and Solutions

**Issue: "Invalid date format"**

- Solution: Ensure dates are in YYYY-MM-DD format

**Issue: "Client not found"**

- Solution: Add missing DropdownValue or enable auto-creation in CsvImportService

**Issue: "Unauthorized (401)"**

- Solution: Token expired - generate new JWT token

**Issue: "Headers don't match template"**

- Solution: Verify CSV headers match template exactly (case-sensitive)

## Memory Bank References

- Check `ai/common-patterns.md` for CSV import code patterns
- Check `ai/project-overview.md` for CSV import feature details
- Check `docs/CSV_Import_User_Guide.md` for comprehensive documentation

## Output Format

Always provide:

1. Pre-import validation results
2. PowerShell command to run
3. Expected outcome
4. Verification steps
5. Rollback instructions if needed
