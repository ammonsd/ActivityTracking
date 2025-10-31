package com.ammons.taskactivity.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Test if the hash in database matches 'admin123'
        String databaseHash = "$2a$10$xqVzLkUlKQVV5KqLx3nB3.OMcYHMGxZLG8rXJhQXG9qPCRXLJJkVu";
        String testPassword = "admin123";

        boolean matches = encoder.matches(testPassword, databaseHash);
        System.out.println("Testing password: " + testPassword);
        System.out.println("Against hash: " + databaseHash);
        System.out.println("Match result: " + matches);

        // Generate a fresh hash
        String freshHash = encoder.encode(testPassword);
        System.out.println("\nFresh hash for '" + testPassword + "':");
        System.out.println(freshHash);

        // Verify the fresh hash works
        boolean freshMatches = encoder.matches(testPassword, freshHash);
        System.out.println("Fresh hash verification: " + freshMatches);
    }
}
