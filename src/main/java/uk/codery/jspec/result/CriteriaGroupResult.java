package uk.codery.jspec.result;

import uk.codery.jspec.model.Junction;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of evaluating a {@link uk.codery.jspec.model.CriteriaGroup} against a document.
 *
 * <p>A criteria group combines multiple criteria using AND/OR logic (junction).
 * This result contains:
 * <ul>
 *   <li>The group identifier</li>
 *   <li>The junction used (AND or OR)</li>
 *   <li>Individual evaluation results for each criterion in the group</li>
 *   <li>Overall match status based on the junction logic</li>
 * </ul>
 *
 * <h2>Match Logic</h2>
 *
 * <p><b>AND Junction:</b>
 * <ul>
 *   <li>{@code matched() == true} when ALL criteria have {@link EvaluationState#MATCHED}</li>
 *   <li>{@code matched() == false} when ANY criterion has {@link EvaluationState#NOT_MATCHED} or {@link EvaluationState#UNDETERMINED}</li>
 * </ul>
 *
 * <p><b>OR Junction:</b>
 * <ul>
 *   <li>{@code matched() == true} when ANY criterion has {@link EvaluationState#MATCHED}</li>
 *   <li>{@code matched() == false} when ALL criteria have {@link EvaluationState#NOT_MATCHED} or {@link EvaluationState#UNDETERMINED}</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Checking Group Match Status</h3>
 * <pre>{@code
 * SpecificationEvaluator evaluator = new SpecificationEvaluator();
 * EvaluationOutcome outcome = evaluator.evaluate(document, specification);
 *
 * for (CriteriaGroupResult groupResult : outcome.criteriaGroupResults()) {
 *     if (groupResult.matched()) {
 *         System.out.println("Group " + groupResult.id() + " passed");
 *     } else {
 *         System.out.println("Group " + groupResult.id() + " failed:");
 *         System.out.println("  Junction: " + groupResult.junction());
 *         System.out.println("  Reason: " + groupResult.reason());
 *     }
 * }
 * }</pre>
 *
 * <h3>Inspecting Individual Criteria in Group</h3>
 * <pre>{@code
 * CriteriaGroupResult groupResult = outcome.criteriaGroupResults().get(0);
 *
 * System.out.println("Group: " + groupResult.id());
 * System.out.println("Junction: " + groupResult.junction());
 * System.out.println("Overall Match: " + groupResult.matched());
 *
 * for (EvaluationResult criterionResult : groupResult.evaluationResults()) {
 *     System.out.println("  - " + criterionResult.id() + ": " + criterionResult.state());
 *     if (!criterionResult.matched()) {
 *         System.out.println("    Reason: " + criterionResult.reason());
 *     }
 * }
 * }</pre>
 *
 * <h3>Debugging Failed Groups</h3>
 * <pre>{@code
 * CriteriaGroupResult groupResult = outcome.criteriaGroupResults().stream()
 *     .filter(r -> !r.matched())
 *     .findFirst()
 *     .orElseThrow();
 *
 * System.out.println("Failed group: " + groupResult.id());
 * System.out.println("Junction: " + groupResult.junction());
 * System.out.println("Combined failure reasons:");
 * System.out.println(groupResult.reason());
 *
 * // Count criteria by state
 * long matchedCount = groupResult.evaluationResults().stream()
 *     .filter(EvaluationResult::matched)
 *     .count();
 * long totalCount = groupResult.evaluationResults().size();
 *
 * System.out.println("Matched: " + matchedCount + "/" + totalCount);
 * }</pre>
 *
 * @param id the unique identifier for this criteria group
 * @param junction the boolean logic used to combine criteria (AND or OR)
 * @param evaluationResults the individual evaluation results for each criterion in the group
 * @param matched the overall match status based on junction logic
 * @see uk.codery.jspec.model.CriteriaGroup
 * @see EvaluationResult
 * @see Junction
 * @see Result
 * @since 0.1.0
 */
public record CriteriaGroupResult(
        String id,
        Junction junction,
        List<EvaluationResult> evaluationResults,
        boolean matched) implements Result {

    /**
     * Returns a combined reason string from all constituent criteria.
     *
     * <p>This method concatenates the reasons from all {@link EvaluationResult}s
     * in this group, separated by commas. Useful for understanding why a group
     * failed to match.
     *
     * <h3>Example Output:</h3>
     * <pre>
     * "Missing data at: age, Missing data at: email, Non-matching values at {status={$eq=active}}"
     * </pre>
     *
     * @return combined reasons from all criteria, or empty string if all criteria matched
     */
    @Override
    public String reason() {
        return evaluationResults.stream().map(EvaluationResult::reason).collect(Collectors.joining(", "));
    }

    /**
     * Returns a human-readable string representation of this criteria group result.
     *
     * <p>The output includes:
     * <ul>
     *   <li>Group ID</li>
     *   <li>Overall match status</li>
     *   <li>Junction type (AND/OR)</li>
     *   <li>Individual criterion results with match status</li>
     *   <li>Missing paths for criteria that failed</li>
     * </ul>
     *
     * <p><b>Note:</b> This format is for human readability and debugging.
     * For programmatic access, use the individual fields and methods.
     *
     * <h3>Example Output:</h3>
     * <pre>
     *   - employment-checks:
     *     match: true
     *     junction: AND
     *     criteria:
     *       - age-check: true
     *       - status-check: true
     *       - email-check: false
     *         missing: email
     * </pre>
     *
     * @return formatted string representation of this result
     */
    // TODO external formatters (YAML,JSON,Text,etc) rather than YAML embedded in the toString method
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  - ").append(id).append(":\n");
        sb.append("    ").append("match: ").append(matched).append("\n");
        sb.append("    ").append("junction: ").append(junction).append("\n");
        sb.append("    criteria:\n");
        evaluationResults.forEach(result -> {
            sb.append("      - ").append(result.criterion().id()).append(": ").append(result.matched()).append("\n");
            if (!result.missingPaths().isEmpty()) {
                sb.append("        missing: ").append(String.join(", ", result.missingPaths())).append("\n");
            }
        });
        return sb.toString();
    }
}
