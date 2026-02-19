# Getting Started with CSV Import - Quick Guide

**For: Dean Ammons**  
**Date: January 12, 2026**

---

## You Now Have Three Ways to Import CSV Data

### 1. PowerShell Script (EASIEST!) 🎯

```powershell
# Navigate to your project
cd C:\Users\deana\GitHub\ActivityTracking

# Import a CSV file - script will auto-detect type and handle authentication
.\scripts\Import-CsvData.ps1 -FilePath "mydata.csv" -Username "admin"

# That's it! The script handles everything:
# - Prompts for password
# - Gets JWT token automatically
# - Detects if it's TaskActivity or Expense based on filename
# - Shows detailed progress
# - Reports success/errors
```

### 2. Direct PowerShell Commands

```powershell
# Get your token first
$creds = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$token = (Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
    -Method Post -ContentType "application/json" -Body $creds).token

# Import TaskActivity
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/import/taskactivities" `
    -Method Post -Headers $headers -Form @{ file = Get-Item "taskactivity-data.csv" }

# Import Expense
Invoke-RestMethod -Uri "http://localhost:8080/api/import/expenses" `
    -Method Post -Headers $headers -Form @{ file = Get-Item "expense-data.csv" }
```

### 3. cURL (if you prefer)

```bash
# Get token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Import
curl -X POST http://localhost:8080/api/import/taskactivities \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@taskactivity-data.csv"
```

---

## CSV File Format

### TaskActivity CSV

```csv
taskdate,client,project,phase,taskhours,taskid,taskname,details,username
2026-01-15,Acme Corp,Website,Development,8.00,TA-001,Implement login,Coding work,john.doe
01/16/2026,Tech Solutions,Mobile,Testing,7.00,,,Testing,jane.smith
```

### Expense CSV

```csv
username,client,project,expense_date,expense_type,description,amount,currency,payment_method,vendor,reference_number,expense_status
john.doe,Acme Corp,Website,2026-01-15,Travel,Flight,450.00,USD,Corporate Card,United,ABC123,Submitted
```

---

## Example Templates Available

I created example templates you can copy and modify:

-   **TaskActivity:** `docs/taskactivity-import-template.csv`
-   **Expense:** `docs/expense-import-template.csv`

Just copy these, replace the data, and import!

---

## What Happens During Import

1. ✅ File is validated (must be CSV)
2. ✅ Each record is parsed and validated
3. ✅ Valid records are saved in batches of 100
4. ✅ Invalid records are reported with line numbers and reasons
5. ✅ You get a summary: X succeeded, Y failed

---

## Common Scenarios

### Scenario 1: Import from Excel

1. Save your Excel file as CSV (File → Save As → CSV)
2. Run: `.\scripts\Import-CsvData.ps1 -FilePath "data.csv" -Username "admin"`
3. Done!

### Scenario 2: Import Multiple Files

```powershell
Get-ChildItem -Path "C:\imports" -Filter "*.csv" | ForEach-Object {
    .\scripts\Import-CsvData.ps1 -FilePath $_.FullName -Username "admin"
}
```

### Scenario 3: Monthly Expense Reports

```powershell
# Save your expense report as CSV, then:
.\scripts\Import-CsvData.ps1 -FilePath "expenses_january.csv" -Type Expense -Username "admin"
```

---

## Troubleshooting

### "Unauthorized" Error

**Problem:** Missing or invalid token  
**Solution:** Make sure the application is running and credentials are correct

### "Access Denied"

**Problem:** User lacks permissions  
**Solution:** Use admin or manager account

### Date Parse Errors

**Problem:** Unsupported date format  
**Solution:** Use format: YYYY-MM-DD (e.g., 2026-01-15)

### Validation Errors

**Problem:** Missing required fields or invalid values  
**Solution:** Check error message for specific line and field, fix CSV and re-import

---

## Need Help?

📖 **Full Guide:** [docs/CSV_Import_User_Guide.md](docs/CSV_Import_User_Guide.md)  
📋 **Quick Reference:** [docs/CSV_Import_Quick_Reference.md](docs/CSV_Import_Quick_Reference.md)  
📄 **Implementation Summary:** [CSV_Import_Implementation_Summary.md](CSV_Import_Implementation_Summary.md)

---

## Pro Tips

💡 **Test First:** Import a small file (5-10 records) to verify format before bulk import  
💡 **Check Errors:** If you get errors, fix those records and re-import just them  
💡 **Use ISO Dates:** Format YYYY-MM-DD (2026-01-15) works best  
💡 **Quote Special Fields:** If field contains commas, wrap in quotes: `"Smith, John"`  
💡 **Save Token:** If doing multiple imports, save the token in a variable  
💡 **Use Script:** The PowerShell script (`Import-CsvData.ps1`) is the easiest method

---

## That's It!

You now have a powerful, easy way to bulk import data. No more tedious copy/paste!

**Most Common Command You'll Use:**

```powershell
.\scripts\Import-CsvData.ps1 -FilePath "mydata.csv" -Username "admin"
```

Happy importing! 🚀
