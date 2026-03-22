<!--
  Description: Command-focused quick reference for CSV import operations.

  Author: Dean Ammons
  Date: March 2026
-->

# CSV Import Quick Reference

This page is a command and format summary for users who already understand the
CSV import workflow and need a fast reference while working.

For step-by-step setup, see [CSV Import Getting
Started](CSV_Import_Getting_Started.md). For complete format and API details,
see [CSV Import User Guide](CSV_Import_User_Guide.md).

## Prerequisites

Before using the commands below:

1. Make sure the application is running.
2. Use an account with the required import permissions, typically **ADMIN** or **MANAGER**.
3. Prepare a CSV file that matches the expected template.
4. Obtain a valid JWT token if you are not using the helper script.

---

## Quick Import Commands

### Using PowerShell Script (Recommended)

```powershell
# Import with automatic type detection
.\scripts\Import-CsvData.ps1 -FilePath "taskactivity-data.csv" -Username "admin"

# Import with explicit type
.\scripts\Import-CsvData.ps1 -FilePath "expense-data.csv" -Type Expense -Token "your_jwt_token"

# Import DropdownValues
.\scripts\Import-CsvData.ps1 -FilePath "dropdownvalue-data.csv" -Type DropdownValue -Username "admin"

# Batch import all CSV files in a directory
Get-ChildItem -Path "C:\imports" -Filter "*.csv" | .\scripts\Import-CsvData.ps1 -Username "admin"
```

### Using cURL

```bash
# Import TaskActivity
curl -X POST http://localhost:8080/api/import/taskactivities \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@taskactivity-data.csv"

# Import Expense
curl -X POST http://localhost:8080/api/import/expenses \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@expense-data.csv"

# Import DropdownValues
curl -X POST http://localhost:8080/api/import/dropdownvalues \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@dropdownvalue-data.csv"
```

### Using PowerShell Invoke-RestMethod

```powershell
$token = "YOUR_JWT_TOKEN"
$headers = @{ "Authorization" = "Bearer $token" }

# Import TaskActivity
Invoke-RestMethod -Uri "http://localhost:8080/api/import/taskactivities" `
  -Method Post -Headers $headers -Form @{ file = Get-Item "taskactivity-data.csv" }

# Import Expense
Invoke-RestMethod -Uri "http://localhost:8080/api/import/expenses" `
  -Method Post -Headers $headers -Form @{ file = Get-Item "expense-data.csv" }

# Import DropdownValues
Invoke-RestMethod -Uri "http://localhost:8080/api/import/dropdownvalues" `
  -Method Post -Headers $headers -Form @{ file = Get-Item "dropdownvalue-data.csv" }
```

---

## CSV Templates

### TaskActivity Template

```csv
taskdate,client,project,phase,taskhours,taskid,taskname,details,username
2026-01-15,Acme Corp,Website,Development,8.00,TA-001,Implement feature,Coding work,john.doe
```

**Required fields:** taskdate, client, project, phase, taskhours, username  
**Optional fields:** taskid (max 50 chars), taskname (max 255 chars), details

### Expense Template

```csv
username,client,project,expense_date,expense_type,description,amount,currency,payment_method,vendor,reference_number,expense_status
john.doe,Acme Corp,Website,2026-01-15,Travel,Flight,450.00,USD,Corporate Card,United,ABC123,Submitted
```

**Required fields:** username, client, expense_date, expense_type, description, amount, payment_method

### DropdownValue Template

```csv
category,subcategory,itemvalue,displayorder,isactive
TASK,CLIENT,Acme Corporation,1,true
TASK,PROJECT,Website Redesign,1,true
TASK,PHASE,Requirements,1,true
```

**Required fields:** category, subcategory, itemvalue  
**Optional fields:** displayorder (default: 0), isactive (default: true)  
**Note:** Category is automatically converted to uppercase

## Preferred Date Format

Use `YYYY-MM-DD` whenever possible. The import supports other date formats, but
ISO format is the safest and least ambiguous choice.

---

## Response Format

**Success:**

```json
{
    "success": true,
    "message": "Import completed",
    "totalProcessed": 100,
    "successCount": 100,
    "errorCount": 0
}
```

**Partial Success with Errors:**

```json
{
    "success": true,
    "message": "Import completed",
    "totalProcessed": 100,
    "successCount": 95,
    "errorCount": 5,
    "errors": [
        "Line 10: Validation failed: hours: Hours must be greater than 0",
        "Line 25: Parse error: Unable to parse date"
    ]
}
```

---

## Common Issues

| Problem           | Solution                                                                                            |
| ----------------- | --------------------------------------------------------------------------------------------------- |
| "Unauthorized"    | Include a valid JWT token in the Authorization header                                               |
| "Access Denied"   | Use an account with ADMIN or MANAGER permissions                                                    |
| Date parse errors | Prefer `YYYY-MM-DD`; other supported formats are described in the full guide                        |
| Validation errors | Check required fields, value constraints, and referenced data                                       |
| "Duplicate key"   | Duplicates are automatically skipped, so re-running the same file is safe                           |

## Duplicate Handling

- Re-importing the same CSV file skips existing records without errors.
- Success count shows only newly inserted records.
- Duplicate detection is based on entity-specific uniqueness rules.
- It is safe to rerun imports after fixing unrelated failures.

---

## Related Documentation

- [CSV Import Getting Started](CSV_Import_Getting_Started.md)
- [CSV Import User Guide](CSV_Import_User_Guide.md)
- [Administrator User Guide](Administrator_User_Guide.md)
