package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.security.RequirePermission;
import com.ammons.taskactivity.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API endpoint for Jenkins build notifications.
 * 
 * <p>
 * This controller provides endpoints for Jenkins CI/CD pipeline to trigger email notifications on
 * build success or failure. These endpoints are called from Jenkins using the Post build actions
 * with HTTP Request plugin or via curl in pipeline scripts.
 * 
 * <p>
 * Example Jenkins pipeline usage:
 * 
 * <pre>
 * post {
 *     success {
 *         script {
 *             sh """
 *                 curl -X POST https://taskactivity.example.com/api/jenkins/build-success \
 *                      -H "Content-Type: application/json" \
 *                      -H "Authorization: Bearer ${JENKINS_API_TOKEN}" \
 *                      -d '{
 *                          "buildNumber": "${BUILD_NUMBER}",
 *                          "branch": "${GIT_BRANCH}",
 *                          "commit": "${GIT_COMMIT}",
 *                          "buildUrl": "${BUILD_URL}"
 *                      }'
 *             """
 *         }
 *     }
 * }
 * </pre>
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@RestController
@RequestMapping("/api/jenkins")
@Tag(name = "Jenkins Build Notifications", description = "Endpoints for CI/CD build notifications")
public class JenkinsBuildNotificationController {

    private static final Logger logger =
            LoggerFactory.getLogger(JenkinsBuildNotificationController.class);

    private final EmailService emailService;

