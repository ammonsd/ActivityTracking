# CSV Import Quick Reference

**Quick commands for bulk importing data into your AWS database.**

---

## Prerequisites

1. Get your JWT token (login via API or copy from browser dev tools)
2. Prepare your CSV file (see templates in `docs/` folder)

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
taskdate,client,project,phase,taskhours,details,username
2026-01-15,Acme Corp,Website,Development,8.00,Coding work,john.doe
```

**Required fields:** taskdate, client, project, phase, taskhours, username

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
| "Unauthorized"    | Include valid JWT token in Authorization header                                                     |
| "Access Denied"   | User must have ADMIN or MANAGER role                                                                |
| Date parse errors | Use supported formats: `YYYY-MM-DD`, `MM/DD/YYYY`, `M/D/YYYY`, or `DD-MMM-YYYY` (e.g., 11-Jun-2018) |
| Validation errors | Check required fields and value constraints                                                         |
| "Duplicate key"   | ✅ **Not an issue!** Duplicates are automatically skipped (see note below)                          |

### 📌 Note: Duplicate Records Are Automatically Skipped

-   Re-importing the same CSV file will skip existing records without errors
-   Success count shows only **newly inserted** records
-   Duplicate detection based on unique constraints (taskdate + client + project + phase + details + username for TaskActivity)
-   Safe to re-run imports without worrying about duplicate key violations

---

## Full Documentation

For complete documentation including:

-   Detailed CSV format specifications
-   All supported date formats
-   Field validation rules
-   Advanced usage examples
-   Troubleshooting guide

See: [CSV Import User Guide](CSV_Import_User_Guide.md)

---

**Created:** January 2026  
**Author:** Dean Ammons
