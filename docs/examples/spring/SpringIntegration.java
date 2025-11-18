/*
 * SpringIntegration.java - JSpec Spring Boot Integration Example
 *
 * This file demonstrates how to integrate jspec with Spring Boot.
 * It contains multiple classes that would typically be in separate files.
 *
 * Contents:
 * 1. JSpecConfig - Configuration class
 * 2. EligibilityService - Service layer with caching
 * 3. ValidationController - REST API example
 * 4. Application - Main application class
 *
 * Dependencies (add to pom.xml):
 * <dependencies>
 *     <dependency>
 *         <groupId>org.springframework.boot</groupId>
 *         <artifactId>spring-boot-starter-web</artifactId>
 *     </dependency>
 *     <dependency>
 *         <groupId>uk.codery</groupId>
 *         <artifactId>jspec</artifactId>
 *         <version>0.4.0</version>
 *     </dependency>
 * </dependencies>
 */

package com.example.jspecdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import uk.codery.jspec.builder.CompositeCriterionBuilder;
import uk.codery.jspec.builder.SpecificationBuilder;
import uk.codery.jspec.evaluator.SpecificationEvaluator;
import uk.codery.jspec.formatter.JsonResultFormatter;
import uk.codery.jspec.formatter.ResultFormatter;
import uk.codery.jspec.model.CompositeCriterion;
import uk.codery.jspec.model.QueryCriterion;
import uk.codery.jspec.model.Specification;
import uk.codery.jspec.result.EvaluationOutcome;
import uk.codery.jspec.result.EvaluationResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// =============================================================================
// 1. CONFIGURATION CLASS
// =============================================================================

/**
 * Spring configuration for jspec components.
 *
 * Configures:
 * - YAML ObjectMapper for parsing specifications
 * - Pre-built specifications as beans
 * - Result formatters
 */
@Configuration
class JSpecConfig {

    /**
     * ObjectMapper configured for YAML parsing.
     * Use this to load specifications from YAML files.
     */
    @Bean
    public ObjectMapper yamlMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    /**
     * JSON result formatter for API responses.
     */
    @Bean
    public ResultFormatter jsonFormatter() {
        return new JsonResultFormatter(true); // pretty-printed
    }

    /**
     * Example: Load specification from classpath resource.
     *
     * Place your specification file at: src/main/resources/specifications/user-eligibility.yaml
     */
    @Bean
    public Specification eligibilitySpec(
            ObjectMapper yamlMapper,
            @Value("classpath:specifications/user-eligibility.yaml") Resource specResource
    ) throws IOException {
        return yamlMapper.readValue(specResource.getInputStream(), Specification.class);
    }

    /**
     * Example: Build specification programmatically.
     *
     * This approach is useful when criteria are defined in code.
     */
    @Bean
    public Specification loanApprovalSpec() {
        // Define individual criteria
        QueryCriterion ageCheck = QueryCriterion.builder()
                .id("age-check")
                .field("age").gte(18).and().lte(65)
                .build();

        QueryCriterion incomeCheck = QueryCriterion.builder()
                .id("income-check")
                .field("annualIncome").gte(30000)
                .build();

        QueryCriterion employmentCheck = QueryCriterion.builder()
                .id("employment-check")
                .field("employmentStatus").in("employed", "self-employed")
                .build();

        QueryCriterion creditScore = QueryCriterion.builder()
                .id("credit-score")
                .field("creditScore").gte(650)
                .build();

        QueryCriterion noDefaultHistory = QueryCriterion.builder()
                .id("no-defaults")
                .field("hasDefaultHistory").eq(false)
                .build();

        // Composite: All criteria must match
        CompositeCriterion loanEligibility = CompositeCriterion.builder()
                .id("loan-eligibility")
                .and()
                .criteria(ageCheck, incomeCheck, employmentCheck, creditScore, noDefaultHistory)
                .build();

        // Build the complete specification
        return Specification.builder()
                .id("loan-approval")
                .addCriterion(ageCheck)
                .addCriterion(incomeCheck)
                .addCriterion(employmentCheck)
                .addCriterion(creditScore)
                .addCriterion(noDefaultHistory)
                .addCriterion(loanEligibility)
                .build();
    }
}

// =============================================================================
// 2. SERVICE LAYER
// =============================================================================

/**
 * Service for evaluating eligibility criteria.
 *
 * Features:
 * - Caches evaluators for performance
 * - Provides convenient methods for common operations
 * - Thread-safe for concurrent requests
 */
@Service
class EligibilityService {

    private final Map<String, SpecificationEvaluator> evaluatorCache = new ConcurrentHashMap<>();
    private final Specification loanApprovalSpec;
    private final ResultFormatter jsonFormatter;

    /**
     * Constructor injection of dependencies.
     */
    public EligibilityService(
            Specification loanApprovalSpec,
            ResultFormatter jsonFormatter
    ) {
        this.loanApprovalSpec = loanApprovalSpec;
        this.jsonFormatter = jsonFormatter;

        // Pre-cache common evaluators
        this.evaluatorCache.put(loanApprovalSpec.id(), new SpecificationEvaluator(loanApprovalSpec));
    }

    /**
     * Evaluate a document against the loan approval specification.
     *
     * @param applicant the applicant data as a Map
     * @return evaluation outcome with all results
     */
    public EvaluationOutcome evaluateLoanApplication(Map<String, Object> applicant) {
        SpecificationEvaluator evaluator = evaluatorCache.get(loanApprovalSpec.id());
        return evaluator.evaluate(applicant);
    }

