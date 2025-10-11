package com.ammons.taskactivity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * TaskactivityApplication - Application entry point.
 *
 * @author Dean Ammons
 * @version 1.0
 */
@SpringBootApplication
@EnableConfigurationProperties
public class TaskactivityApplication {

	/**
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(TaskactivityApplication.class, args);
	}
}
