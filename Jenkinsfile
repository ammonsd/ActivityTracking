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
 * - Jenkins tools: Maven 3.9+, JDK 21
 * - AWS resources: ECR repository, ECS cluster, task definition
 */

pipeline {
    agent any
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'production'],
            description: 'Target deployment environment'
        )
        choice(
            name: 'DEPLOY_ACTION',
            choices: ['deploy', 'build-only', 'rollback'],
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
    }
    
    environment {
        // AWS Configuration - Update these for your environment
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '378010131175'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        ECR_REPOSITORY = 'taskactivity'
        
        // ECS Configuration - Environment-specific
        ECS_CLUSTER = "taskactivity-cluster-${params.ENVIRONMENT}"
        ECS_SERVICE = "taskactivity-service-${params.ENVIRONMENT}"
        TASK_DEFINITION_FAMILY = "taskactivity-task-${params.ENVIRONMENT}"
        
        // Docker Image Tags
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        IMAGE_FULL = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
        IMAGE_LATEST = "${ECR_REGISTRY}/${ECR_REPOSITORY}:latest-${params.ENVIRONMENT}"
        
        // Build Configuration
        MAVEN_OPTS = '-Xmx1024m -XX:MaxPermSize=256m'
        JAVA_HOME = tool name: 'JDK-21', type: 'jdk'
        MAVEN_HOME = tool name: 'Maven-3.9', type: 'maven'
        PATH = "${MAVEN_HOME}/bin:${JAVA_HOME}/bin:${env.PATH}"
        
        // Application version from pom.xml
        APP_VERSION = readMavenPom().getVersion()
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
    }
    
    stages {
        stage('Initialize') {
            steps {
                script {
                    echo "========================================="
                    echo "Task Activity Tracking - CI/CD Pipeline"
                    echo "========================================="
                    echo "Environment: ${params.ENVIRONMENT}"
                    echo "Action: ${params.DEPLOY_ACTION}"
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    echo "Branch: ${env.GIT_BRANCH}"
                    echo "Commit: ${env.GIT_COMMIT?.take(7)}"
                    echo "Application Version: ${APP_VERSION}"
                    echo "========================================="
                    
                    // Validation
                    if (params.ENVIRONMENT == 'production' && params.SKIP_TESTS) {
                        error("Cannot skip tests for production deployments!")
                    }
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
                expression { params.DEPLOY_ACTION != 'rollback' }
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
                expression { params.DEPLOY_ACTION != 'rollback' && !params.SKIP_TESTS }
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
                expression { params.DEPLOY_ACTION != 'rollback' }
            }
            steps {
                echo "Building Docker image: ${IMAGE_FULL}"
                script {
                    def buildArgs = params.NO_CACHE ? '--no-cache' : ''
                    
                    // Build Docker image
                    docker.build(
                        "${IMAGE_FULL}",
                        "${buildArgs} --build-arg BUILD_NUMBER=${env.BUILD_NUMBER} ."
                    )
                    
                    // Tag with latest for environment
                    sh "docker tag ${IMAGE_FULL} ${IMAGE_LATEST}"
                    
                    echo "Docker image built successfully"
                    sh "docker images | grep ${ECR_REPOSITORY}"
                }
            }
        }
        
        stage('Security Scan') {
            when {
                expression { params.DEPLOY_ACTION != 'rollback' }
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
                expression { params.DEPLOY_ACTION != 'rollback' && params.DEPLOY_ACTION != 'build-only' }
            }
            steps {
                echo "Pushing Docker image to ECR: ${ECR_REGISTRY}/${ECR_REPOSITORY}"
                script {
                    withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                        // Login to ECR
                        sh """
                            aws ecr get-login-password --region ${AWS_REGION} | \
                            docker login --username AWS --password-stdin ${ECR_REGISTRY}
                        """
                        
                        // Push images
                        sh "docker push ${IMAGE_FULL}"
                        sh "docker push ${IMAGE_LATEST}"
                        
                        echo "Images pushed successfully to ECR"
                    }
                }
            }
        }
        
        stage('Deploy to ECS') {
            when {
                expression { params.DEPLOY_ACTION == 'deploy' }
            }
            steps {
                echo "Deploying to ECS cluster: ${ECS_CLUSTER}"
                script {
                    withAWS(credentials: 'aws-credentials', region: "${AWS_REGION}") {
                        // Get current task definition
                        def taskDefJson = sh(
                            script: """
                                aws ecs describe-task-definition \
                                    --task-definition ${TASK_DEFINITION_FAMILY} \
                                    --query 'taskDefinition' \
                                    --output json
                            """,
                            returnStdout: true
                        ).trim()
                        
                        // Update image in task definition
                        def updatedTaskDef = sh(
                            script: """
                                echo '${taskDefJson}' | \
                                jq --arg IMAGE "${IMAGE_FULL}" \
                                '.containerDefinitions[0].image = \$IMAGE | 
                                 del(.taskDefinitionArn, .revision, .status, .requiresAttributes, 
                                     .compatibilities, .registeredAt, .registeredBy)'
                            """,
                            returnStdout: true
                        ).trim()
                        
                        // Register new task definition
                        def newTaskDefArn = sh(
                            script: """
                                echo '${updatedTaskDef}' | \
                                aws ecs register-task-definition \
                                    --cli-input-json file:///dev/stdin \
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
                expression { params.DEPLOY_ACTION == 'deploy' }
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
                echo "========================================="
                echo "✓ BUILD SUCCESSFUL"
                echo "========================================="
                echo "Environment: ${params.ENVIRONMENT}"
                echo "Action: ${params.DEPLOY_ACTION}"
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Image: ${IMAGE_FULL}"
                echo "========================================="
                
                // Send success notification (configure as needed)
                // emailext subject: "SUCCESS: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                //          body: "Build completed successfully",
                //          to: "team@example.com"
            }
        }
        
        failure {
            script {
                echo "========================================="
                echo "✗ BUILD FAILED"
                echo "========================================="
                echo "Environment: ${params.ENVIRONMENT}"
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Check console output for details"
                echo "========================================="
                
                // Send failure notification (configure as needed)
                // emailext subject: "FAILURE: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                //          body: "Build failed - please check console output",
                //          to: "team@example.com"
            }
        }
        
        unstable {
            echo 'Build is unstable - some tests may have failed'
        }
        
        always {
            // Clean up workspace
            cleanWs(
                deleteDirs: true,
                patterns: [
                    [pattern: 'target/**', type: 'INCLUDE'],
                    [pattern: '.mvn/**', type: 'INCLUDE']
                ]
            )
        }
    }
}
