package com.ammons.taskactivity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TaskactivityApplication - Application entry point.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class TaskactivityApplication {

	/**
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(TaskactivityApplication.class, args);
	}
}
