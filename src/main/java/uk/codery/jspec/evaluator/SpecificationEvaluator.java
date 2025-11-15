package uk.codery.jspec.evaluator;

import lombok.extern.slf4j.Slf4j;
import uk.codery.jspec.model.Junction;
import uk.codery.jspec.model.Specification;
import uk.codery.jspec.result.EvaluationOutcome;
import uk.codery.jspec.result.EvaluationResult;
import uk.codery.jspec.result.EvaluationSummary;
import uk.codery.jspec.result.CriteriaGroupResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates the evaluation of {@link Specification}s against documents.
 *
 * <p>The {@code SpecificationEvaluator} is the main entry point for evaluating
 * specifications. It coordinates the evaluation of individual criteria and
 * criteria groups, using parallel processing for optimal performance.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Parallel Evaluation:</b> Criteria are evaluated concurrently using parallel streams</li>
 *   <li><b>Result Caching:</b> Individual criterion results are cached for efficient group evaluation</li>
 *   <li><b>Graceful Degradation:</b> One failed criterion never stops the overall evaluation</li>
 *   <li><b>Comprehensive Results:</b> Returns detailed outcomes with summary statistics</li>
 *   <li><b>Thread-Safe:</b> Safe to use from multiple threads concurrently</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Evaluation</h3>
 * <pre>{@code
 * // Create evaluator
 * SpecificationEvaluator evaluator = new SpecificationEvaluator();
 *
 * // Define document
 * Map<String, Object> document = Map.of(
 *     "age", 25,
 *     "status", "active",
 *     "email", "user@example.com"
 * );
 *
 * // Define specification
 * Specification spec = Specification.builder()
 *     .id("user-validation")
 *     .addCriterion(
 *         Criterion.builder()
 *             .id("age-check")
 *             .field("age").gte(18)
 *             .build()
 *     )
 *     .build();
 *
 * // Evaluate
 * EvaluationOutcome outcome = evaluator.evaluate(document, spec);
 * }</pre>
 *
 * <h3>Custom Operators</h3>
 * <pre>{@code
 * // Create registry with custom operators
 * OperatorRegistry registry = OperatorRegistry.withDefaults();
 * registry.register("$length", (value, operand) -> {
 *     return value instanceof String &&
 *            ((String) value).length() == ((Number) operand).intValue();
 * });
 *
 * // Create evaluator with custom registry
 * CriterionEvaluator criterionEvaluator = new CriterionEvaluator(registry);
 * SpecificationEvaluator evaluator = new SpecificationEvaluator(criterionEvaluator);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe and immutable:
 * <ul>
 *   <li>Record class with final fields</li>
 *   <li>Uses parallel streams (thread-safe operations)</li>
 *   <li>No mutable shared state</li>
 *   <li>Safe to share across threads</li>
 * </ul>
 *
 * @param evaluator the criterion evaluator to use for individual criterion evaluation
 * @see Specification
 * @see CriterionEvaluator
 * @see EvaluationOutcome
 * @since 0.1.0
 */
@Slf4j
public record SpecificationEvaluator(CriterionEvaluator evaluator) {

    /**
     * Creates a SpecificationEvaluator with default built-in operators.
     *
     * <p>This is the recommended constructor for most use cases.
     * It creates an internal {@link CriterionEvaluator} with all 13 built-in
     * MongoDB-style operators.
     *
     * @see #SpecificationEvaluator(CriterionEvaluator)
     */
    public SpecificationEvaluator(){
        this(new CriterionEvaluator());
    }

    /**
     * Evaluates a specification against a document.
     *
     * <p>This method:
     * <ol>
     *   <li>Evaluates all individual criteria in parallel</li>
     *   <li>Caches criterion results for efficient group evaluation</li>
     *   <li>Evaluates all criteria groups (using cached results where possible)</li>
     *   <li>Generates summary statistics</li>
     *   <li>Returns comprehensive evaluation outcome</li>
     * </ol>
     *
     * <h3>Evaluation Process:</h3>
     * <ul>
     *   <li><b>Parallel Criterion Evaluation:</b> All criteria evaluated concurrently</li>
     *   <li><b>Result Caching:</b> Results stored in map by criterion ID</li>
     *   <li><b>Group Evaluation:</b> Groups use cached results when available</li>
     *   <li><b>Summary Generation:</b> Statistics computed from all results</li>
     * </ul>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * Map<String, Object> document = Map.of("age", 25, "status", "active");
     * Specification spec = loadSpecification();
     *
     * EvaluationOutcome outcome = evaluator.evaluate(document, spec);
     *
     * System.out.println("Total: " + outcome.summary().total());
     * System.out.println("Matched: " + outcome.summary().matched());
     * System.out.println("Fully Determined: " + outcome.summary().fullyDetermined());
     * }</pre>
     *
     * @param doc the document to evaluate (typically a Map, but can be any Object)
     * @param specification the specification containing criteria and groups
     * @return evaluation outcome with results and summary
     * @throws NullPointerException if specification is null
     * @see EvaluationOutcome
     * @see EvaluationResult
     * @see CriteriaGroupResult
     */
    public EvaluationOutcome evaluate(Object doc, Specification specification) {
        log.info("Starting evaluation of specification '{}'", specification.id());

        // FIX: Use this.evaluator instead of creating new instance
        Map<String, EvaluationResult> criteriaResultMap =
                specification.criteria().parallelStream()
                        .map(criterion -> this.evaluator.evaluateCriterion(doc, criterion))
                        .collect(Collectors.toMap(result -> result.criterion().id(), Function.identity()));

        log.debug("Evaluated {} criteria for specification '{}'", criteriaResultMap.size(), specification.id());

        List<CriteriaGroupResult> results = specification.criteriaGroups().parallelStream().map(criteriaGroup -> {
            List<EvaluationResult> criteriaGroupResults = criteriaGroup.criteria().parallelStream()
                    .map(criterion -> criteriaResultMap.getOrDefault(criterion.id(), this.evaluator.evaluateCriterion(doc, criterion)))
                    .toList();

            boolean match = (Junction.AND == criteriaGroup.junction())
                    ? criteriaGroupResults.stream().allMatch(EvaluationResult::matched)
                    : criteriaGroupResults.stream().anyMatch(EvaluationResult::matched);
            return new CriteriaGroupResult(criteriaGroup.id(), criteriaGroup.junction(), criteriaGroupResults, match);
        }).toList();

        // Create summary
        EvaluationSummary summary = EvaluationSummary.from(criteriaResultMap.values());

        log.info("Completed evaluation of specification '{}' - Total: {}, Matched: {}, Not Matched: {}, Undetermined: {}, Fully Determined: {}",
                   specification.id(), summary.total(), summary.matched(),
                   summary.notMatched(), summary.undetermined(), summary.fullyDetermined());

        return new EvaluationOutcome(specification.id(), new ArrayList<>(criteriaResultMap.values()), results, summary);
    }
}
