# CSV Import User Guide

## Overview

The CSV Import feature allows you to bulk load TaskActivity, Expense, and DropdownValue records into the database using CSV (Comma-Separated Values) files. This is much more efficient than manual entry, especially for large datasets or when migrating from other systems.

**Author:** Dean Ammons  
**Date:** January 2026

---

## Features

-   ✅ Bulk import of TaskActivity, Expense, and DropdownValue records
-   ✅ Automatic validation of all records before import
-   ✅ Individual record processing with duplicate detection
-   ✅ Flexible date format support (for TaskActivity and Expense)
-   ✅ Detailed error reporting for failed records
-   ✅ **Duplicate handling: Re-importing the same data file will skip existing records without errors**
-   ✅ RESTful API endpoints for programmatic access
-   ✅ Role-based access control (ADMIN and MANAGER only)

---

## Important: Duplicate Record Handling

**The CSV import automatically skips duplicate records without failing the import.**

### How Duplicates Are Detected

**TaskActivity records** are considered duplicates if they have the same combination of:

-   `taskdate`
-   `client`
-   `project`
-   `phase`
-   `details`
-   `username`

**Expense records** are considered duplicates if they have the same combination of:

-   `expense_date`
-   `client`
-   `project`
-   `expense_type`
-   `expense_amount`
-   `username`

**DropdownValue records** are considered duplicates if they have the same combination of:

-   `category`
-   `subcategory`
-   `itemvalue`

### What Happens When Duplicates Are Found

-   ✅ The duplicate record is **silently skipped**
-   ✅ The import **continues processing** remaining records
-   ✅ No error is reported for duplicate records
-   ✅ Success count reflects **only newly inserted** records

### Example Scenario

If you import a file with 100 records:

-   **First import:** All 100 records are new → Success: 100, Errors: 0
-   **Second import (same file):** All 100 records already exist → Success: 0, Errors: 0 (duplicates skipped)
-   **Third import (50 new, 50 duplicates):** Success: 50, Errors: 0 (50 duplicates skipped)

This behavior allows you to:

-   **Re-run imports safely** without worrying about duplicate key errors
-   **Resume failed imports** by re-importing the entire file
-   **Merge data** from multiple sources with overlapping records

---

## Quick Start

### Method 1: Using cURL (Command Line)

**Import TaskActivity records:**

```bash
curl -X POST http://localhost:8080/api/import/taskactivities \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@taskactivity-import-template.csv"
```

**Import Expense records:**

```bash
curl -X POST http://localhost:8080/api/import/expenses \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@expense-import-template.csv"
```

### Method 2: Using PowerShell

**Import TaskActivity records:**

```powershell
$headers = @{
    "Authorization" = "Bearer YOUR_JWT_TOKEN"
}

$filePath = "C:\path\to\taskactivity-import-template.csv"
$uri = "http://localhost:8080/api/import/taskactivities"

$form = @{
    file = Get-Item -Path $filePath
}

Invoke-RestMethod -Uri $uri -Method Post -Headers $headers -Form $form
```

**Import Expense records:**

```powershell
$headers = @{
    "Authorization" = "Bearer YOUR_JWT_TOKEN"
}

$filePath = "C:\path\to\expense-import-template.csv"
$uri = "http://localhost:8080/api/import/expenses"

$form = @{
    file = Get-Item -Path $filePath
}

Invoke-RestMethod -Uri $uri -Method Post -Headers $headers -Form $form
```

### Method 3: Using Python

```python
import requests

# Get your JWT token first
token = "YOUR_JWT_TOKEN"

# Import TaskActivity
with open('taskactivity-import-template.csv', 'rb') as f:
    response = requests.post(
        'http://localhost:8080/api/import/taskactivities',
        headers={'Authorization': f'Bearer {token}'},
        files={'file': f}
    )
    print(response.json())

# Import Expense
with open('expense-import-template.csv', 'rb') as f:
    response = requests.post(
        'http://localhost:8080/api/import/expenses',
        headers={'Authorization': f'Bearer {token}'},
        files={'file': f}
    )
    print(response.json())
```

---

## CSV File Formats

### Column Name Flexibility

The CSV parser is flexible with column names and accepts common variations:

