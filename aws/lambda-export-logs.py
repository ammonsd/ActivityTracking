"""
Lambda function to export CloudWatch logs to S3
Triggered by EventBridge on a schedule
"""
import json
import boto3
from datetime import datetime, timedelta
import os

def lambda_handler(event, context):
    """
    Export CloudWatch logs to S3
    Environment Variables:
    - LOG_GROUP_NAME: CloudWatch log group name (default: /ecs/taskactivity)
    - S3_BUCKET: S3 bucket name (default: taskactivity-logs-archive)
    - EXPORT_DAYS: Number of days to export (default: 1)
    """
    
    logs_client = boto3.client('logs')
    
    # Configuration from environment variables
    log_group_name = os.environ.get('LOG_GROUP_NAME', '/ecs/taskactivity')
    s3_bucket = os.environ.get('S3_BUCKET', 'taskactivity-logs-archive')
    export_days = int(os.environ.get('EXPORT_DAYS', '1'))
    
    # Calculate time range
    to_time = datetime.utcnow()
    from_time = to_time - timedelta(days=export_days)
    
    # Convert to milliseconds since epoch
    from_timestamp = int(from_time.timestamp() * 1000)
    to_timestamp = int(to_time.timestamp() * 1000)
    
    # Create destination prefix with date
    destination_prefix = f"cloudwatch-exports/{to_time.strftime('%Y-%m-%d')}"
    
    try:
        # Create export task
        response = logs_client.create_export_task(
            logGroupName=log_group_name,
            fromTime=from_timestamp,
            to=to_timestamp,
            destination=s3_bucket,
            destinationPrefix=destination_prefix
        )
        
        task_id = response['taskId']
        
        print(f"Export task created successfully!")
        print(f"Task ID: {task_id}")
        print(f"Log Group: {log_group_name}")
        print(f"Date Range: {from_time.strftime('%Y-%m-%d %H:%M:%S')} to {to_time.strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"Destination: s3://{s3_bucket}/{destination_prefix}")
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Export task created successfully',
                'taskId': task_id,
                'destination': f"s3://{s3_bucket}/{destination_prefix}"
            })
        }
        
    except Exception as e:
        print(f"Error creating export task: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({
                'message': 'Failed to create export task',
                'error': str(e)
            })
        }