    public JenkinsBuildNotificationController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Notify build success.
     * 
     * @param request the build notification request
     * @return success response
     */
    @PostMapping("/build-success")
    @Operation(summary = "Send build success notification",
            description = "Sends an email notification when a Jenkins build succeeds. Called from Jenkins pipeline post-success actions.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                            description = "Notification sent successfully",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                            description = "Invalid request parameters",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                            description = "Unauthorized - Invalid or missing JWT token")})
    @RequirePermission(resource = "JENKINS", action = "NOTIFY")
    public ResponseEntity<ApiResponse<String>> notifyBuildSuccess(@RequestBody @Parameter(
            description = "Build notification details including build number, branch, commit, and URL",
            required = true) BuildNotificationRequest request) {

        logger.info("Received build success notification for build: {}", request.buildNumber());

        if (request.buildNumber() == null || request.buildNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Build number is required"));
        }

        if (request.buildUrl() == null || request.buildUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Build URL is required"));
        }

        emailService.sendBuildSuccessNotification(request.buildNumber(), request.branch(),
                        request.commit(), request.buildUrl(), request.environment());

        return ResponseEntity.ok(ApiResponse
                .success("Build success notification sent for build: " + request.buildNumber()));
    }

    /**
     * Notify build failure.
     * 
     * @param request the build notification request
     * @return success response
     */
    @PostMapping("/build-failure")
    @Operation(summary = "Send build failure notification",
            description = "Sends an email notification when a Jenkins build fails. Called from Jenkins pipeline post-failure actions.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                            description = "Notification sent successfully",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                            description = "Invalid request parameters",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                            description = "Unauthorized - Invalid or missing JWT token")})
    @RequirePermission(resource = "JENKINS", action = "NOTIFY")
    public ResponseEntity<ApiResponse<String>> notifyBuildFailure(@RequestBody @Parameter(
            description = "Build notification details including build number, branch, commit, URLs, and optional console log URL",
            required = true) BuildNotificationRequest request) {

        logger.info("Received build failure notification for build: {}", request.buildNumber());

        if (request.buildNumber() == null || request.buildNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Build number is required"));
        }

        if (request.buildUrl() == null || request.buildUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Build URL is required"));
        }

        String consoleUrl = request.consoleUrl() != null ? request.consoleUrl()
                : request.buildUrl() + "console";

        emailService.sendBuildFailureNotification(request.buildNumber(), request.branch(),
                        request.commit(), request.buildUrl(), consoleUrl, request.environment());

        return ResponseEntity.ok(ApiResponse
                .success("Build failure notification sent for build: " + request.buildNumber()));
    }

    /**
     * Notify deploy success.
     * 
     * @param request the deploy notification request
     * @return success response
     */
    @PostMapping("/deploy-success")
    @Operation(summary = "Send deploy success notification",
                    description = "Sends an email notification when a Jenkins deployment succeeds. Called from Jenkins pipeline post-deploy success actions.",
                    security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                                    description = "Notification sent successfully",
                                    content = @Content(schema = @Schema(
                                                    implementation = ApiResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                                    description = "Invalid request parameters",
                                    content = @Content(schema = @Schema(
                                                    implementation = ApiResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                                    description = "Unauthorized - Invalid or missing JWT token")})
    @RequirePermission(resource = "JENKINS", action = "NOTIFY")
    public ResponseEntity<ApiResponse<String>> notifyDeploySuccess(@RequestBody @Parameter(
                    description = "Deploy notification details including build number, branch, commit, URL, and environment",
                    required = true) DeployNotificationRequest request) {

            logger.info("Received deploy success notification for build: {} to {}",
                            request.buildNumber(), request.environment());

            if (request.buildNumber() == null || request.buildNumber().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                                    .body(ApiResponse.error("Build number is required"));
            }

            if (request.deployUrl() == null || request.deployUrl().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                                    .body(ApiResponse.error("Deploy URL is required"));
            }

            if (request.environment() == null || request.environment().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                                    .body(ApiResponse.error("Environment is required"));
            }

            emailService.sendDeploySuccessNotification(request.buildNumber(), request.branch(),
                            request.commit(), request.deployUrl(), request.environment());

            return ResponseEntity
                            .ok(ApiResponse.success("Deploy success notification sent for build: "
                                            + request.buildNumber()));
    }

    /**
     * Notify deploy failure.
     * 
     * @param request the deploy notification request
     * @return success response
     */
    @PostMapping("/deploy-failure")
    @Operation(summary = "Send deploy failure notification",
                    description = "Sends an email notification when a Jenkins deployment fails. Called from Jenkins pipeline post-deploy failure actions.",
                    security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                                    description = "Notification sent successfully",
                                    content = @Content(schema = @Schema(
                                                    implementation = ApiResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                                    description = "Invalid request parameters",
                                    content = @Content(schema = @Schema(
                                                    implementation = ApiResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                                    description = "Unauthorized - Invalid or missing JWT token")})
    @RequirePermission(resource = "JENKINS", action = "NOTIFY")
    public ResponseEntity<ApiResponse<String>> notifyDeployFailure(@RequestBody @Parameter(
                    description = "Deploy notification details including build number, branch, commit, URLs, environment, and optional console log URL",
                    required = true) DeployNotificationRequest request) {

            logger.info("Received deploy failure notification for build: {} to {}",
                            request.buildNumber(), request.environment());

            if (request.buildNumber() == null || request.buildNumber().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                                    .body(ApiResponse.error("Build number is required"));
            }

            if (request.deployUrl() == null || request.deployUrl().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                                    .body(ApiResponse.error("Deploy URL is required"));
            }

            if (request.environment() == null || request.environment().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                                    .body(ApiResponse.error("Environment is required"));
            }

            String consoleUrl = request.consoleUrl() != null ? request.consoleUrl()
                            : request.deployUrl() + "console";

            emailService.sendDeployFailureNotification(request.buildNumber(), request.branch(),
                            request.commit(), request.deployUrl(), consoleUrl,
                            request.environment());

            return ResponseEntity
                            .ok(ApiResponse.success("Deploy failure notification sent for build: "
                                            + request.buildNumber()));
    }

    /**
     * Request DTO for Jenkins build notifications.
     * 
     * @param buildNumber the Jenkins build number
     * @param branch the Git branch (e.g., "main", "develop")
     * @param commit the Git commit hash (short or full)
     * @param buildUrl the Jenkins build URL
     * @param consoleUrl optional console log URL (defaults to buildUrl/console)
     */
    @Schema(description = "Jenkins build notification request")
    public record BuildNotificationRequest(
            @Schema(description = "Jenkins build number", example = "72",
                    required = true) String buildNumber,
            @Schema(description = "Git branch name", example = "main") String branch,
            @Schema(description = "Git commit hash", example = "abc1234") String commit,
            @Schema(description = "Jenkins build URL",
                    example = "https://jenkins.example.com/job/taskactivity/72/",
                    required = true) String buildUrl,
            @Schema(description = "Console log URL (optional, defaults to buildUrl/console)",
                                    example = "https://jenkins.example.com/job/taskactivity/72/console") String consoleUrl,
                    @Schema(description = "Build environment",
                                    example = "production") String environment,
            @Schema(description = "What triggered the build",
                                    example = "scm") String triggeredBy) {
    }

    /**
     * Request DTO for Jenkins deploy notifications.
     * 
     * @param buildNumber the Jenkins build number
     * @param branch the Git branch (e.g., "main", "develop")
     * @param commit the Git commit hash (short or full)
     * @param deployUrl the Jenkins deploy URL
     * @param consoleUrl optional console log URL (defaults to deployUrl/console)
     * @param environment the deployment environment (e.g., "staging", "production")
     */
    @Schema(description = "Jenkins deploy notification request")
    public record DeployNotificationRequest(
                    @Schema(description = "Jenkins build number", example = "72",
                                    required = true) String buildNumber,
                    @Schema(description = "Git branch name", example = "main") String branch,
                    @Schema(description = "Git commit hash", example = "abc1234") String commit,
                    @Schema(description = "Jenkins deploy URL",
                                    example = "https://jenkins.example.com/job/taskactivity-deploy/72/",
                                    required = true) String deployUrl,
                    @Schema(description = "Console log URL (optional, defaults to deployUrl/console)",
                                    example = "https://jenkins.example.com/job/taskactivity-deploy/72/console") String consoleUrl,
                    @Schema(description = "Deployment environment", example = "production",
                                    required = true) String environment) {
    }
}
