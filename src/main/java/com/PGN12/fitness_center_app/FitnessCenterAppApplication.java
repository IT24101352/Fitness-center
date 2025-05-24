package com.PGN12.fitness_center_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Fitness Center Spring Boot application.
 */
@SpringBootApplication
public class
FitnessCenterAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(FitnessCenterAppApplication.class, args);
		System.out.println("\nFitness Center Backend Application Started!");
		System.out.println("Access API endpoints at http://localhost:8080 (or your configured port)");
		System.out.println("Ensure  data directory 'src/main/resources/data/' is created if it doesn't exist.");
	}

}
