# Script parameter: The Docker image tag to check
param([Parameter(Mandatory=$true)][string]$Tag)

<#
.SYNOPSIS
    Tests whether a specific image tag exists in an AWS ECR repository.

.DESCRIPTION
    Queries the AWS Elastic Container Registry (ECR) to verify if a specified
    image tag exists in the 'taskactivity' repository. This is useful for
    validation in deployment pipelines or build verification processes.

.PARAMETER Tag
    The Docker image tag to search for in the ECR repository.

.EXAMPLE
    Check-ECRImageTag -Tag "v1.2.3"
    Checks if the tag 'v1.2.3' exists in the taskactivity ECR repository.

.EXAMPLE
    Check-ECRImageTag -Tag "latest"
    Checks if the 'latest' tag exists in the repository.

.OUTPUTS
    Boolean - Returns $true if the tag exists, $false otherwise.

.NOTES
    Requires AWS CLI to be installed and configured with appropriate credentials.
    The IAM user/role must have ecr:ListImages permission for the repository.
#>
function Check-ECRImageTag {
    param([string]$Tag)
    
    Write-Host "Searching for tag '$Tag' in ECR repository 'taskactivity'..." -ForegroundColor Cyan
    
    # Query ECR for all images in the taskactivity repository
    # --output json ensures we get structured data that can be parsed
    $result = aws ecr list-images --repository-name taskactivity --region us-east-1 --output json | ConvertFrom-Json
    
    # Search through the imageIds array to find a matching tag
    $found = $result.imageIds | Where-Object { $_.imageTag -eq $Tag }
    
    # Display result with visual indicator
    if ($found) {
        Write-Host "Tag '$Tag' exists in ECR" -ForegroundColor Green
        
        # Get detailed information about the image
        $imageDigest = $found.imageDigest
        $details = aws ecr describe-images --repository-name taskactivity --region us-east-1 --image-ids imageDigest=$imageDigest --output json | ConvertFrom-Json
        
        if ($details.imageDetails) {
            $imageDetail = $details.imageDetails[0]
            Write-Host "  Image Digest: $imageDigest" -ForegroundColor Gray
            
            # Calculate and display image size
            if ($imageDetail.imageSizeInBytes) {
                $sizeMB = [math]::Round($imageDetail.imageSizeInBytes / 1MB, 2)
                Write-Host "  Size: $sizeMB MB" -ForegroundColor Gray
            }
            
            # Display push timestamp
            if ($imageDetail.imagePushedAt) {
                Write-Host "  Pushed: $($imageDetail.imagePushedAt)" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "Tag '$Tag' not found in ECR" -ForegroundColor Red
    }
}

# Execute the function with the provided tag parameter
Check-ECRImageTag -Tag $Tag
