# ============================================================================
# AWS Billing Check Script
# ============================================================================
# This script retrieves current AWS billing information using Cost Explorer API
#
# Prerequisites:
# 1. AWS CLI installed and configured
# 2. Cost Explorer enabled in AWS account
# 3. IAM permissions (see cost-explorer-policy.json)
#
# Usage:
#   .\check-billing.ps1                      # Current month total only
#   .\check-billing.ps1 -Detailed            # Current month with service breakdown
#   .\check-billing.ps1 -Detailed -Forecast  # Include forecast for rest of month
#   .\check-billing.ps1 -LastMonth           # Previous month total only
#   .\check-billing.ps1 -LastMonth -Detailed # Previous month with service breakdown
# ============================================================================

param(
    [switch]$Detailed,
    [switch]$Forecast,
    [switch]$LastMonth
)

# Colors for output
$ErrorColor = "Red"
$SuccessColor = "Green"
$InfoColor = "Cyan"
$WarningColor = "Yellow"

# Function to check if AWS CLI is installed
function Test-AwsCli {
    try {
        $null = aws --version 2>$null
        return $true
    }
    catch {
        return $false
    }
}

# Function to check if Cost Explorer is enabled
function Test-CostExplorerEnabled {
    try {
        $testDate = (Get-Date).AddMonths(-1).ToString("yyyy-MM-01")
        $endDate = (Get-Date).AddMonths(-1).ToString("yyyy-MM-15")
        
        $result = aws ce get-cost-and-usage `
            --time-period Start=$testDate,End=$endDate `
            --granularity MONTHLY `
            --metrics "UnblendedCost" 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            return $false
        }
        return $true
    }
    catch {
        return $false
    }
}

# Main script
Write-Host "`n============================================" -ForegroundColor $InfoColor
Write-Host "AWS Billing Information" -ForegroundColor $InfoColor
Write-Host "============================================`n" -ForegroundColor $InfoColor

# Check AWS CLI
if (-not (Test-AwsCli)) {
    Write-Host "ERROR: AWS CLI is not installed or not in PATH" -ForegroundColor $ErrorColor
    Write-Host "Please install AWS CLI: https://aws.amazon.com/cli/" -ForegroundColor $WarningColor
    exit 1
}

# Get current AWS identity
try {
    $identity = aws sts get-caller-identity | ConvertFrom-Json
    Write-Host "  Connected as: $($identity.Arn)" -ForegroundColor $SuccessColor
    Write-Host "  Account: $($identity.Account)`n" -ForegroundColor $SuccessColor
}
catch {
    Write-Host "ERROR: Unable to verify AWS credentials" -ForegroundColor $ErrorColor
    Write-Host "Please run 'aws configure' to set up your credentials`n" -ForegroundColor $WarningColor
    exit 1
}

# Determine date range
if ($LastMonth) {
    $prevMonth = (Get-Date).AddMonths(-1)
    $lastDay = [DateTime]::DaysInMonth($prevMonth.Year, $prevMonth.Month)
    $startDate = "{0:yyyy-MM-01}" -f $prevMonth
    $endDate = "{0:yyyy-MM}-{1:00}" -f $prevMonth, $lastDay
}
else {
    $startDate = (Get-Date -Day 1).ToString("yyyy-MM-dd")
    # AWS Cost Explorer requires end date to be today or earlier
    $endDate = (Get-Date).ToString("yyyy-MM-dd")
}

Write-Host "Period: $startDate to $endDate" -ForegroundColor $InfoColor
Write-Host "-----------------------------------------------------------------------" -ForegroundColor $InfoColor

# Get total costs
try {
    if ($Detailed) {
        # Get costs by service
        $timePeriod = "Start=$startDate,End=$endDate"
        $result = aws ce get-cost-and-usage --time-period $timePeriod --granularity MONTHLY --metrics "UnblendedCost" --group-by Type=DIMENSION,Key=SERVICE | ConvertFrom-Json
        
        $currency = "USD"  # Default currency
        $services = $result.ResultsByTime[0].Groups | 
            ForEach-Object { 
                [PSCustomObject]@{
                    Service = $_.Keys[0]
                    Cost = [math]::Round([decimal]$_.Metrics.UnblendedCost.Amount, 2)
                    Currency = $_.Metrics.UnblendedCost.Unit
                }
            } | 
            Where-Object { $_.Cost -gt 0 } |
            Sort-Object Cost -Descending
        
        # Calculate total from all services
        $totalCost = ($services | Measure-Object -Property Cost -Sum).Sum
        if ($services.Count -gt 0) {
            $currency = $services[0].Currency
        }
        
        Write-Host "`nTotal: " -NoNewline -ForegroundColor $InfoColor
        Write-Host "$currency `$$totalCost" -ForegroundColor $SuccessColor
        Write-Host "`nCosts by Service:" -ForegroundColor $InfoColor
        Write-Host "-----------------------------------------------------------------------" -ForegroundColor $InfoColor
        
        foreach ($service in $services) {
            $serviceName = $service.Service.PadRight(50)
            $costFormatted = "`${0:N2}" -f $service.Cost
            Write-Host "  $serviceName" -NoNewline
            Write-Host $costFormatted -ForegroundColor $SuccessColor
        }
    }
    else {
        # Get total cost only
        $timePeriod = "Start=$startDate,End=$endDate"
        $result = aws ce get-cost-and-usage --time-period $timePeriod --granularity MONTHLY --metrics "UnblendedCost" | ConvertFrom-Json
        
        $totalCost = [math]::Round([decimal]$result.ResultsByTime[0].Total.UnblendedCost.Amount, 2)
        $currency = $result.ResultsByTime[0].Total.UnblendedCost.Unit
        
        Write-Host "`nTotal: " -NoNewline -ForegroundColor $InfoColor
        Write-Host "$currency `$$totalCost" -ForegroundColor $SuccessColor
        Write-Host "`nTip: Use -Detailed flag to see breakdown by service" -ForegroundColor $WarningColor
    }
}
catch {
    Write-Host "`nERROR: Unable to fetch cost data" -ForegroundColor $ErrorColor
    Write-Host $_.Exception.Message -ForegroundColor $ErrorColor
    
    # Check if it's a permissions issue
    if ($_.Exception.Message -like "*AccessDenied*" -or $_.Exception.Message -like "*not authorized*") {
        Write-Host "`nPossible causes:" -ForegroundColor $WarningColor
        Write-Host "1. Cost Explorer is not enabled (enable in Billing console)" -ForegroundColor $WarningColor
        Write-Host "2. IAM user lacks Cost Explorer permissions" -ForegroundColor $WarningColor
        Write-Host "   See aws/cost-explorer-policy.json for required permissions`n" -ForegroundColor $WarningColor
    }
    exit 1
}

# Get forecast if requested
if ($Forecast -and -not $LastMonth) {
    $today = Get-Date
    $lastDayOfMonth = [DateTime]::DaysInMonth($today.Year, $today.Month)
    $daysRemaining = $lastDayOfMonth - $today.Day
    
    if ($daysRemaining -gt 0) {
        Write-Host "`n-----------------------------------------------------------------------" -ForegroundColor $InfoColor
        Write-Host "Fetching forecast..." -ForegroundColor $InfoColor
        
        try {
            $forecastStart = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
            $forecastEnd = (Get-Date -Year $today.Year -Month $today.Month -Day $lastDayOfMonth).ToString("yyyy-MM-dd")
            $forecastPeriod = "Start=$forecastStart,End=$forecastEnd"
            
            $forecastResult = aws ce get-cost-forecast --time-period $forecastPeriod --metric UNBLENDED_COST --granularity MONTHLY | ConvertFrom-Json
            
            $forecastCost = [math]::Round([decimal]$forecastResult.Total.Amount, 2)
            $projectedTotal = $totalCost + $forecastCost
            
            Write-Host "`nProjected Rest of Month: " -NoNewline -ForegroundColor $InfoColor
            Write-Host "$currency `$$forecastCost" -ForegroundColor $WarningColor
            Write-Host "Projected Month Total:   " -NoNewline -ForegroundColor $InfoColor
            Write-Host "$currency `$$projectedTotal" -ForegroundColor $WarningColor
        }
        catch {
            Write-Host "`nWARNING: Unable to fetch forecast data" -ForegroundColor $WarningColor
            Write-Host $_.Exception.Message -ForegroundColor $WarningColor
        }
    } else {
        Write-Host "`n-----------------------------------------------------------------------" -ForegroundColor $InfoColor
        Write-Host "Note: Today is the last day of the month - no forecast available" -ForegroundColor $WarningColor
    }
}

Write-Host "`n========================================================================`n" -ForegroundColor $InfoColor
