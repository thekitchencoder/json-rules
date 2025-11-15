package uk.codery.jspec.result;

/**
 * Common interface for all evaluation result types.
 *
 * <p>This interface provides a unified contract for both individual criterion results
 * ({@link EvaluationResult}) and criteria group results ({@link CriteriaGroupResult}),
 * enabling polymorphic handling of evaluation outcomes.
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link EvaluationResult} - Result of evaluating a single criterion</li>
 *   <li>{@link CriteriaGroupResult} - Result of evaluating a group of criteria combined with AND/OR logic</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SpecificationEvaluator evaluator = new SpecificationEvaluator();
 * EvaluationOutcome outcome = evaluator.evaluate(document, specification);
 *
 * // Process all individual criterion results
 * for (EvaluationResult result : outcome.evaluationResults()) {
 *     if (!result.matched()) {
 *         System.out.println("Criterion " + result.id() + " failed: " + result.reason());
 *     }
 * }
 *
 * // Process all criteria group results
 * for (CriteriaGroupResult groupResult : outcome.criteriaGroupResults()) {
 *     if (!groupResult.matched()) {
 *         System.out.println("Group " + groupResult.id() + " failed: " + groupResult.reason());
 *     }
 * }
 * }</pre>
 *
 * @see EvaluationResult
 * @see CriteriaGroupResult
 * @see EvaluationOutcome
 * @since 0.1.0
 */
public interface Result {
    /**
     * Returns the unique identifier of this result.
     *
     * <p>For {@link EvaluationResult}, this returns the criterion ID.
     * For {@link CriteriaGroupResult}, this returns the criteria group ID.
     *
     * @return the result identifier (never null)
     */
    String id();

    /**
     * Returns whether this result represents a match.
     *
     * <p>For {@link EvaluationResult}:
     * <ul>
     *   <li>Returns {@code true} only if the criterion state is {@link EvaluationState#MATCHED}</li>
     *   <li>Returns {@code false} for {@link EvaluationState#NOT_MATCHED} and {@link EvaluationState#UNDETERMINED}</li>
     * </ul>
     *
     * <p>For {@link CriteriaGroupResult}:
     * <ul>
     *   <li>Returns {@code true} if the group's junction logic is satisfied</li>
     *   <li>For AND groups: all criteria must match</li>
     *   <li>For OR groups: at least one criterion must match</li>
     * </ul>
     *
     * @return {@code true} if this result represents a match, {@code false} otherwise
     */
    boolean matched();

    /**
     * Returns a human-readable explanation of why this result did not match, or null if it matched.
     *
     * <p>For {@link EvaluationResult}:
     * <ul>
     *   <li>Returns {@code null} if state is {@link EvaluationState#MATCHED}</li>
     *   <li>Returns failure reason if state is {@link EvaluationState#UNDETERMINED}</li>
     *   <li>Returns missing paths or mismatch details if state is {@link EvaluationState#NOT_MATCHED}</li>
     * </ul>
     *
     * <p>For {@link CriteriaGroupResult}:
     * <ul>
     *   <li>Returns combined reasons from all constituent criteria</li>
     *   <li>Useful for debugging which criteria in the group failed</li>
     * </ul>
     *
     * <h3>Example Output:</h3>
     * <ul>
     *   <li>"Missing data at: age, email"</li>
     *   <li>"Unknown operator: $custom"</li>
     *   <li>"Non-matching values at {age={$gte=18}}"</li>
     *   <li>"Criterion definition not found"</li>
     * </ul>
     *
     * @return explanation of non-match, or {@code null} if matched
     */
    String reason();
}
