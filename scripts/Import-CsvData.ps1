<#
.SYNOPSIS
    Import CSV data into the Task Activity application database.

.DESCRIPTION
    This script provides an easy way to bulk import TaskActivity and Expense records
    from CSV files into the AWS database. It handles authentication, file validation,
    and provides detailed progress reporting.
    
    The script supports both TaskActivity and Expense imports, with automatic type
    detection based on file name or explicit type specification.

.PARAMETER FilePath
    Path to the CSV file to import. Supports wildcards for batch imports.

.PARAMETER Type
    Type of import: 'TaskActivity' or 'Expense'. If not specified, the script
    will attempt to determine the type from the filename.

.PARAMETER BaseUrl
    Base URL of the Task Activity API. Defaults to http://localhost:8080

.PARAMETER Token
    JWT authentication token. If not provided, the script will prompt for credentials
    and obtain a token automatically.

.PARAMETER Username
    Username for authentication (used when Token is not provided).

.PARAMETER Password
    Password for authentication (used when Token is not provided).

.EXAMPLE
    .\Import-CsvData.ps1 -FilePath "taskactivity-data.csv" -Type TaskActivity
    Import a TaskActivity CSV file using interactive authentication.

.EXAMPLE
    .\Import-CsvData.ps1 -FilePath "expense-data.csv" -Token "your_jwt_token"
    Import an Expense CSV file using a pre-obtained token.

.EXAMPLE
    .\Import-CsvData.ps1 -FilePath "C:\imports\*.csv" -Username "admin" -Password "pass123"
    Batch import all CSV files from a directory using provided credentials.

.EXAMPLE
    Get-ChildItem -Path "C:\imports" -Filter "*.csv" | ForEach-Object {
        .\Import-CsvData.ps1 -FilePath $_.FullName -Token $token
    }
    Import multiple CSV files using pipeline.

.NOTES
    Author: Dean Ammons
    Date: January 2026
    
    Prerequisites:
        - PowerShell 5.1 or higher (works with both Windows PowerShell 5.1 and PowerShell 6+)
        - Network access to Task Activity application
        - Valid user account with ADMIN or MANAGER role
    
    CSV File Requirements:
        - UTF-8 encoding
        - Header row with column names
        - Valid data in all required fields
        
    Date Format Options (for date fields in TaskActivity and Expense):
        - YYYY-MM-DD (recommended, ISO 8601): 2026-01-15
        - MM/dd/yyyy: 01/15/2026
        - M/d/yyyy: 1/15/2026
        - yyyy-MM-dd: 2026-01-15
        - dd-MMM-yyyy (English month abbreviations): 15-Jan-2026
        
    For detailed CSV format specifications, see CSV_Import_User_Guide.md
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, ValueFromPipeline = $true, ValueFromPipelineByPropertyName = $true)]
    [Alias("Path", "FullName")]
    [string]$FilePath,
    
    [Parameter(Mandatory = $false)]
    [ValidateSet('TaskActivity', 'Expense', 'DropdownValue', 'Auto')]
    [string]$Type = 'Auto',
    
    [Parameter(Mandatory = $false)]
    [string]$BaseUrl = 'http://localhost:8080',
    
    [Parameter(Mandatory = $false)]
    [string]$Token,
    
    [Parameter(Mandatory = $false)]
    [string]$Username,
    
    [Parameter(Mandatory = $false)]
    [string]$Password
)