    /**
     * Check if applicant is eligible for a loan.
     *
     * @param applicant the applicant data
     * @return true if the composite "loan-eligibility" criterion matches
     */
    public boolean isEligibleForLoan(Map<String, Object> applicant) {
        EvaluationOutcome outcome = evaluateLoanApplication(applicant);

        // Check the composite criterion result
        return outcome.findResult("loan-eligibility")
                .map(result -> result.state().matched())
                .orElse(false);
    }

    /**
     * Get detailed evaluation results as JSON.
     *
     * @param applicant the applicant data
     * @return JSON formatted evaluation results
     */
    public String getDetailedResults(Map<String, Object> applicant) {
        EvaluationOutcome outcome = evaluateLoanApplication(applicant);
        return jsonFormatter.format(outcome);
    }

    /**
     * Evaluate with a dynamic specification.
     *
     * Useful when specifications are loaded dynamically or user-defined.
     *
     * @param document the document to evaluate
     * @param spec the specification to use
     * @return evaluation outcome
     */
    public EvaluationOutcome evaluateWithSpec(Object document, Specification spec) {
        // Get or create cached evaluator
        SpecificationEvaluator evaluator = evaluatorCache.computeIfAbsent(
                spec.id(),
                id -> new SpecificationEvaluator(spec)
        );
        return evaluator.evaluate(document);
    }

    /**
     * Get list of failed criteria for an applicant.
     *
     * @param applicant the applicant data
     * @return list of criterion IDs that did not match
     */
    public List<String> getFailedCriteria(Map<String, Object> applicant) {
        EvaluationOutcome outcome = evaluateLoanApplication(applicant);

        return outcome.results().stream()
                .filter(result -> result.state().notMatched())
                .map(EvaluationResult::id)
                .toList();
    }

    /**
     * Get evaluation summary statistics.
     *
     * @param applicant the applicant data
     * @return summary with matched/not-matched/undetermined counts
     */
    public EvaluationSummaryDto getSummary(Map<String, Object> applicant) {
        EvaluationOutcome outcome = evaluateLoanApplication(applicant);
        var summary = outcome.summary();

        return new EvaluationSummaryDto(
                summary.matched(),
                summary.notMatched(),
                summary.undetermined(),
                summary.total()
        );
    }

    /**
     * DTO for evaluation summary.
     */
    public record EvaluationSummaryDto(
            int matched,
            int notMatched,
            int undetermined,
            int total
    ) {}
}

// =============================================================================
// 3. REST CONTROLLER
// =============================================================================

/**
 * REST API for loan eligibility evaluation.
 *
 * Endpoints:
 * - POST /api/loan/evaluate - Full evaluation results
 * - POST /api/loan/eligible - Simple eligibility check
 * - POST /api/loan/summary - Evaluation summary
 * - POST /api/loan/failures - List of failed criteria
 */
@RestController
@RequestMapping("/api/loan")
class ValidationController {

    private final EligibilityService eligibilityService;

    public ValidationController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /**
     * Evaluate loan application and return detailed results.
     *
     * Example request:
     * POST /api/loan/evaluate
     * {
     *   "age": 35,
     *   "annualIncome": 75000,
     *   "employmentStatus": "employed",
     *   "creditScore": 720,
     *   "hasDefaultHistory": false
     * }
     */
    @PostMapping("/evaluate")
    public ResponseEntity<String> evaluate(@RequestBody Map<String, Object> applicant) {
        String results = eligibilityService.getDetailedResults(applicant);
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(results);
    }

    /**
     * Simple eligibility check.
     *
     * Returns: { "eligible": true/false }
     */
    @PostMapping("/eligible")
    public ResponseEntity<Map<String, Boolean>> checkEligibility(
            @RequestBody Map<String, Object> applicant
    ) {
        boolean eligible = eligibilityService.isEligibleForLoan(applicant);
        return ResponseEntity.ok(Map.of("eligible", eligible));
    }

    /**
     * Get evaluation summary.
     *
     * Returns: { "matched": 5, "notMatched": 1, "undetermined": 0, "total": 6 }
     */
    @PostMapping("/summary")
    public ResponseEntity<EligibilityService.EvaluationSummaryDto> getSummary(
            @RequestBody Map<String, Object> applicant
    ) {
        return ResponseEntity.ok(eligibilityService.getSummary(applicant));
    }

    /**
     * Get list of failed criteria.
     *
     * Returns: { "failedCriteria": ["income-check", "credit-score"] }
     */
    @PostMapping("/failures")
    public ResponseEntity<Map<String, List<String>>> getFailures(
            @RequestBody Map<String, Object> applicant
    ) {
        List<String> failures = eligibilityService.getFailedCriteria(applicant);
        return ResponseEntity.ok(Map.of("failedCriteria", failures));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

// =============================================================================
// 4. MAIN APPLICATION
// =============================================================================

/**
 * Spring Boot application entry point.
 *
 * Run with: mvn spring-boot:run
 * Or: java -jar target/jspec-demo.jar
 */
@SpringBootApplication
public class SpringIntegration {

    public static void main(String[] args) {
        SpringApplication.run(SpringIntegration.class, args);
    }
}

// =============================================================================
// SAMPLE SPECIFICATION FILE (src/main/resources/specifications/user-eligibility.yaml)
// =============================================================================
/*
id: user-eligibility
criteria:
  - id: age-check
    query:
      age:
        $gte: 18
  - id: status-check
    query:
      status:
        $eq: active
  - id: email-exists
    query:
      email:
        $exists: true
  - id: eligibility
    junction: AND
    criteria:
      - age-check
      - status-check
      - email-exists
*/

// =============================================================================
// SAMPLE APPLICATION.YAML (src/main/resources/application.yaml)
// =============================================================================
/*
server:
  port: 8080

spring:
  application:
    name: jspec-demo

logging:
  level:
    uk.codery.jspec: DEBUG
*/
