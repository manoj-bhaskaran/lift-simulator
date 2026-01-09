package com.liftsimulator.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Lift Config Service Spring Boot application.
 * This provides the admin backend for managing lift simulator configurations.
 */
@SpringBootApplication
public class LiftConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiftConfigServiceApplication.class, args);
    }
}
