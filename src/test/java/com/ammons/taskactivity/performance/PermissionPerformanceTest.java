package com.ammons.taskactivity.performance;

import com.ammons.taskactivity.entity.Permission;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.repository.PermissionRepository;
import com.ammons.taskactivity.repository.RoleRepository;
import com.ammons.taskactivity.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for the database-driven permission system. Tests response times, concurrent
 * access, and scalability of permission checks.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("Permission System Performance Tests")
class PermissionPerformanceTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private static final int WARMUP_ITERATIONS = 10;
    private static final int TEST_ITERATIONS = 100;
    private static final int CONCURRENT_THREADS = 50;

    @BeforeEach
    void setUp() {
        // Use existing test data from test-data.sql instead of creating new entities
        // This avoids primary key conflicts
        // ADMIN (ID=1), USER (ID=2), GUEST (ID=3), EXPENSE_ADMIN (ID=4) already exist
        // with their associated permissions
    }

    private Roles getTestRole(Long roleId) {
        return roleRepository.findById(roleId).orElseThrow(
                () -> new RuntimeException("Role " + roleId + " not found in test data"));
    }

    @Test
    @DisplayName("Single permission check should complete within acceptable time")
    void singlePermissionCheckPerformance() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            permissionService.userHasPermission("admin", "TASK_ACTIVITY:CREATE");
        }

        // Measure
        List<Long> responseTimes = new ArrayList<>();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Instant start = Instant.now();
            permissionService.userHasPermission("admin", "TASK_ACTIVITY:CREATE");
            Instant end = Instant.now();
            responseTimes.add(Duration.between(start, end).toMillis());
        }

        // Calculate statistics
        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.println("=== Single Permission Check Performance ===");
        System.out.println("Average time: " + avgTime + "ms");
        System.out.println("Min time: " + minTime + "ms");
        System.out.println("Max time: " + maxTime + "ms");

        // Assert reasonable performance (adjust threshold as needed)
        assertThat(avgTime).isLessThan(100.0); // Average should be under 100ms
        assertThat(maxTime).isLessThan(500); // Max should be under 500ms
    }

    @Test
    @DisplayName("Multiple permission checks should scale linearly")
    void multiplePermissionCheckPerformance() {
        String[] permissionChecks =
                {"TASK_ACTIVITY:CREATE", "TASK_ACTIVITY:READ", "TASK_ACTIVITY:READ_ALL"};

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            for (String permission : permissionChecks) {
                permissionService.userHasPermission("admin", permission);
            }
        }

        // Measure
        List<Long> responseTimes = new ArrayList<>();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Instant start = Instant.now();
            for (String permission : permissionChecks) {
                permissionService.userHasPermission("admin", permission);
            }
            Instant end = Instant.now();
            responseTimes.add(Duration.between(start, end).toMillis());
        }

        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("=== Multiple Permission Check Performance ===");
        System.out.println("Checking " + permissionChecks.length + " permissions");
        System.out.println("Average time: " + avgTime + "ms");
        System.out.println("Max time: " + maxTime + "ms");

        // Should still be reasonably fast
        assertThat(avgTime).isLessThan(200.0);
        assertThat(maxTime).isLessThan(1000);
    }

    @Test
    @DisplayName("Get all user permissions should complete quickly")
    void getAllUserPermissionsPerformance() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            permissionService.getUserPermissions("admin");
        }

        // Measure
        List<Long> responseTimes = new ArrayList<>();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Instant start = Instant.now();
            Set<Permission> permissions = permissionService.getUserPermissions("admin");
            Instant end = Instant.now();
            responseTimes.add(Duration.between(start, end).toMillis());

            assertThat(permissions).isNotEmpty();
        }

        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("=== Get All User Permissions Performance ===");
        System.out.println("Average time: " + avgTime + "ms");
        System.out.println("Max time: " + maxTime + "ms");

        assertThat(avgTime).isLessThan(150.0);
        assertThat(maxTime).isLessThan(750);
    }

    @Test
    @DisplayName("Concurrent permission checks should handle load")
    void concurrentPermissionCheckPerformance() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<Future<Long>> futures = new ArrayList<>();

        // Submit concurrent tasks
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadNum = i;
            Future<Long> future = executor.submit(() -> {
                Instant start = Instant.now();
                permissionService.userHasPermission("admin", "TASK_ACTIVITY:CREATE");
                Instant end = Instant.now();
                return Duration.between(start, end).toMillis();
            });
            futures.add(future);
        }

        // Collect results
        List<Long> responseTimes = new ArrayList<>();
        for (Future<Long> future : futures) {
            responseTimes.add(future.get());
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Calculate statistics
        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.println("=== Concurrent Permission Check Performance ===");
        System.out.println("Threads: " + CONCURRENT_THREADS);
        System.out.println("Average time: " + avgTime + "ms");
        System.out.println("Min time: " + minTime + "ms");
        System.out.println("Max time: " + maxTime + "ms");

        // Under concurrent load, times may be higher
        assertThat(avgTime).isLessThan(500.0);
        assertThat(maxTime).isLessThan(2000);
    }

    @Test
    @DisplayName("Permission checks for different roles should have similar performance")
    void crossRolePerformanceComparison() {
        String[] roles = {"admin", "user", "guest"};
        Map<String, Double> rolePerformance = new HashMap<>();

        for (String role : roles) {
            List<Long> responseTimes = new ArrayList<>();

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                permissionService.getUserPermissions(role);
            }

            // Measure
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                Instant start = Instant.now();
                permissionService.getUserPermissions(role);
                Instant end = Instant.now();
                responseTimes.add(Duration.between(start, end).toMillis());
            }

            double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            rolePerformance.put(role, avgTime);
        }

        System.out.println("=== Cross-Role Performance Comparison ===");
        rolePerformance.forEach(
                (role, avgTime) -> System.out.println(role + ": " + avgTime + "ms average"));

        // All roles should complete within reasonable time
        rolePerformance.values().forEach(avgTime -> assertThat(avgTime).isLessThan(200.0));
    }

    @Test
    @DisplayName("Permission check with invalid role should fail fast")
    void invalidRolePerformance() {
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Instant start = Instant.now();
            boolean result =
                    permissionService.userHasPermission("NONEXISTENT_ROLE", "TASK_ACTIVITY:CREATE");
            Instant end = Instant.now();
            responseTimes.add(Duration.between(start, end).toMillis());

            assertThat(result).isFalse();
        }

        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("=== Invalid Role Performance ===");
        System.out.println("Average time: " + avgTime + "ms");
        System.out.println("Max time: " + maxTime + "ms");

        // Should fail fast
        assertThat(avgTime).isLessThan(100.0);
        assertThat(maxTime).isLessThan(300);
    }

    @Test
    @DisplayName("Bulk permission operations should be efficient")
    void bulkPermissionOperationPerformance() {
        // Test getting permissions for multiple roles
        String[] roles = {"admin", "user", "guest"};

        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Instant start = Instant.now();

            for (String role : roles) {
                permissionService.getUserPermissions(role);
            }

            Instant end = Instant.now();
            responseTimes.add(Duration.between(start, end).toMillis());
        }

        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("=== Bulk Permission Operation Performance ===");
        System.out.println("Roles processed: " + roles.length);
        System.out.println("Average time: " + avgTime + "ms");
        System.out.println("Max time: " + maxTime + "ms");

        assertThat(avgTime).isLessThan(400.0);
        assertThat(maxTime).isLessThan(1500);
    }

    @Test
    @DisplayName("Permission check performance under sustained load")
    void sustainedLoadPerformance() throws InterruptedException {
        int sustainedIterations = 1000;
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(10);

        System.out.println("=== Sustained Load Test ===");
        System.out.println("Iterations: " + sustainedIterations);
        System.out.println("Thread pool size: 10");

        Instant testStart = Instant.now();

        // Submit all tasks
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < sustainedIterations; i++) {
            futures.add(executor.submit(() -> {
                Instant start = Instant.now();
                permissionService.userHasPermission("admin", "TASK_ACTIVITY:CREATE");
                Instant end = Instant.now();
                responseTimes.add(Duration.between(start, end).toMillis());
            }));
        }

        // Wait for all to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                // Log but don't fail test
                System.err.println("Task execution failed: " + e.getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        Instant testEnd = Instant.now();
        long totalTime = Duration.between(testStart, testEnd).toMillis();

        // Calculate statistics
        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        double throughput = (sustainedIterations * 1000.0) / totalTime; // requests per second

        System.out.println("Total test time: " + totalTime + "ms");
        System.out.println("Average response time: " + avgTime + "ms");
        System.out.println("Min response time: " + minTime + "ms");
        System.out.println("Max response time: " + maxTime + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");

        // System should maintain performance under sustained load
        assertThat(avgTime).isLessThan(200.0);
        assertThat(throughput).isGreaterThan(10.0); // At least 10 requests per second
    }
}

