###############################################################################
# Check AWS SES Configuration and CloudWatch Metrics
# 
# This script verifies that SES email tracking is configured correctly and
# displays available CloudWatch metrics for email monitoring.
#
# Usage:
#   .\check-ses-metrics.ps1
#
###############################################################################

param(
    [Parameter(Mandatory=$false)]
    [string]$ConfigSetName = "taskactivity-emails",
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

Write-Host ""
Write-Host "==================  SES Configuration & Metrics Check  ==================" -ForegroundColor Cyan
Write-Host "Configuration Set: $ConfigSetName" -ForegroundColor Cyan
Write-Host "AWS Region:        $Region" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Check if configuration set exists
Write-Info "Checking SES configuration set..."
$ErrorActionPreference = "SilentlyContinue"
$configSet = aws ses describe-configuration-set `
    --configuration-set-name $ConfigSetName `
    --region $Region `
    --output json 2>&1 | ConvertFrom-Json
$configSetExists = ($LASTEXITCODE -eq 0)
$ErrorActionPreference = "Stop"

if ($configSetExists) {
    Write-Success "Configuration set '$ConfigSetName' exists"
    
    # Show event destinations
    if ($configSet.EventDestinations) {
        Write-Info "Event destinations configured:"
        foreach ($dest in $configSet.EventDestinations) {
            Write-Host "  - $($dest.Name)" -ForegroundColor Gray
            Write-Host "    Enabled: $($dest.Enabled)" -ForegroundColor Gray
            Write-Host "    Event Types: $($dest.MatchingEventTypes -join ', ')" -ForegroundColor Gray
        }
    }
} else {
    Write-Error "Configuration set '$ConfigSetName' not found!"
    Write-Host "Run setup-ses-tracking.ps1 to create it." -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# 2. Check CloudWatch metrics
Write-Info "Checking for CloudWatch SES metrics..."
$metrics = aws cloudwatch list-metrics `
    --namespace "AWS/SES" `
    --region $Region `
    --output json 2>&1 | ConvertFrom-Json

if ($metrics.Metrics -and $metrics.Metrics.Count -gt 0) {
    Write-Success "Found $($metrics.Metrics.Count) SES metrics in CloudWatch"
    Write-Host ""
    Write-Host "Available Metrics:" -ForegroundColor Cyan
    
    # Group by metric name
    $metricsByName = $metrics.Metrics | Group-Object MetricName | Sort-Object Name
    
    foreach ($group in $metricsByName) {
        Write-Host "  - $($group.Name) ($($group.Count) dimension(s))" -ForegroundColor Yellow
        
        # Show dimensions for each metric
        foreach ($metric in $group.Group) {
            if ($metric.Dimensions) {
                $dimString = ($metric.Dimensions | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ", "
                Write-Host "    $dimString" -ForegroundColor Gray
            }
        }
    }
} else {
    Write-Warning "No SES metrics found in CloudWatch yet"
    Write-Host ""
    Write-Host "Metrics will appear after emails are sent. To test:" -ForegroundColor Yellow
    Write-Host "1. Make sure your application is deployed with email enabled" -ForegroundColor Gray
    Write-Host "2. Trigger an email (e.g., password reset, new user registration)" -ForegroundColor Gray
    Write-Host "3. Wait 1-5 minutes for metrics to appear in CloudWatch" -ForegroundColor Gray
    Write-Host "4. Run this script again to see the metrics" -ForegroundColor Gray
}

Write-Host ""

# 3. Check real-time CloudWatch metrics (more accurate than get-send-statistics)
Write-Info "Checking real-time email metrics from CloudWatch (last 1 hour)..."

# Query CloudWatch for Send metric
$startTime = (Get-Date).AddHours(-1).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss')
$endTime = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss')

$ErrorActionPreference = "SilentlyContinue"
$sendMetrics = aws cloudwatch get-metric-statistics `
    --namespace AWS/SES `
    --metric-name Send `
    --start-time $startTime `
    --end-time $endTime `
    --period 300 `
    --statistics Sum `
    --region $Region `
    --output json 2>&1 | ConvertFrom-Json

$deliveryMetrics = aws cloudwatch get-metric-statistics `
    --namespace AWS/SES `
    --metric-name Delivery `
    --start-time $startTime `
    --end-time $endTime `
    --period 300 `
    --statistics Sum `
    --region $Region `
    --output json 2>&1 | ConvertFrom-Json

$bounceMetrics = aws cloudwatch get-metric-statistics `
    --namespace AWS/SES `
    --metric-name Bounce `
    --start-time $startTime `
    --end-time $endTime `
    --period 300 `
    --statistics Sum `
    --region $Region `
    --output json 2>&1 | ConvertFrom-Json

$complaintMetrics = aws cloudwatch get-metric-statistics `
    --namespace AWS/SES `
    --metric-name Complaint `
    --start-time $startTime `
    --end-time $endTime `
    --period 300 `
    --statistics Sum `
    --region $Region `
    --output json 2>&1 | ConvertFrom-Json
$ErrorActionPreference = "Stop"

$totalSent = 0
$totalDelivered = 0
$totalBounces = 0
$totalComplaints = 0

if ($sendMetrics.Datapoints) {
    $totalSent = ($sendMetrics.Datapoints | Measure-Object -Property Sum -Sum).Sum
}
if ($deliveryMetrics.Datapoints) {
    $totalDelivered = ($deliveryMetrics.Datapoints | Measure-Object -Property Sum -Sum).Sum
}
if ($bounceMetrics.Datapoints) {
    $totalBounces = ($bounceMetrics.Datapoints | Measure-Object -Property Sum -Sum).Sum
}
if ($complaintMetrics.Datapoints) {
    $totalComplaints = ($complaintMetrics.Datapoints | Measure-Object -Property Sum -Sum).Sum
}

if ($totalSent -gt 0) {
    Write-Host ""
    Write-Host "Recent Email Activity (Last Hour):" -ForegroundColor Cyan
    Write-Success "Sent:        $totalSent"
    Write-Success "Delivered:   $totalDelivered"
    if ($totalBounces -gt 0) {
        Write-Host "Bounces:     $totalBounces" -ForegroundColor Yellow
    } else {
        Write-Host "Bounces:     $totalBounces" -ForegroundColor Green
    }
    if ($totalComplaints -gt 0) {
        Write-Host "Complaints:  $totalComplaints" -ForegroundColor Red
    } else {
        Write-Host "Complaints:  $totalComplaints" -ForegroundColor Green
    }
    
    # Show individual datapoints if available
    if ($sendMetrics.Datapoints -and $sendMetrics.Datapoints.Count -gt 0) {
        Write-Host ""
        Write-Host "Recent Send Events:" -ForegroundColor Cyan
        $sortedDatapoints = $sendMetrics.Datapoints | Sort-Object Timestamp -Descending
        foreach ($point in $sortedDatapoints) {
            $timestamp = [DateTime]::Parse($point.Timestamp).ToLocalTime().ToString("yyyy-MM-dd HH:mm:ss")
            Write-Host "  $timestamp - $($point.Sum) email(s)" -ForegroundColor Gray
        }
    }
} else {
    Write-Warning "No emails sent in the last hour"
    Write-Host "Try triggering an email from your application (password reset, new user, etc.)" -ForegroundColor Gray
}

Write-Host ""

# 4. Also check aggregate statistics (has 15-30 min delay, but shows longer history)
Write-Info "Checking aggregate SES statistics (has 15-30 min delay)..."
$ErrorActionPreference = "SilentlyContinue"
$statsJson = aws ses get-send-statistics `
    --region $Region `
    --output json 2>&1
$statsSuccess = ($LASTEXITCODE -eq 0)
$ErrorActionPreference = "Stop"

if ($statsSuccess) {
    try {
        $stats = $statsJson | ConvertFrom-Json
    } catch {
        $stats = $null
    }
} else {
    $stats = $null
}

if ($stats -and $stats.SendDataPoints -and $stats.SendDataPoints.Count -gt 0) {
    $recentStats = $stats.SendDataPoints | Sort-Object Timestamp -Descending | Select-Object -First 5
    
    Write-Host "Aggregate Statistics (15-minute intervals, last 24 hours):" -ForegroundColor Cyan
    Write-Host "Time                          Sent    Delivered    Bounces    Complaints    Rejects" -ForegroundColor Gray
    Write-Host "----------------------------  ------  -----------  ---------  ------------  ---------" -ForegroundColor Gray
    
    foreach ($stat in $recentStats) {
        $timestamp = [DateTime]::Parse($stat.Timestamp).ToLocalTime().ToString("yyyy-MM-dd HH:mm:ss")
        $sent = $stat.DeliveryAttempts.ToString().PadLeft(6)
        $delivered = $stat.Deliveries.ToString().PadLeft(11)
        $bounces = $stat.Bounces.ToString().PadLeft(9)
        $complaints = $stat.Complaints.ToString().PadLeft(12)
        $rejects = $stat.Rejects.ToString().PadLeft(9)
        
        Write-Host "$timestamp  $sent  $delivered  $bounces  $complaints  $rejects"
    }
} else {
    Write-Host "No aggregate statistics available yet (data may still be processing)" -ForegroundColor Gray
}

Write-Host ""

# 4. Show helpful links
Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "Useful Commands:" -ForegroundColor Yellow
Write-Host ""
Write-Host "View CloudWatch Metrics in Console:" -ForegroundColor Cyan
Write-Host "  https://console.aws.amazon.com/cloudwatch/home?region=$Region#metricsV2:graph=~();namespace=~'AWS*2fSES" -ForegroundColor Gray
Write-Host ""
Write-Host "View Application Logs (email events):" -ForegroundColor Cyan
Write-Host "  aws logs tail /ecs/taskactivity --filter-pattern `"email`" --follow --region $Region" -ForegroundColor Gray
Write-Host ""
Write-Host "Query specific metric data (last 1 hour):" -ForegroundColor Cyan
Write-Host "  aws cloudwatch get-metric-statistics ``" -ForegroundColor Gray
Write-Host "    --namespace AWS/SES ``" -ForegroundColor Gray
Write-Host "    --metric-name Send ``" -ForegroundColor Gray
Write-Host "    --dimensions Name=ses:configuration-set,Value=$ConfigSetName ``" -ForegroundColor Gray
Write-Host "    --start-time (Get-Date).AddHours(-1).ToString('yyyy-MM-ddTHH:mm:ss') ``" -ForegroundColor Gray
Write-Host "    --end-time (Get-Date).ToString('yyyy-MM-ddTHH:mm:ss') ``" -ForegroundColor Gray
Write-Host "    --period 3600 ``" -ForegroundColor Gray
Write-Host "    --statistics Sum ``" -ForegroundColor Gray
Write-Host "    --region $Region" -ForegroundColor Gray
Write-Host ""
Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host ""
