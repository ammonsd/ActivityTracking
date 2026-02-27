#!/usr/bin/env groovy

/**
 * Jenkins Pipeline for Task Activity Tracking Application
 * 
 * This pipeline implements a complete CI/CD workflow including:
 * - Source code checkout
 * - Maven build and unit testing
 * - Docker image creation
 * - Amazon ECR push
 * - ECS Fargate deployment
 * - Automated rollback on failure
 * 
 * Prerequisites:
 * - Jenkins plugins: Pipeline, Docker Pipeline, AWS Steps, Maven Integration, Git
 * - Jenkins credentials: 'aws-credentials' (AWS access key/secret)
 * - Jenkins tools: Maven 3.9+, JDK 21, Spring Boot 3.5+
 * - AWS resources: ECR repository, ECS cluster, task definition
 */

pipeline {
    agent any
    
    triggers {
        // Build on main branch commits (auto-build with no-cache)
        // Poll every 5 minutes for changes
        pollSCM('H/5 * * * *')
        
        // Daily deployments at 8am, 12pm, and 4pm (only if there are new builds)
        // Using H notation to spread load evenly across the hour
        // Using 0 notation for top of the hour
        // cron('0 8,12,17 * * *')
        // cron('H 16 * * *')
        // Scheduled deployments will check if there are new commits since last deployment and
        // only deploy if there are changes
        cron('0 18 * * *')
    }
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'production'],
            description: 'Target deployment environment'
        )
        choice(
            name: 'DEPLOY_ACTION',
            choices: ['build-only', 'deploy', 'rollback'],
            description: 'Deployment action to perform'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip unit tests (not recommended for production)'
        )
        booleanParam(
            name: 'NO_CACHE',
            defaultValue: false,
            description: 'Build Docker image without cache (forces complete rebuild)'
        )
        booleanParam(
            name: 'DEPLOY_INFRASTRUCTURE',
            defaultValue: false,
            description: 'Deploy/update CloudFormation infrastructure stack before application deployment'
        )
        choice(
            name: 'INFRASTRUCTURE_ACTION',
            choices: ['update', 'create', 'preview'],
            description: 'CloudFormation action (only used if DEPLOY_INFRASTRUCTURE is true)'
        )
        booleanParam(
            name: 'MANUAL_TRIGGER',
            defaultValue: false,
            description: 'Set to true for manual deployments (affects scheduled deployment logic)'
        )
    }
    
    environment {
        // AWS Configuration - Update these for your environment
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '378010131175'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        ECR_REPOSITORY = 'taskactivity'
        
        // ECS Configuration - Matches existing AWS resources
        ECS_CLUSTER = "taskactivity-cluster"
        ECS_SERVICE = "taskactivity-service"
        TASK_DEFINITION_FAMILY = "taskactivity"
        
        // Docker Image Tags
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        IMAGE_FULL = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
        IMAGE_LATEST = "${ECR_REGISTRY}/${ECR_REPOSITORY}:latest-${params.ENVIRONMENT}"
        
        // Build Configuration
        MAVEN_OPTS = '-Xmx1024m'
        JAVA_HOME = tool name: 'JDK-21', type: 'jdk'
        MAVEN_HOME = tool name: 'Maven-3.9', type: 'maven'
        PATH = "${MAVEN_HOME}/bin:${JAVA_HOME}/bin:${env.PATH}"
        
        // Application version from pom.xml (set after checkout in Initialize stage)
        // APP_VERSION will be dynamically set
        
        // Build Notifications Configuration
        APP_URL = 'https://taskactivitytracker.com'
        JENKINS_URL = 'http://172.27.85.228:8081'  // Jenkins base URL
        JENKINS_API_TOKEN = credentials('jenkins-api-token')  // JWT token with JENKINS:NOTIFY permission
    }
    
    options {
        // Keep only last 30 builds
        buildDiscarder(logRotator(numToKeepStr: '30'))
        
        // Timeout after 30 minutes
        timeout(time: 30, unit: 'MINUTES')
        
        // Add timestamps to console output
        timestamps()
        
        // Prevent concurrent builds
        disableConcurrentBuilds()
        
        // Skip default checkout - we'll do it manually after cleanup
        skipDefaultCheckout(true)
    }
    
    stages {
        stage('Workspace Cleanup') {
            steps {
                script {
                    echo "========================================="
                    echo "Cleaning Workspace"
                    echo "========================================="
                    
                    // Clean up any corrupted @script directories from previous failed builds
                    try {
                        sh '''
                            # Find and remove empty or corrupted @script subdirectories
                            if [ -d "${WORKSPACE}@script" ]; then
                                echo "Cleaning up @script directories..."
                                find "${WORKSPACE}@script" -mindepth 1 -maxdepth 1 -type d -empty -delete 2>/dev/null || true
                                echo "Cleanup complete"
                            fi
                        '''
                    } catch (Exception e) {
                        echo "Warning: Could not clean @script directories: ${e.message}"
                    }
                    
                    // Clean workspace but keep .git for faster checkout
                    cleanWs(
                        deleteDirs: true,
                        disableDeferredWipeout: true,
                        patterns: [
                            [pattern: '.git', type: 'EXCLUDE'],
                            [pattern: '.gitignore', type: 'EXCLUDE']
                        ]
                    )
                }
            }
        }
        
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
                
                script {
                    // Display recent commits
                    sh 'git log --oneline -5'
                }
            }
        }
        
        stage('Initialize') {
            steps {
                script {
                    // Read application version from pom.xml after checkout
                    def pom = readMavenPom()
                    env.APP_VERSION = pom.version
                    
                    echo "========================================="
                    echo "Task Activity Tracking - CI/CD Pipeline"
                    echo "========================================="
                    echo "Environment: ${params.ENVIRONMENT}"
                    echo "Action: ${params.DEPLOY_ACTION}"
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    echo "Branch: ${env.GIT_BRANCH}"
                    echo "Commit: ${env.GIT_COMMIT?.take(7)}"
                    echo "Application Version: ${env.APP_VERSION}"
                    echo "Trigger: ${currentBuild.getBuildCauses()[0]._class}"
                    echo "========================================="
                    
                    // Determine if this is a scheduled build
                    def isScheduledBuild = currentBuild.getBuildCauses().toString().contains('TimerTrigger')
                    def isSCMBuild = currentBuild.getBuildCauses().toString().contains('SCMTrigger')
                    
                    // Store build trigger information
                    env.IS_SCHEDULED_BUILD = isScheduledBuild.toString()
                    env.IS_SCM_BUILD = isSCMBuild.toString()
                    env.IS_MANUAL_BUILD = params.MANUAL_TRIGGER.toString()
                    
                    echo "Scheduled Build: ${env.IS_SCHEDULED_BUILD}"
                    echo "SCM Triggered Build: ${env.IS_SCM_BUILD}"
                    echo "Manual Build: ${env.IS_MANUAL_BUILD}"
                    
                    // Auto-enable NO_CACHE for SCM builds on main branch
                    if (isSCMBuild && env.GIT_BRANCH?.contains('main')) {
                        env.BUILD_NO_CACHE = 'true'
                        echo "Auto-enabled NO_CACHE for main branch build"
                    } else {
                        env.BUILD_NO_CACHE = params.NO_CACHE.toString()
                    }
                    
                    // Check deployment eligibility for scheduled builds
                    if (isScheduledBuild) {
                        echo "========================================="
                        echo "Checking scheduled deployment eligibility..."
                        echo "Current commit: ${env.GIT_COMMIT}"
                        echo "========================================="
                        
                        // Compare current HEAD commit against last successfully deployed commit (stored in SSM)
                        // Use git CLI as fallback since env.GIT_COMMIT can be null in some Jenkins configurations
                        def currentCommit = env.GIT_COMMIT ?: sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                        env.RESOLVED_GIT_COMMIT = currentCommit
                        
                        def lastDeployedCommit = getLastDeployedCommit()
                        
                        echo "Current commit: ${currentCommit}"
                        echo "Last deployed commit: ${lastDeployedCommit ?: 'none'}"
                        
                        // Determine if we should deploy
                        def shouldDeploy = false
                        
                        if (lastDeployedCommit == null) {
                            echo "No previous deployments found - will deploy"
                            shouldDeploy = true
                        } else if (lastDeployedCommit == currentCommit) {
                            echo "Current commit already deployed - skipping deployment"
                        } else {
                            echo "New commit detected since last deployment - will deploy"
                            shouldDeploy = true
                        }
                        
                        env.SHOULD_DEPLOY = shouldDeploy.toString()
                        
                        if (shouldDeploy) {
                            // Override deploy action for scheduled builds
                            env.SCHEDULED_DEPLOY_ACTION = 'deploy'
                            echo "This scheduled build will perform deployment"
                        } else {
                            env.SCHEDULED_DEPLOY_ACTION = 'skip'
                            echo "This scheduled build will be skipped (no new builds)"
                            currentBuild.result = 'NOT_BUILT'
                            currentBuild.description = 'Skipped: No new builds since last deployment'
                        }
                        
                        echo "========================================="
                    }
                    
                    // Validation
                    if (params.ENVIRONMENT == 'production' && params.SKIP_TESTS) {
                        error("Cannot skip tests for production deployments!")
                    }
                }
            }
        }
        
        stage('Skip Scheduled Build') {
            when {
                expression { 
                    return env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false'
                }
            }
            steps {
                script {
                    echo "========================================="
                    echo "Scheduled deployment skipped"
                    echo "Reason: No new builds since last deployment"
                    echo "========================================="
                    
                    // Send notification if JENKINS_DEPLOY_SKIPPED_CHECK is enabled
                    try {
                        // Read JENKINS_DEPLOY_SKIPPED_CHECK from ECS task definition
                        def taskDefJson = null
                        withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                            taskDefJson = sh(
                                script: """
                                    aws ecs describe-task-definition \
                                        --task-definition ${TASK_DEFINITION_FAMILY} \
                                        --region ${AWS_REGION} \
                                        --query 'taskDefinition.containerDefinitions[0].environment[?name==`JENKINS_DEPLOY_SKIPPED_CHECK`].value' \
                                        --output text
                                """,
                                returnStdout: true
                            ).trim()
                        }
                        
                        def skippedCheckEnabled = taskDefJson?.toLowerCase() == 'true'
                        
                        echo "JENKINS_DEPLOY_SKIPPED_CHECK from ECS task definition: ${taskDefJson}"
                        
                        if (skippedCheckEnabled) {
                            echo "Sending skipped deployment notification..."
                            
                            def endpoint = "${APP_URL}/api/jenkins/deploy-skipped"
                            def payload = """
                                {
                                    "buildNumber": "${BUILD_NUMBER}",
                                    "branch": "${env.GIT_BRANCH ?: 'main'}",
                                    "commit": "${env.GIT_COMMIT ?: 'unknown'}",
                                    "buildUrl": "${JENKINS_URL}/job/TaskActivity-Pipeline/${BUILD_NUMBER}/",
                                    "environment": "${params.ENVIRONMENT}",
                                    "reason": "No new builds since last deployment",
                                    "triggeredBy": "scheduled"
                                }
                            """
                            
                            def response = sh(
                                script: """
                                    curl -s -w '\\n%{http_code}' -X POST ${endpoint} \\
                                         -H "Content-Type: application/json" \\
                                         -H "Authorization: Bearer ${JENKINS_API_TOKEN}" \\
                                         -d '${payload}'
                                """,
                                returnStdout: true
                            ).trim()
                            
                            def lines = response.split('\n')
                            def httpCode = lines[-1]
                            def body = lines.size() > 1 ? lines[0..-2].join('\n') : ''
                            
                            if (httpCode == '200') {
                                echo "✓ Skipped deployment notification sent successfully"
                                echo "Response: ${body}"
                            } else {
                                echo "⚠ Notification failed with HTTP ${httpCode}"
                                echo "Response: ${body}"
                            }
                        } else {
                            echo "Skipped deployment notification disabled (JENKINS_DEPLOY_SKIPPED_CHECK not enabled)"
                        }
                    } catch (Exception e) {
                        echo "⚠ Failed to send skipped deployment notification: ${e.message}"
                        // Don't fail the build if notification fails
                    }
                }
            }
        }
        
        stage('Deploy Infrastructure') {
            when {
                expression { params.DEPLOY_INFRASTRUCTURE == true }
            }
            steps {
                echo "========================================="
                echo "CloudFormation Infrastructure Deployment"
                echo "========================================="
                echo "Environment: ${params.ENVIRONMENT}"
                echo "Action: ${params.INFRASTRUCTURE_ACTION}"
                echo "Stack: taskactivity-${params.ENVIRONMENT}"
                echo "========================================="
                
                script {
                    withAWS(credentials: 'aws-credentials', region: AWS_REGION) {
                        // Run infrastructure deployment script
                        def scriptPath = './cloudformation/scripts/deploy-infrastructure.sh'
                        
                        // Make script executable
                        sh "chmod +x ${scriptPath}"
                        
                        // Execute infrastructure deployment
                        def infraResult = sh(
                            script: "${scriptPath} ${params.ENVIRONMENT} ${params.INFRASTRUCTURE_ACTION}",
                            returnStatus: true
                        )
                        
                        if (infraResult != 0) {
                            error("Infrastructure deployment failed!")
                        }
                        
                        echo "Infrastructure deployment completed successfully"
                        
                        // If this was just a preview, warn that application won't be deployed
                        if (params.INFRASTRUCTURE_ACTION == 'preview') {
                            echo "WARNING: Infrastructure preview only - application will not be deployed"
                        }
                    }
                }
            }
            post {
                success {
                    echo "✓ Infrastructure ${params.INFRASTRUCTURE_ACTION} completed successfully"
                }
                failure {
                    echo "✗ Infrastructure ${params.INFRASTRUCTURE_ACTION} failed"
                }
            }
        }
        
        stage('Build & Test') {
            when {
                allOf {
                    expression { params.DEPLOY_ACTION != 'rollback' }
                    expression { 
                        // Skip if scheduled build determined deployment is not needed
                        return !(env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false')
                    }
                }
            }
            steps {
                echo 'Building application with Maven...'
                script {
                    if (params.SKIP_TESTS) {
                        echo 'WARNING: Skipping tests as requested'
                        sh './mvnw clean package -DskipTests -B'
                    } else {
                        echo 'Running unit tests...'
                        sh './mvnw clean test -B'
                        
                        echo 'Packaging application...'
                        sh './mvnw package -DskipTests -B'
                    }
                }
            }
            post {
                success {
                    echo 'Build completed successfully'
                    
                    // Archive the JAR file
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
                always {
                    // Publish test results if tests were run
                    script {
                        if (!params.SKIP_TESTS) {
                            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                        }
                    }
                }
            }
        }
        
        stage('Code Quality Analysis') {
            when {
                allOf {
                    expression { params.DEPLOY_ACTION != 'rollback' && !params.SKIP_TESTS }
                    expression { 
                        return !(env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false')
                    }
                }
            }
            steps {
                echo 'Running code quality checks...'
                // Placeholder for SonarQube or other quality tools
                script {
                    echo 'Code quality analysis placeholder'
                    echo 'Integrate SonarQube here if available'
                }
            }
        }
        
        stage('Build Docker Image') {
            when {
                allOf {
                    expression { params.DEPLOY_ACTION != 'rollback' }
                    expression { 
                        return !(env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false')
                    }
                    // Only proceed if build and tests passed
                    expression { currentBuild.currentResult == 'SUCCESS' }
                }
            }
            steps {
                echo "Building Docker image: ${IMAGE_FULL}"
                script {
                    // Use cached value from Initialize stage for NO_CACHE setting
                    def buildArgs = env.BUILD_NO_CACHE == 'true' ? '--no-cache' : ''
                    
                    if (env.BUILD_NO_CACHE == 'true') {
                        echo "Building with NO_CACHE enabled"
                    }
                    
                    // Build Docker image
                    docker.build(
                        "${IMAGE_FULL}",
                        "${buildArgs} --build-arg BUILD_NUMBER=${env.BUILD_NUMBER} ."
                    )
                    
                    // Tag with latest for environment
                    sh "docker tag ${IMAGE_FULL} ${IMAGE_LATEST}"
                    
                    // Tag with simple latest for local development
                    sh "docker tag ${IMAGE_FULL} taskactivity:latest"
                    
                    echo "Docker image built successfully"
                    sh "docker images | grep ${ECR_REPOSITORY}"
                }
            }
        }
        
        stage('Security Scan') {
            when {
                allOf {
                    expression { params.DEPLOY_ACTION != 'rollback' }
                    expression { 
                        return !(env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false')
                    }
                    // Only proceed if build and tests passed
                    expression { currentBuild.currentResult == 'SUCCESS' }
                }
            }
            steps {
                echo 'Scanning Docker image for vulnerabilities...'
                script {
                    // Placeholder for Trivy or other security scanning tools
                    echo 'Security scan placeholder'
                    echo 'Consider integrating Trivy, Snyk, or AWS ECR scanning'
                }
            }
        }
        
        stage('Push to ECR') {
            when {
                allOf {
                    anyOf {
                        // Manual deploy action (not build-only or rollback)
                        expression { 
                            params.DEPLOY_ACTION != 'rollback' && params.DEPLOY_ACTION != 'build-only'
                        }
                        // Scheduled deployment that passed eligibility check
                        expression { 
                            env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'true'
                        }
                    }
                    // Don't run if scheduled build was skipped
                    expression { 
                        return !(env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false')
                    }
                    // Only proceed if build and tests passed
                    expression { currentBuild.currentResult == 'SUCCESS' }
                }
            }
            steps {
                echo "Pushing Docker image to ECR: ${ECR_REGISTRY}/${ECR_REPOSITORY}"
                script {
                    withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                        // Using ECR credential helper for automatic authentication
                        // No manual docker login required - credentials handled securely by helper
                        echo "Authenticating to ECR using credential helper..."
                        
                        // Push images - credential helper handles authentication automatically
                        sh "docker push ${IMAGE_FULL}"
                        sh "docker push ${IMAGE_LATEST}"
                        
                        echo "Images pushed successfully to ECR"
                    }
                }
            }
        }
        
        stage('Deploy to ECS') {
            when {
                allOf {
                    anyOf {
                        // Manual deploy action
                        expression { params.DEPLOY_ACTION == 'deploy' }
                        // Scheduled deployment that passed eligibility check
                        expression { 
                            env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'true'
                        }
                    }
                    expression { 
                        return !(env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false')
                    }
                    // Only proceed if build and tests passed
                    expression { currentBuild.currentResult == 'SUCCESS' }
                }
            }
            steps {
                echo "Deploying to ECS cluster: ${ECS_CLUSTER}"
                script {
                    withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                        // Get current task definition and save to file
                        sh """
                            aws ecs describe-task-definition \
                                --task-definition ${TASK_DEFINITION_FAMILY} \
                                --query 'taskDefinition' \
                                --output json > task-def.json
                        """
                        
                        // Update image in task definition and save to new file
                        sh """
                            jq --arg IMAGE "${IMAGE_FULL}" \
                            '.containerDefinitions[0].image = \$IMAGE | 
                             del(.taskDefinitionArn, .revision, .status, .requiresAttributes, 
                                 .compatibilities, .registeredAt, .registeredBy)' \
                            task-def.json > updated-task-def.json
                        """
                        
                        // Register new task definition from file
                        def newTaskDefArn = sh(
                            script: """
                                aws ecs register-task-definition \
                                    --cli-input-json file://updated-task-def.json \
                                    --query 'taskDefinition.taskDefinitionArn' \
                                    --output text
                            """,
                            returnStdout: true
                        ).trim()
                        
                        echo "New task definition registered: ${newTaskDefArn}"
                        
                        // Update ECS service
                        sh """
                            aws ecs update-service \
                                --cluster ${ECS_CLUSTER} \
                                --service ${ECS_SERVICE} \
                                --task-definition ${newTaskDefArn} \
                                --force-new-deployment
                        """
                        
                        echo "ECS service update initiated"
                        
                        // Wait for deployment to stabilize
                        echo "Waiting for deployment to stabilize (this may take several minutes)..."
                        sh """
                            aws ecs wait services-stable \
                                --cluster ${ECS_CLUSTER} \
                                --services ${ECS_SERVICE}
                        """
                        
                        echo "Deployment completed successfully!"
                    }
                }
            }
        }
        
        stage('Rollback') {
            when {
                expression { params.DEPLOY_ACTION == 'rollback' }
            }
            steps {
                echo "Rolling back to previous task definition..."
                script {
                    withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                        // Get list of task definitions
                        def taskDefs = sh(
                            script: """
                                aws ecs list-task-definitions \
                                    --family-prefix ${TASK_DEFINITION_FAMILY} \
                                    --status ACTIVE \
                                    --sort DESC \
                                    --query 'taskDefinitionArns[0:2]' \
                                    --output text
                            """,
                            returnStdout: true
                        ).trim().split()
                        
                        if (taskDefs.size() < 2) {
                            error("No previous task definition available for rollback")
                        }
                        
                        def previousTaskDef = taskDefs[1]
                        echo "Rolling back to: ${previousTaskDef}"
                        
                        // Update service with previous task definition
                        sh """
                            aws ecs update-service \
                                --cluster ${ECS_CLUSTER} \
                                --service ${ECS_SERVICE} \
                                --task-definition ${previousTaskDef} \
                                --force-new-deployment
                        """
                        
                        // Wait for rollback to complete
                        echo "Waiting for rollback to complete..."
                        sh """
                            aws ecs wait services-stable \
                                --cluster ${ECS_CLUSTER} \
                                --services ${ECS_SERVICE}
                        """
                        
                        echo "Rollback completed successfully!"
                    }
                }
            }
        }
        
        stage('Verify Deployment') {
            when {
                allOf {
                    anyOf {
                        expression { params.DEPLOY_ACTION == 'deploy' }
                        expression { 
                            env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'true'
                        }
                    }
                    expression { 
                        return !(env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false')
                    }
                }
            }
            steps {
                echo 'Verifying deployment...'
                script {
                    withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                        // Get service status
                        def serviceInfo = sh(
                            script: """
                                aws ecs describe-services \
                                    --cluster ${ECS_CLUSTER} \
                                    --services ${ECS_SERVICE} \
                                    --query 'services[0]' \
                                    --output json
                            """,
                            returnStdout: true
                        ).trim()
                        
                        echo "Service Status:"
                        sh """
                            echo '${serviceInfo}' | jq '{
                                runningCount: .runningCount,
                                desiredCount: .desiredCount,
                                taskDefinition: .taskDefinition,
                                deployments: .deployments
                            }'
                        """
                        
                        // Check if deployment was successful
                        def runningCount = sh(
                            script: "echo '${serviceInfo}' | jq -r '.runningCount'",
                            returnStdout: true
                        ).trim().toInteger()
                        
                        def desiredCount = sh(
                            script: "echo '${serviceInfo}' | jq -r '.desiredCount'",
                            returnStdout: true
                        ).trim().toInteger()
                        
                        if (runningCount != desiredCount) {
                            error("Deployment verification failed: running=${runningCount}, desired=${desiredCount}")
                        }
                        
                        echo "✓ Deployment verified: ${runningCount}/${desiredCount} tasks running"
                    }
                }
            }
        }
        
        stage('Cleanup') {
            when {
                expression { params.DEPLOY_ACTION != 'build-only' }
            }
            steps {
                echo 'Cleaning up local Docker images...'
                script {
                    // Remove local Docker images to save space
                    sh """
                        docker rmi ${IMAGE_FULL} || true
                        docker rmi ${IMAGE_LATEST} || true
                    """
                    
                    echo 'Cleanup completed'
                }
            }
        }
    }
    
    post {
        success {
            script {
                // Skip success notification if scheduled build was skipped
                if (env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false') {
                    echo "Skipped deployment - no notification sent"
                    return
                }
                
                // Record deployed commit to SSM for future scheduled deployment eligibility checks
                def actualAction = params.DEPLOY_ACTION
                if (env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'true') {
                    actualAction = 'deploy'
                }
                
                if (actualAction == 'deploy') {
                    recordDeployedCommit()
                }
                
                echo "========================================="
                echo "✓ BUILD SUCCESSFUL"
                echo "========================================="
                echo "Environment: ${params.ENVIRONMENT}"
                echo "Action: ${actualAction}"
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Image: ${IMAGE_FULL}"
                
                // Mark this build with description based on type
                if (env.IS_SCHEDULED_BUILD == 'true') {
                    currentBuild.description = "Scheduled deployment completed"
                } else if (env.IS_SCM_BUILD == 'true') {
                    currentBuild.description = "Auto-build from main branch (no-cache)"
                } else if (env.IS_MANUAL_BUILD == 'true') {
                    currentBuild.description = "Manual ${params.DEPLOY_ACTION}"
                }
                
                echo "========================================="
                
                // Send success notification to Task Activity application
                try {
                    // actualAction already defined above - reuse it
                    
                    def endpoint = actualAction == 'deploy' ? 
                        "${APP_URL}/api/jenkins/deploy-success" : 
                        "${APP_URL}/api/jenkins/build-success"
                    
                    def payload = actualAction == 'deploy' ? """
                        {
                            "buildNumber": "${BUILD_NUMBER}",
                            "branch": "${env.GIT_BRANCH ?: 'main'}",
                            "commit": "${env.GIT_COMMIT ?: 'unknown'}",
                            "deployUrl": "${JENKINS_URL}/job/TaskActivity-Pipeline/${BUILD_NUMBER}/",
                            "environment": "${params.ENVIRONMENT}",
                            "triggeredBy": "${env.IS_SCHEDULED_BUILD == 'true' ? 'scheduled' : (env.IS_SCM_BUILD == 'true' ? 'scm' : 'manual')}"
                        }
                    """ : """
                        {
                            "buildNumber": "${BUILD_NUMBER}",
                            "branch": "${env.GIT_BRANCH ?: 'main'}",
                            "commit": "${env.GIT_COMMIT ?: 'unknown'}",
                            "buildUrl": "${JENKINS_URL}/job/TaskActivity-Pipeline/${BUILD_NUMBER}/",
                            "environment": "${params.ENVIRONMENT}",
                            "triggeredBy": "${env.IS_SCHEDULED_BUILD == 'true' ? 'scheduled' : (env.IS_SCM_BUILD == 'true' ? 'scm' : 'manual')}",
                            "noCache": "${env.BUILD_NO_CACHE}"
                        }
                    """
                    
                    def response = sh(
                        script: """
                            curl -s -w '\\n%{http_code}' -X POST ${endpoint} \\
                                 -H "Content-Type: application/json" \\
                                 -H "Authorization: Bearer ${JENKINS_API_TOKEN}" \\
                                 -d '${payload}'
                        """,
                        returnStdout: true
                    ).trim()
                    
                    def lines = response.split('\n')
                    def httpCode = lines[-1]
                    def body = lines.size() > 1 ? lines[0..-2].join('\n') : ''
                    
                    if (httpCode == '200') {
                        def notificationType = actualAction == 'deploy' ? 'Deploy' : 'Build'
                        echo "✓ ${notificationType} success notification sent successfully"
                        echo "Response: ${body}"
                    } else {
                        echo "⚠ Notification failed with HTTP ${httpCode}"
                        echo "Response: ${body}"
                    }
                } catch (Exception e) {
                    echo "⚠ Failed to send notification: ${e.message}"
                    // Don't fail the build if notification fails
                }
            }
        }
        
        failure {
            script {
                // Send email notification for ALL failures (including syntax errors)
                def notificationEmail = 'deanammons@gmail.com'
                
                try {
                    emailext(
                        subject: "❌ Jenkins Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """Build failed for ${env.JOB_NAME}

Build Number: ${env.BUILD_NUMBER}
Environment: ${params.ENVIRONMENT ?: 'N/A'}
Branch: ${env.GIT_BRANCH ?: 'main'}
Commit: ${env.GIT_COMMIT ?: 'unknown'}

Console Output:
${env.BUILD_URL}console

This is an automated notification. Do not reply to this email.
""",
                        to: notificationEmail,
                        mimeType: 'text/plain'
                    )
                    echo "✓ Build failure email notification sent to: ${notificationEmail}"
                } catch (Exception e) {
                    echo "⚠ Failed to send email notification: ${e.message}"
                }
            }
            
            script {
                // Skip failure notification if scheduled build was skipped
                if (env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'false') {
                    return
                }
                
                echo "========================================="
                echo "✗ BUILD FAILED"
                echo "========================================="
                echo "Environment: ${params.ENVIRONMENT}"
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Check console output for details"
                echo "========================================="
                
                // Send failure notification to Task Activity application
                try {
                    // Determine actual action that was attempted
                    def actualAction = params.DEPLOY_ACTION
                    if (env.IS_SCHEDULED_BUILD == 'true' && env.SHOULD_DEPLOY == 'true') {
                        actualAction = 'deploy'
                    }
                    
                    def endpoint = actualAction == 'deploy' ? 
                        "${APP_URL}/api/jenkins/deploy-failure" : 
                        "${APP_URL}/api/jenkins/build-failure"
                    
                    def payload = actualAction == 'deploy' ? """
                        {
                            "buildNumber": "${BUILD_NUMBER}",
                            "branch": "${env.GIT_BRANCH ?: 'main'}",
                            "commit": "${env.GIT_COMMIT ?: 'unknown'}",
                            "deployUrl": "${JENKINS_URL}/job/TaskActivity-Pipeline/${BUILD_NUMBER}/",
                            "consoleUrl": "${JENKINS_URL}/job/TaskActivity-Pipeline/${BUILD_NUMBER}/console",
                            "environment": "${params.ENVIRONMENT}",
                            "triggeredBy": "${env.IS_SCHEDULED_BUILD == 'true' ? 'scheduled' : (env.IS_SCM_BUILD == 'true' ? 'scm' : 'manual')}"
                        }
                    """ : """
                        {
                            "buildNumber": "${BUILD_NUMBER}",
                            "branch": "${env.GIT_BRANCH ?: 'main'}",
                            "commit": "${env.GIT_COMMIT ?: 'unknown'}",
                            "buildUrl": "${JENKINS_URL}/job/TaskActivity-Pipeline/${BUILD_NUMBER}/",
                            "consoleUrl": "${JENKINS_URL}/job/TaskActivity-Pipeline/${BUILD_NUMBER}/console",
                            "environment": "${params.ENVIRONMENT}",
                            "triggeredBy": "${env.IS_SCHEDULED_BUILD == 'true' ? 'scheduled' : (env.IS_SCM_BUILD == 'true' ? 'scm' : 'manual')}"
                        }
                    """
                    
                    def response = sh(
                        script: """
                            curl -s -w '\\n%{http_code}' -X POST ${endpoint} \\
                                 -H "Content-Type: application/json" \\
                                 -H "Authorization: Bearer ${JENKINS_API_TOKEN}" \\
                                 -d '${payload}'
                        """,
                        returnStdout: true
                    ).trim()
                    
                    def lines = response.split('\n')
                    def httpCode = lines[-1]
                    def body = lines.size() > 1 ? lines[0..-2].join('\n') : ''
                    
                    if (httpCode == '200') {
                        def notificationType = actualAction == 'deploy' ? 'Deploy' : 'Build'
                        echo "✓ ${notificationType} failure notification sent successfully"
                        echo "Response: ${body}"
                    } else {
                        echo "⚠ Notification failed with HTTP ${httpCode}"
                        echo "Response: ${body}"
                    }
                } catch (Exception e) {
                    echo "⚠ Failed to send notification: ${e.message}"
                    // Don't fail the build if notification fails
                }
            }
        }
        
        unstable {
            echo 'Build is unstable - some tests may have failed'
        }
        
        always {
            // Clean up workspace thoroughly to prevent corrupt directories
            script {
                echo "Performing final workspace cleanup..."
                
                // Clean up @script directories that may have been created during this build
                try {
                    sh '''
                        # Remove all @script directories
                        if [ -d "${WORKSPACE}@script" ]; then
                            echo "Removing @script directory: ${WORKSPACE}@script"
                            rm -rf "${WORKSPACE}@script" 2>/dev/null || true
                        fi
                        
                        # Clean up any @tmp directories
                        if [ -d "${WORKSPACE}@tmp" ]; then
                            echo "Removing @tmp directory: ${WORKSPACE}@tmp"
                            rm -rf "${WORKSPACE}@tmp" 2>/dev/null || true
                        fi
                    '''
                } catch (Exception e) {
                    echo "Warning: Could not clean temporary directories: ${e.message}"
                }
            }
            
            cleanWs(
                deleteDirs: true,
                disableDeferredWipeout: true,
                patterns: [
                    [pattern: 'target/**', type: 'INCLUDE'],
                    [pattern: '.mvn/**', type: 'INCLUDE'],
                    [pattern: 'node_modules/**', type: 'INCLUDE'],
                    [pattern: 'frontend/node_modules/**', type: 'INCLUDE'],
                    [pattern: 'frontend-react/node_modules/**', type: 'INCLUDE']
                ]
            )
        }
    }
}

/**
 * Retrieve the last successfully deployed Git commit SHA from SSM Parameter Store.
 * Returns null if no deployment has been recorded yet.
 */
def getLastDeployedCommit() {
    try {
        def result = null
        withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
            result = sh(
                script: """
                    aws ssm get-parameter \\
                        --name /taskactivity/last-deployed-commit \\
                        --query 'Parameter.Value' \\
                        --output text 2>/dev/null || echo ""
                """,
                returnStdout: true
            ).trim()
        }
        
        if (result && result.length() > 0) {
            echo "Last deployed commit: ${result} (from SSM)"
            return result
        }
        
        echo "No deployed commit found in SSM - first deployment"
        return null
        
    } catch (Exception e) {
        echo "ERROR: Exception while reading last deployed commit from SSM: ${e.message}"
        return null
    }
}

/**
 * Record the successfully deployed Git commit SHA to SSM Parameter Store.
 * Called after a successful deployment so the next scheduled run can compare
 * the current HEAD commit against this value to skip unnecessary deployments.
 */
def recordDeployedCommit() {
    try {
        // Use git CLI as fallback since env.GIT_COMMIT can be null in some Jenkins configurations
        def commitSha = env.RESOLVED_GIT_COMMIT ?: env.GIT_COMMIT ?: sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
        
        if (!commitSha || commitSha == 'null') {
            echo "WARNING: Could not determine commit SHA - skipping SSM record"
            return
        }
        
        withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
            sh """
                aws ssm put-parameter \\
                    --name /taskactivity/last-deployed-commit \\
                    --value "${commitSha}" \\
                    --type String \\
                    --overwrite
            """
            echo "Recorded deployed commit ${commitSha} to SSM"
        }
    } catch (Exception e) {
        echo "WARNING: Failed to record deployed commit to SSM: ${e.message}"
        echo "This is non-critical - deployment eligibility checks may be affected on next scheduled build"
    }
}