-   **Database column names** (recommended): `taskdate`, `taskhours`, `expense_date`, `expense_type`, `payment_method`, `reference_number`, `expense_status`
-   **Alternative formats**: `taskDate`, `task_date`, `hours`, `expenseDate`, `expense-date` (all case-insensitive)

**Recommendation**: Use the actual database column names (lowercase with underscores where applicable) for consistency with the database schema.

### TaskActivity CSV Format

**Required Headers:**

```
taskdate,client,project,phase,taskhours,taskid,taskname,details,username
```

**Field Specifications:**

| Field     | Required | Type    | Max Length | Notes                                  |
| --------- | -------- | ------- | ---------- | -------------------------------------- |
| taskdate  | Yes      | Date    | -          | Multiple formats supported (see below) |
| client    | Yes      | String  | 255        | Client name                            |
| project   | Yes      | String  | 255        | Project name                           |
| phase     | Yes      | String  | 255        | Phase or activity type                 |
| taskhours | Yes      | Decimal | -          | Range: 0.01 to 24.00                   |
| taskid    | No       | String  | 50         | External reference / ticket number     |
| taskname  | No       | String  | 255        | Short label or title for the task      |
| details   | No       | String  | 255        | Additional details or notes            |
| username  | Yes      | String  | 50         | User who performed the work            |

**Example File:**

```csv
taskdate,client,project,phase,taskhours,taskid,taskname,details,username
2026-01-15,Acme Corp,Website Redesign,Development,8.00,TA-001,Implement auth,Implemented user authentication,john.doe
2026-01-15,Tech Solutions,Mobile App,Testing,6.50,TA-042,Regression suite,Performed regression testing,jane.smith
01/16/2026,Acme Corp,Website Redesign,Design,4.00,,,Created mockups for dashboard,john.doe
```

See [taskactivity-import-template.csv](taskactivity-import-template.csv) for a complete example.

---

### Expense CSV Format

**Required Headers:**

```
username,client,project,expense_date,expense_type,description,amount,currency,payment_method,vendor,reference_number,expense_status
```

**Field Specifications:**

| Field            | Required | Type    | Max Length | Default | Notes                               |
| ---------------- | -------- | ------- | ---------- | ------- | ----------------------------------- |
| username         | Yes      | String  | 50         | -       | User who incurred the expense       |
| client           | Yes      | String  | 50         | -       | Client name                         |
| project          | No       | String  | 50         | -       | Project name (optional)             |
| expense_date     | Yes      | Date    | -          | -       | Multiple formats supported          |
| expense_type     | Yes      | String  | 50         | -       | E.g., Travel, Meals, Lodging        |
| description      | Yes      | String  | 255        | -       | Expense description                 |
| amount           | Yes      | Decimal | -          | -       | Must be > 0.01                      |
| currency         | No       | String  | 3          | USD     | ISO currency code                   |
| payment_method   | Yes      | String  | 50         | -       | E.g., Corporate Card, Personal Card |
| vendor           | No       | String  | 100        | -       | Vendor/merchant name                |
| reference_number | No       | String  | 50         | -       | Receipt or transaction number       |
| expense_status   | No       | String  | 50         | Draft   | E.g., Draft, Submitted, Approved    |

**Example File:**

```csv
username,client,project,expense_date,expense_type,description,amount,currency,payment_method,vendor,reference_number,expense_status
john.doe,Acme Corp,Website Redesign,2026-01-15,Travel,Flight to client site,450.00,USD,Corporate Card,United Airlines,UA12345,Submitted
jane.smith,Tech Solutions,Mobile App,2026-01-15,Meals,Client dinner meeting,125.50,USD,Personal Card,The Steakhouse,INV-001,Draft
```

See [expense-import-template.csv](expense-import-template.csv) for a complete example.

---

## Supported Date Formats

The import service supports multiple date formats for flexibility:

| Format     | Example     | Description                          |
| ---------- | ----------- | ------------------------------------ |
| ISO 8601   | 2026-01-15  | International standard (recommended) |
| US Format  | 01/15/2026  | Month/Day/Year with leading zeros    |
| Short US   | 1/15/2026   | Month/Day/Year without leading zeros |
| Date-Month | 15-Jan-2026 | Day-Month abbreviation-Year          |

