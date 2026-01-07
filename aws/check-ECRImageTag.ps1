function Test-ECRImageTag {
    <#
    .SYNOPSIS
        Check if a Docker image tag exists in AWS ECR repository.
    
    .DESCRIPTION
        Queries AWS ECR to verify if a specific image tag exists in the taskactivity repository.
        Returns true if found, false otherwise, with colored console output.
    
    .PARAMETER Tag
        The Docker image tag to search for (e.g., '16', 'latest', 'latest-dev')
    
    .PARAMETER RepositoryName
        The ECR repository name. Defaults to 'taskactivity'.
    
    .PARAMETER Region
        AWS region. Defaults to 'us-east-1'.
    
    .EXAMPLE
        Test-ECRImageTag '16'
        # Checks if tag '16' exists in ECR
    
    .EXAMPLE
        Test-ECRImageTag -Tag 'latest-dev' -RepositoryName 'myapp' -Region 'us-west-2'
        # Checks custom repository and region
    #>
    
    param(
        [Parameter(Mandatory=$true, Position=0)]
        [string]$Tag,
        
        [Parameter()]
        [string]$RepositoryName = 'taskactivity',
        
        [Parameter()]
        [string]$Region = 'us-east-1'
    )
    
    try {
        Write-Host "Searching for tag '$Tag' in ECR repository '$RepositoryName'..." -ForegroundColor Cyan
        
        # Query ECR for all images in the repository
        $result = aws ecr list-images --repository-name $RepositoryName --region $Region --output json 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Error querying ECR: $result" -ForegroundColor Red
            return $false
        }
        
        $images = $result | ConvertFrom-Json
        $found = $images.imageIds | Where-Object { $_.imageTag -eq $Tag }
        
        if ($found) {
            Write-Host "Tag '$Tag' exists in ECR" -ForegroundColor Green
            
            # Get additional details about the image
            $details = aws ecr describe-images --repository-name $RepositoryName --region $Region --image-ids imageTag=$Tag --output json | ConvertFrom-Json
            if ($details.imageDetails) {
                $image = $details.imageDetails[0]
                Write-Host "  Image Digest: $($image.imageDigest)" -ForegroundColor Gray
                Write-Host "  Size: $([math]::Round($image.imageSizeInBytes / 1MB, 2)) MB" -ForegroundColor Gray
                Write-Host "  Pushed: $($image.imagePushedAt)" -ForegroundColor Gray
                
                # Show other tags on the same image
                # if ($image.imageTags.Count -gt 1) {
                #     Write-Host "  Other tags: $($image.imageTags -join ', ')" -ForegroundColor Gray
                # }
            }
            
            return $true
        } else {
            Write-Host "Tag '$Tag' not found in ECR" -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "Exception: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# If script is run directly (not dot-sourced), execute tests
if ($MyInvocation.InvocationName -ne '.') {
    if ($args.Count -gt 0) {
        # User provided tags as arguments - check each one
        foreach ($tag in $args) {
            Test-ECRImageTag $tag
            Write-Host ""
        }
    } else {
        # No arguments provided - run default tests
        Write-Host ""
        Write-Host "=== Testing ECR Image Tag Check ===" -ForegroundColor Yellow
        Write-Host ""
        
        # Test with tag 16
        Test-ECRImageTag '16'
        Write-Host ""
        
        # Test with latest-dev
        Test-ECRImageTag 'latest-dev'
        Write-Host ""
        
        # Test with non-existent tag
        Test-ECRImageTag '999'
    }
}
