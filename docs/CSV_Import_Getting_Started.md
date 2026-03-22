<!--
  Description: Quick-start guide for bulk CSV import of task and expense data.

  Author: Dean Ammons
  Date: March 2026
-->

# Getting Started with CSV Import

## What This Guide Is For

CSV import is intended for users who need to load larger sets of task or expense
records without entering them one at a time through the application.

Typical use cases include:

- Migrating existing spreadsheet data into the system
- Loading monthly or historical task records
- Importing batches of expense data from another source
- Correcting or reloading structured data after cleanup

## Before You Start

Make sure all of the following are true before importing:

- The application is running and reachable
- You have an account with the required permissions, typically **ADMIN** or **MANAGER**
- Your file is saved in CSV format
- Your column headers match the expected template
- Dates use a supported format, with `YYYY-MM-DD` preferred

## Recommended Method

The recommended approach is the PowerShell script because it handles
authentication, file upload, and import type detection for you.

```powershell
cd C:\Users\deana\GitHub\ActivityTracking
.\scripts\Import-CsvData.ps1 -FilePath "mydata.csv" -Username "admin"
```

The script will:

- Prompt for your password
- Authenticate automatically
- Detect whether the file is for task activity or expenses
- Show progress and import results

## Other Import Methods

If you need more direct control, you can also import with API calls.

### PowerShell API Example

```powershell
$creds = @{ username = "admin"; password = "your-password" } | ConvertTo-Json
$token = (Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
    -Method Post -ContentType "application/json" -Body $creds).token

$headers = @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Uri "http://localhost:8080/api/import/taskactivities" `
    -Method Post -Headers $headers -Form @{ file = Get-Item "taskactivity-data.csv" }
```

### cURL Example

```bash
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"your-password"}' | jq -r '.token')

curl -X POST http://localhost:8080/api/import/taskactivities \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@taskactivity-data.csv"
```

## Supported CSV Types

### Task Activity CSV

```csv
taskdate,client,project,phase,taskhours,taskid,taskname,details,username
2026-01-15,Acme Corp,Website,Development,8.00,TA-001,Implement login,Coding work,john.doe
2026-01-16,Tech Solutions,Mobile,Testing,7.00,,,Testing,jane.smith
```

### Expense CSV

```csv
username,client,project,expense_date,expense_type,description,amount,currency,payment_method,vendor,reference_number,expense_status
john.doe,Acme Corp,Website,2026-01-15,Travel,Flight,450.00,USD,Corporate Card,United,ABC123,Submitted
```

## Templates

Use the provided templates as your starting point:

- `docs/taskactivity-import-template.csv`
- `docs/expense-import-template.csv`

Copy the template that matches your import type, replace the sample data, and
save the file as CSV before importing.

## What Happens During Import

When an import runs, the system:

1. Validates that the uploaded file is a CSV file.
2. Parses each row and checks required fields.
3. Validates values such as dates, amounts, and supported reference data.
4. Saves valid rows in batches.
5. Returns a summary showing successes and failures.

If any rows fail, the response identifies the affected lines so you can correct
them and re-import.

## Common Scenarios

### Import Data from Excel

1. Open the spreadsheet in Excel.
2. Save it as a CSV file.
3. Run the import script against the saved CSV file.

### Import Multiple Files

```powershell
Get-ChildItem -Path "C:\imports" -Filter "*.csv" | ForEach-Object {
    .\scripts\Import-CsvData.ps1 -FilePath $_.FullName -Username "admin"
}
```

### Import an Expense Batch Explicitly

```powershell
.\scripts\Import-CsvData.ps1 -FilePath "expenses_january.csv" -Type Expense -Username "admin"
```

## Troubleshooting

### Unauthorized

Make sure the application is running and that your credentials are valid.

### Access Denied

Your account likely does not have the required import permissions. Use an
authorized account or contact an administrator.

### Date Parse Errors

Use `YYYY-MM-DD` whenever possible. For example: `2026-01-15`.

### Validation Errors

Review the reported line number and field, correct the CSV data, and re-import
only the failed records if needed.

## Good Practices

- Test with a small file first before importing a large batch.
- Keep a copy of the original source file before cleanup or transformation.
- Use the provided templates instead of building the file format from scratch.
- Quote fields that contain commas.
- Prefer the PowerShell script unless you need API-level control.

## Related Documentation

- [docs/CSV_Import_User_Guide.md](docs/CSV_Import_User_Guide.md)
- [docs/CSV_Import_Quick_Reference.md](docs/CSV_Import_Quick_Reference.md)
- [docs/CSV_Import_Implementation_Summary.md](docs/CSV_Import_Implementation_Summary.md)