**Recommendation:** Use ISO 8601 format (YYYY-MM-DD) for consistency and to avoid ambiguity.

---

## API Endpoints

### Import TaskActivity Records

**Endpoint:** `POST /api/import/taskactivities`  
**Content-Type:** `multipart/form-data`  
**Required Role:** ADMIN or MANAGER  
**Parameter:** `file` (CSV file)

**Success Response (200 OK):**

```json
{
    "success": true,
    "message": "Import completed",
    "totalProcessed": 100,
    "successCount": 100,
    "errorCount": 0
}
```

**Partial Success Response (206 Partial Content):**

```json
{
  "success": true,
  "message": "Import completed",
  "totalProcessed": 100,
  "successCount": 95,
  "errorCount": 5,
  "errors": [
    "Line 10: Validation failed: hours: Hours must be greater than 0",
    "Line 25: Parse error: Unable to parse date from provided values",
    ...
  ]
}
```

---

### Import Expense Records

**Endpoint:** `POST /api/import/expenses`  
**Content-Type:** `multipart/form-data`  
**Required Role:** ADMIN or MANAGER  
**Parameter:** `file` (CSV file)

**Response format:** Same as TaskActivity import

---

### Import DropdownValue Records

**Endpoint:** `POST /api/import/dropdownvalues`  
**Content-Type:** `multipart/form-data`  
**Required Role:** ADMIN or MANAGER  
**Parameter:** `file` (CSV file)

**Purpose:** Import dropdown values for client, project, phase, and other categorized data used throughout the application.

**CSV Format:**
```csv
category,subcategory,itemvalue,displayorder,isactive
TASK,CLIENT,Acme Corporation,1,true
TASK,CLIENT,XYZ Industries,2,true
TASK,PROJECT,Website Redesign,1,true
TASK,PROJECT,Cloud Migration,2,true
TASK,PHASE,Requirements,1,true
TASK,PHASE,Design,2,true
```

**Field Specifications:**
- **category** (Required): Category name, max 50 chars - automatically converted to uppercase
- **subcategory** (Required): Subcategory name, max 50 chars
- **itemvalue** (Required): The actual dropdown value, max 100 chars
- **displayorder** (Optional): Display order (integer), defaults to 0
- **isactive** (Optional): Active status (true/false), defaults to true

**Response format:** Same as TaskActivity import

**Note:** Duplicates are detected based on the combination of category, subcategory, and itemvalue. Re-importing the same values will skip existing records without errors.

---

### Get Import Templates

**TaskActivity Template:**

```bash
GET /api/import/taskactivities/template
```

**Expense Template:**

```bash
GET /api/import/expenses/template
```

**DropdownValue Template:**

```bash
GET /api/import/dropdownvalues/template
```

Returns template structure, example data, and field specifications.

---

## Error Handling

### Common Errors and Solutions

| Error Message                                            | Cause                              | Solution                                |
| -------------------------------------------------------- | ---------------------------------- | --------------------------------------- |
| "File is empty"                                          | Empty CSV file uploaded            | Ensure CSV file has header row and data |
| "File must be a CSV file"                                | Wrong file type                    | Only .csv files are accepted            |
| "Validation failed: hours: Hours must be greater than 0" | Invalid hours value                | Ensure hours are between 0.01 and 24.00 |
| "Parse error: Unable to parse date from provided values" | Invalid date format                | Use supported date format (see above)   |
| "Validation failed: client: Client is required"          | Missing required field             | All required fields must have values    |
| "Invalid number format"                                  | Non-numeric value in numeric field | Ensure amount/hours are valid numbers   |

### Partial Import Behavior

-   Records are validated **before** being saved to the database
-   Invalid records are skipped with error messages
-   Valid records are saved successfully
-   Import continues even if some records fail
-   Response includes detailed error list for failed records

---

## Best Practices

### Preparing Your CSV File

1. **Use UTF-8 Encoding:** Save your CSV file with UTF-8 encoding to support special characters
2. **Include Header Row:** First row must contain column names (case-insensitive)
3. **Validate Data First:** Check for required fields and correct formats before uploading
4. **Test with Small Files:** Test with a small sample (5-10 records) before bulk import
5. **Handle Quoted Fields:** Use quotes for fields containing commas: `"Smith, John"`

