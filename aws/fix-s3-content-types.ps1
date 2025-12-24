# Fix S3 Content-Type for Word and Excel Files
# This script updates the Content-Type metadata for Word and Excel files in the S3 docs bucket
# Run this script to fix existing files that were uploaded with incorrect content types

$bucketName = "taskactivity-docs"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Fix S3 Content-Type for Documents" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"

# Check if AWS CLI is configured
Write-Host "Checking AWS CLI configuration..." -ForegroundColor Yellow
try {
    $identity = aws sts get-caller-identity 2>&1 | ConvertFrom-Json
    Write-Host "✓ AWS CLI configured" -ForegroundColor Green
    Write-Host "  Account: $($identity.Account)" -ForegroundColor Gray
    Write-Host "  User: $($identity.Arn)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "✗ AWS CLI not configured or not authenticated" -ForegroundColor Red
    Write-Host "  Run: aws configure" -ForegroundColor Yellow
    exit 1
}

Write-Host "Fixing Content-Type metadata for documents in S3 bucket: $bucketName" -ForegroundColor Green
Write-Host ""

# Get list of all objects in bucket
Write-Host "Retrieving list of files from S3..." -ForegroundColor Yellow
try {
    $objectsJson = aws s3api list-objects-v2 --bucket $bucketName --query "Contents[].Key" --output json 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ Failed to list objects in bucket" -ForegroundColor Red
        Write-Host ""
        Write-Host "Error details:" -ForegroundColor Yellow
        Write-Host $objectsJson -ForegroundColor Red
        Write-Host ""
        Write-Host "Common causes:" -ForegroundColor Yellow
        Write-Host "  1. Missing IAM permission: s3:ListBucket" -ForegroundColor Gray
        Write-Host "  2. Bucket does not exist" -ForegroundColor Gray
        Write-Host "  3. Incorrect bucket name" -ForegroundColor Gray
        Write-Host ""
        Write-Host "To fix IAM permissions, run:" -ForegroundColor Cyan
        Write-Host "  .\update-iam-policy.ps1" -ForegroundColor White
        Write-Host ""
        exit 1
    }
    
    $objects = $objectsJson | ConvertFrom-Json
    
    if (-not $objects -or $objects.Count -eq 0) {
        Write-Host "ℹ No objects found in bucket $bucketName" -ForegroundColor Yellow
        exit 0
    }
    
    Write-Host "✓ Found $($objects.Count) file(s)" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "✗ Error accessing S3 bucket: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "Checking and updating content types..." -ForegroundColor Yellow
Write-Host ""

$updatedCount = 0
$errorCount = 0

foreach ($key in $objects) {
    $contentType = $null
    
    # Determine correct content type based on file extension
    if ($key -match "\.docx$") {
        $contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }
    elseif ($key -match "\.doc$") {
        $contentType = "application/msword"
    }
    elseif ($key -match "\.xlsx$") {
        $contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
    elseif ($key -match "\.xls$") {
        $contentType = "application/vnd.ms-excel"
    }
    elseif ($key -match "\.pdf$") {
        $contentType = "application/pdf"
    }
    elseif ($key -match "\.html$") {
        $contentType = "text/html"
    }
    
    if ($contentType) {
        # Get current metadata
        try {
            $currentMetadata = aws s3api head-object --bucket $bucketName --key $key 2>&1
            
            if ($LASTEXITCODE -ne 0) {
                Write-Host "✗ Error checking $key" -ForegroundColor Red
                $errorCount++
                continue
            }
        } catch {
            Write-Host "✗ Error checking $key : $($_.Exception.Message)" -ForegroundColor Red
            $errorCount++
            continue
        }
        
        if ($currentMetadata -match '"ContentType":\s*"([^"]+)"') {
            $currentContentType = $matches[1]
            
            # Only update if content type is incorrect
            if ($currentContentType -ne $contentType -and 
                ($currentContentType -eq "text/plain" -or 
                 $currentContentType -eq "application/octet-stream" -or
                 $currentContentType -eq "binary/octet-stream")) {
                
                Write-Host "Updating $key" -ForegroundColor Cyan
                Write-Host "  Old: $currentContentType" -ForegroundColor Red
                Write-Host "  New: $contentType" -ForegroundColor Green
                
                # Copy object to itself with new content type (updates metadata)
                try {
                    aws s3 cp "s3://$bucketName/$key" "s3://$bucketName/$key" --content-type $contentType --metadata-directive REPLACE 2>&1 | Out-Null
                    
                    if ($LASTEXITCODE -ne 0) {
                        Write-Host "  ✗ Failed to update" -ForegroundColor Red
                        $errorCount++
                    } else {
                        $updatedCount++
                        Write-Host "  ✓ Updated successfully" -ForegroundColor Green
                    }
                } catch {
                    Write-Host "  ✗ Error: $($_.Exception.Message)" -ForegroundColor Red
                    $errorCount++
                }
                Write-Host ""
            }
            else {
                Write-Host "✓ $key already has correct content type: $currentContentType" -ForegroundColor DarkGray
            }
        }
    }
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "Content-Type fix completed!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host "Updated: $updatedCount file(s)" -ForegroundColor Green
if ($errorCount -gt 0) {
    Write-Host "Errors: $errorCount file(s)" -ForegroundColor Red
}
Write-Host ""
Write-Host "Note: The Java application has been updated to handle incorrect content types" -ForegroundColor Yellow
Write-Host "automatically based on file extensions as a fallback." -ForegroundColor Yellow
