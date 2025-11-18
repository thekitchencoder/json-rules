package com.example.jspecdemo.service;

import org.springframework.stereotype.Service;

import uk.codery.jspec.evaluator.SpecificationEvaluator;
import uk.codery.jspec.formatter.ResultFormatter;
import uk.codery.jspec.model.Specification;
import uk.codery.jspec.result.EvaluationOutcome;
import uk.codery.jspec.result.EvaluationResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for evaluating eligibility criteria.
 *
 * <p>Features:
 * <ul>
 *   <li>Caches evaluators for performance</li>
 *   <li>Provides convenient methods for common operations</li>
 *   <li>Thread-safe for concurrent requests</li>
 * </ul>
 */
@Service
public class EligibilityService {

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
        return outcome.find("loan-eligibility")
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
     * <p>Useful when specifications are loaded dynamically or user-defined.
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