### Performance Tips

-   Files are processed in batches of 100 records for optimal performance
-   Large files (10,000+ records) may take several seconds to process
-   Database transaction is committed per batch for reliability
-   Monitor the response for any errors and re-import failed records after correction

### Data Validation

Before importing, verify:

-   ✅ All required fields are populated
-   ✅ Date formats are consistent
-   ✅ Numeric values are in correct format (use period for decimal: `8.50`)
-   ✅ Field lengths don't exceed maximum
-   ✅ Usernames exist in the system

---

## Security Considerations

-   **Authentication Required:** All import endpoints require valid JWT token
-   **Authorization:** Only users with ADMIN or MANAGER roles can import data
-   **Validation:** All records are validated against entity constraints
-   **Audit Trail:** Import actions are logged for security auditing
-   **File Size Limits:** Spring Boot default max file size is 1MB (configurable)

### Increasing File Size Limit

Add to `application.properties`:

```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## Troubleshooting

### Import Fails with "Unauthorized"

**Cause:** Missing or invalid JWT token  
**Solution:** Ensure you include valid authentication token in Authorization header

```bash
curl -X POST http://localhost:8080/api/import/taskactivities \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@data.csv"
```

### Import Fails with "Access Denied"

**Cause:** User doesn't have ADMIN or MANAGER role  
**Solution:** Use an account with appropriate permissions

### Some Records Import, Others Fail

**Cause:** Validation errors in specific records  
**Solution:** Check the `errors` array in the response, fix the data, and re-import failed records

### Date Parse Errors

**Cause:** Unsupported date format  
**Solution:** The import service supports the following date formats:

-   `YYYY-MM-DD` (ISO format, e.g., `2018-06-11`)
-   `MM/DD/YYYY` (US format, e.g., `06/11/2018`)
-   `M/D/YYYY` (Short US format, e.g., `6/11/2018`)
-   `DD-MMM-YYYY` (With month abbreviation, e.g., `11-Jun-2018`)

If your dates are in a different format, convert them to one of the supported formats above.

---

## Advanced Usage

### Scripting Bulk Imports

You can automate imports using shell scripts or scheduled tasks:

**PowerShell Script Example:**

```powershell
# bulk-import.ps1
param(
    [string]$Token,
    [string]$DataDirectory
)

$headers = @{
    "Authorization" = "Bearer $Token"
}

# Import all CSV files in directory
Get-ChildItem -Path $DataDirectory -Filter "*.csv" | ForEach-Object {
    Write-Host "Importing $($_.Name)..."

    $uri = if ($_.Name -like "expense*") {
        "http://localhost:8080/api/import/expenses"
    } else {
        "http://localhost:8080/api/import/taskactivities"
    }

    $form = @{ file = Get-Item -Path $_.FullName }
    $result = Invoke-RestMethod -Uri $uri -Method Post -Headers $headers -Form $form

    Write-Host "Success: $($result.successCount), Errors: $($result.errorCount)"
}
```

**Usage:**

```powershell
.\bulk-import.ps1 -Token "your_jwt_token" -DataDirectory "C:\imports"
```

---

## Getting Help

### Template Files

Download example CSV templates:

-   [taskactivity-import-template.csv](taskactivity-import-template.csv)
-   [expense-import-template.csv](expense-import-template.csv)

### API Documentation

Visit Swagger UI for interactive API documentation:

```
http://localhost:8080/swagger-ui.html
```

### Additional Resources

-   [User Guide](User_Guide.md) - General application usage
-   [Administrator User Guide](Administrator_User_Guide.md) - Admin features
-   [Developer Guide](Developer_Guide.md) - Technical implementation details

---

## Changelog

**Version 1.0 - January 2026**

-   Initial release of CSV import functionality
-   Support for TaskActivity and Expense entities
-   Batch processing with validation
-   Multiple date format support
-   RESTful API with authentication

---

## Notes

-   This feature is designed for bulk data loading and migration scenarios
-   For day-to-day data entry, use the web UI for better user experience
-   CSV import is transactional per batch, ensuring data consistency
-   Maximum file size is configurable (default: 1MB)
-   Import performance: approximately 1000-2000 records per second

---

_For questions or issues, contact your system administrator._
