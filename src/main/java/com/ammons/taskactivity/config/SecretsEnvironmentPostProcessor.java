package com.ammons.taskactivity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Environment post-processor to support Docker secrets by reading credentials from files. This
 * allows the application to read DB_USERNAME and DB_PASSWORD from files when DB_USERNAME_FILE and
 * DB_PASSWORD_FILE environment variables are specified.
 * 
 * This is particularly useful for Docker Swarm secrets and Kubernetes mounted secrets.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
public class SecretsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger =
            LoggerFactory.getLogger(SecretsEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
            SpringApplication application) {
        Properties secrets = new Properties();

        // Check for username file
        String usernameFile = environment.getProperty("DB_USERNAME_FILE");
        if (usernameFile != null && !usernameFile.isEmpty()) {
            String username = readSecretFromFile(usernameFile, "DB_USERNAME");
            if (username != null) {
                secrets.setProperty("DB_USERNAME", username);
                logger.info("Loaded DB_USERNAME from file: {}", usernameFile);
            }
        }

        // Check for password file
        String passwordFile = environment.getProperty("DB_PASSWORD_FILE");
        if (passwordFile != null && !passwordFile.isEmpty()) {
            String password = readSecretFromFile(passwordFile, "DB_PASSWORD");
            if (password != null) {
                secrets.setProperty("DB_PASSWORD", password);
                logger.info("Loaded DB_PASSWORD from file: {}", passwordFile);
            }
        }

        // Add secrets to environment if any were loaded
        if (!secrets.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new PropertiesPropertySource("docker-secrets", secrets));
        }
    }

    /**
     * Reads a secret from a file and returns its content as a trimmed string.
     * 
     * @param filePath the path to the secret file
     * @param secretName the name of the secret (for logging purposes)
     * @return the secret content, or null if the file cannot be read
     */
    private String readSecretFromFile(String filePath, String secretName) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.warn("Secret file does not exist: {} (for {})", filePath, secretName);
                return null;
            }

            String content = Files.readString(path).trim();
            if (content.isEmpty()) {
                logger.warn("Secret file is empty: {} (for {})", filePath, secretName);
                return null;
            }

            logger.debug("Successfully read secret from file: {} (for {})", filePath, secretName);
            return content;

        } catch (IOException e) {
            logger.error("Failed to read secret from file: {} (for {})", filePath, secretName, e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error reading secret from file: {} (for {})", filePath,
                    secretName, e);
            return null;
        }
    }
}
