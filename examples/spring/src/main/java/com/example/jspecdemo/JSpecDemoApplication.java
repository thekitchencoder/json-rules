package com.example.jspecdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application demonstrating jspec integration.
 *
 * <p>Run with: {@code mvn spring-boot:run}
 * <p>Or: {@code java -jar target/jspec-spring-demo-1.0.0-SNAPSHOT.jar}
 */
@SpringBootApplication
public class JSpecDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(JSpecDemoApplication.class, args);
    }
}