begin {
    # Validate file exists
    function Test-FileExists {
        param([string]$Path)
        
        if (-not (Test-Path -Path $Path)) {
            throw "File not found: $Path"
        }
        
        if ((Get-Item -Path $Path).PSIsContainer) {
            throw "Path is a directory, not a file: $Path"
        }
        
        if (-not $Path.EndsWith('.csv', [System.StringComparison]::OrdinalIgnoreCase)) {
            Write-Warning "File does not have .csv extension: $Path"
        }
    }
    
    # Determine import type from filename
    function Get-ImportType {
        param([string]$FileName)
        
        if ($FileName -match 'task|activity') {
            return 'TaskActivity'
        }
        elseif ($FileName -match 'expense') {
            return 'Expense'
        }
        elseif ($FileName -match 'dropdown|value') {
            return 'DropdownValue'
        }
        else {
            return $null
        }
    }
    
    # Get authentication token
    function Get-AuthToken {
        param(
            [string]$BaseUrl,
            [string]$Username,
            [string]$Password
        )
        
        if (-not $Username) {
            $Username = Read-Host -Prompt "Username"
        }
        
        if (-not $Password) {
            $SecurePassword = Read-Host -Prompt "Password" -AsSecureString
            $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecurePassword)
            $Password = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        }
        
        $loginUrl = "$BaseUrl/api/auth/login"
        $body = @{
            username = $Username
            password = $Password
        } | ConvertTo-Json
        
        try {
            $response = Invoke-RestMethod -Uri $loginUrl -Method Post `
                -ContentType "application/json" -Body $body
            
            if ($response.token) {
                return $response.token
            }
            elseif ($response.accessToken) {
                return $response.accessToken
            }
            else {
                throw "No token found in response"
            }
        }
        catch {
            throw "Authentication failed: $($_.Exception.Message)"
        }
    }
    
    # Import CSV file
    function Import-CsvFile {
        param(
            [string]$FilePath,
            [string]$ImportType,
            [string]$BaseUrl,
            [string]$Token
        )
        
        $endpoint = switch ($ImportType) {
            'TaskActivity' { "$BaseUrl/api/import/taskactivities" }
            'Expense' { "$BaseUrl/api/import/expenses" }
            'DropdownValue' { "$BaseUrl/api/import/dropdownvalues" }
            default { throw "Unknown import type: $ImportType" }
        }
        
        Write-Host "Importing $ImportType from: $FilePath" -ForegroundColor Cyan
        
        # Check PowerShell version and use appropriate method
        if ($PSVersionTable.PSVersion.Major -ge 6) {
            # PowerShell 6+ supports -Form parameter
            $headers = @{
                "Authorization" = "Bearer $Token"
            }
            
            $form = @{
                file = Get-Item -Path $FilePath
            }
            
            try {
                $response = Invoke-RestMethod -Uri $endpoint -Method Post `
                    -Headers $headers -Form $form
                
                return $response
            }
            catch {
                $errorMessage = $_.Exception.Message
                
                # Try to extract detailed error message from response
                if ($_.ErrorDetails.Message) {
                    try {
                        $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json
                        if ($errorDetails.message) {
                            $errorMessage = $errorDetails.message
                        }
                    }
                    catch {
                        # If JSON parsing fails, use raw error details
                        $errorMessage = $_.ErrorDetails.Message
                    }
                }
                
                # Include HTTP status if available
                if ($_.Exception.Response) {
                    $statusCode = $_.Exception.Response.StatusCode.value__
                    $statusDescription = $_.Exception.Response.StatusDescription
                    $errorMessage = "HTTP $statusCode $statusDescription - $errorMessage"
                }
                
                throw "Import failed: $errorMessage"
            }
        }
        else {
            # PowerShell 5.1 - use Invoke-WebRequest with manual multipart/form-data
            $boundary = [System.Guid]::NewGuid().ToString()
            $LF = "`r`n"
            
            $fileBytes = [System.IO.File]::ReadAllBytes($FilePath)
            $fileName = [System.IO.Path]::GetFileName($FilePath)
            
            $bodyLines = @(
                "--$boundary",
                "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"",
                "Content-Type: text/csv$LF",
                [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileBytes),
                "--$boundary--$LF"
            ) -join $LF
            
            $headers = @{
                "Authorization" = "Bearer $Token"
                "Content-Type" = "multipart/form-data; boundary=$boundary"
            }
            
            try {
                $response = Invoke-RestMethod -Uri $endpoint -Method Post `
                    -Headers $headers -Body $bodyLines
                
                return $response
            }
            catch {
                $errorMessage = $_.Exception.Message
                
                # Try to extract detailed error message from response
                if ($_.ErrorDetails.Message) {
                    try {
                        $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json
                        if ($errorDetails.message) {
                            $errorMessage = $errorDetails.message
                        }
                    }
                    catch {
                        # If JSON parsing fails, use raw error details
                        $errorMessage = $_.ErrorDetails.Message
                    }
                }
                
                # Include HTTP status if available
                if ($_.Exception.Response) {
                    $statusCode = $_.Exception.Response.StatusCode.value__
                    $statusDescription = $_.Exception.Response.StatusDescription
                    $errorMessage = "HTTP $statusCode $statusDescription - $errorMessage"
                }
                
                throw "Import failed: $errorMessage"
            }
        }
    }
    
    # Display import results
    function Show-ImportResults {
        param([object]$Result)
        
        Write-Host "`nImport Results:" -ForegroundColor Green
        Write-Host "  Total Processed: $($Result.totalProcessed)" -ForegroundColor White
        Write-Host "  Success: $($Result.successCount)" -ForegroundColor Green
        
        if ($Result.errorCount -gt 0) {
            Write-Host "  Errors: $($Result.errorCount)" -ForegroundColor Red
            
            if ($Result.errors) {
                Write-Host "`nError Details:" -ForegroundColor Yellow
                $Result.errors | ForEach-Object {
                    Write-Host "    $_" -ForegroundColor Yellow
                }
            }
        }
        else {
            Write-Host "  No errors - all records imported successfully!" -ForegroundColor Green
        }
        
        Write-Host ""
    }
    
    # Obtain token if not provided
    if (-not $Token) {
        Write-Host "Authentication required..." -ForegroundColor Cyan
        $Token = Get-AuthToken -BaseUrl $BaseUrl -Username $Username -Password $Password
        Write-Host "Authentication successful!" -ForegroundColor Green
    }
}

process {
    try {
        # Resolve wildcards and get all matching files
        $files = Get-Item -Path $FilePath -ErrorAction Stop
        
        foreach ($file in $files) {
            # Validate file
            Test-FileExists -Path $file.FullName
            
            # Determine import type
            $importType = $Type
            if ($importType -eq 'Auto') {
                $importType = Get-ImportType -FileName $file.Name
                
                if (-not $importType) {
                    Write-Warning "Cannot determine import type from filename: $($file.Name)"
                    Write-Host "Available types: TaskActivity, Expense"
                    $importType = Read-Host -Prompt "Enter import type"
                }
            }
            
            # Import file
            $result = Import-CsvFile -FilePath $file.FullName -ImportType $importType `
                -BaseUrl $BaseUrl -Token $Token
            
            # Display results
            Show-ImportResults -Result $result
            
            # Return result for pipeline
            [PSCustomObject]@{
                FilePath = $file.FullName
                ImportType = $importType
                BaseUrl = $BaseUrl
                TotalProcessed = $result.totalProcessed
                SuccessCount = $result.successCount
                ErrorCount = $result.errorCount
                Success = $result.errorCount -eq 0
            }
        }
    }
    catch {
        Write-Error "Import failed for $FilePath : $_"
        
        # Return failure result
        [PSCustomObject]@{
            FilePath = $FilePath
            ImportType = $Type
            BaseUrl = $BaseUrl
            TotalProcessed = 0
            SuccessCount = 0
            ErrorCount = 1
            Success = $false
            ErrorMessage = $_.Exception.Message
        }
    }
}

end {
    Write-Host "Import operation completed." -ForegroundColor Cyan
}
