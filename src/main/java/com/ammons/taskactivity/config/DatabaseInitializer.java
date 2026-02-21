package com.ammons.taskactivity.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Database Initializer for AWS profile Runs schema.sql and data.sql after Spring Boot startup
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Configuration
@Profile("aws")
public class DatabaseInitializer {

    @Bean
    public CommandLineRunner initDatabase(DataSource dataSource) {
        return args -> {
            try {
                // Check if tables already exist in public schema
                try (var connection = dataSource.getConnection();
                        var statement = connection.createStatement();
                        var rs = statement.executeQuery(
                                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users')")) {

                    if (rs.next() && rs.getBoolean(1)) {
                        System.out.println(
                                "[DATABASE] Tables already initialized, skipping schema creation");
                        return;
                    }
                }

                System.out.println("[DATABASE] Initializing database schema...");

                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema.sql"));
                populator.addScript(new ClassPathResource("data.sql"));
                populator.setContinueOnError(false);
                populator.execute(dataSource);

                System.out.println("[DATABASE] Database initialization completed successfully");

            } catch (Exception e) {
                System.err.println("[DATABASE] Error initializing database: " + e.getMessage());
                e.printStackTrace();
                // Don't throw - let app continue even if init fails
            }
        };
    }
}
