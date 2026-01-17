/**
 * Utility to generate JWT tokens for Jenkins API authentication.
 * 
 * This standalone class can be run to generate a JWT token that Jenkins can use to authenticate
 * webhook requests to the production application.
 * 
 * Usage: javac -cp "target/*;target/classes" scripts/GenerateJenkinsToken.java java -cp
 * "target/*;target/classes;scripts" GenerateJenkinsToken
 * 
 * Author: Dean Ammons Date: January 2026
 */

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GenerateJenkinsToken {

    public static void main(String[] args) {
        // Read JWT secret from environment variable
        String jwtSecret = System.getenv("JWT_SECRET");

        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            System.err.println("ERROR: JWT_SECRET environment variable not set!");
            System.err.println("Set it with: export JWT_SECRET='your-secret-key'");
            System.err.println("The secret must match the one in your ECS environment variables.");
            System.exit(1);
        }

        // Token lifetime: 30 days (must match ECS JWT_EXPIRATION setting)
        long expirationMs = 30L * 24 * 60 * 60 * 1000; // 2592000000 ms

        System.out.println("=".repeat(70));
        System.out.println("Jenkins API Token Generator");
        System.out.println("=".repeat(70));
        System.out.println();

        try {
            String token = generateJenkinsToken(jwtSecret, expirationMs);

            System.out.println("✓ Token generated successfully!");
            System.out.println();
            System.out.println("Token Details:");
            System.out.println("-".repeat(70));
            System.out.println("  Subject: JENKINS_SERVICE");
            System.out.println("  Permission: JENKINS:NOTIFY");
            System.out.println("  Expires: " + new Date(System.currentTimeMillis() + expirationMs));
            System.out.println("  Lifetime: 30 days");
            System.out.println();
            System.out.println("JWT Token:");
            System.out.println("-".repeat(70));
            System.out.println(token);
            System.out.println("-".repeat(70));
            System.out.println();
            System.out.println("Next Steps:");
            System.out.println("  1. Copy the token above (entire line)");
            System.out.println("  2. Go to Jenkins: http://172.27.85.228:8081");
            System.out.println("  3. Manage Jenkins → Credentials → System → Global");
            System.out.println("  4. Find 'jenkins-api-token' credential");
            System.out.println("  5. Click Update → Paste token in Secret field → Save");
            System.out.println("  6. Test with a Jenkins build");
            System.out.println();
            System.out.println(
                    "Token expires on: " + new Date(System.currentTimeMillis() + expirationMs));
            System.out.println("Set reminder to regenerate: February 16, 2026");
            System.out.println("=".repeat(70));

        } catch (Exception e) {
            System.err.println("ERROR: Failed to generate token");
            System.err.println("Reason: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String generateJenkinsToken(String secret, long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("permissions", "JENKINS:NOTIFY");
        claims.put("type", "SERVICE_ACCOUNT");

        return Jwts.builder().subject("JENKINS_SERVICE").issuedAt(now).expiration(expiration)
                .id(UUID.randomUUID().toString()).claims(claims).signWith(key).compact();
    }
}
