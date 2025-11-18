package com.example.jspecdemo.controller;

import com.example.jspecdemo.service.EligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for loan eligibility evaluation.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/loan/evaluate - Full evaluation results</li>
 *   <li>POST /api/loan/eligible - Simple eligibility check</li>
 *   <li>POST /api/loan/summary - Evaluation summary</li>
 *   <li>POST /api/loan/failures - List of failed criteria</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/loan")
public class ValidationController {

    private final EligibilityService eligibilityService;

    public ValidationController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /**
     * Evaluate loan application and return detailed results.
     *
     * <p>Example request:
     * <pre>
     * POST /api/loan/evaluate
     * {
     *   "age": 35,
     *   "annualIncome": 75000,
     *   "employmentStatus": "employed",
     *   "creditScore": 720,
     *   "hasDefaultHistory": false
     * }
     * </pre>
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
     * @return { "eligible": true/false }
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
     * @return { "matched": 5, "notMatched": 1, "undetermined": 0, "total": 6 }
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
     * @return { "failedCriteria": ["income-check", "credit-score"] }
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
