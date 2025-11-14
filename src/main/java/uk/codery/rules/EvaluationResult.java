package uk.codery.rules;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.function.Predicate.not;

/**
 * Result of evaluating a single rule against a document.
 *
 * <p>Uses a tri-state model to distinguish between:
 * <ul>
 *   <li>MATCHED - Rule evaluated successfully and condition is true</li>
 *   <li>NOT_MATCHED - Rule evaluated successfully and condition is false</li>
 *   <li>UNDETERMINED - Rule could not be evaluated due to errors or missing data</li>
 * </ul>
 *
 * <p>The {@code failureReason} provides details when state is UNDETERMINED,
 * helping developers debug issues with rules or data.
 */
public record EvaluationResult(
        Rule rule,
        EvaluationState state,
        List<String> missingPaths,
        String failureReason) implements Result {

    public EvaluationResult {
        missingPaths = Optional.ofNullable(missingPaths)
                .map(Collections::unmodifiableList)
                .orElseGet(Collections::emptyList);
    }

    /**
     * Creates an UNDETERMINED result for a missing rule definition.
     */
    public static EvaluationResult missing(Rule rule){
        return missing(rule.id());
    }

    /**
     * Creates an UNDETERMINED result for a missing rule definition.
     */
    public static EvaluationResult missing(String id){
        return new EvaluationResult(
                new Rule(id),
                EvaluationState.UNDETERMINED,
                Collections.singletonList("rule definition"),
                "Rule definition not found"
        );
    }

    /**
     * Implements the Result interface contract.
     *
     * @return true only if state is MATCHED, false otherwise
     */
    @Override
    public boolean matched() {
        return state == EvaluationState.MATCHED;
    }

    /**
     * Returns true if the evaluation was deterministic (not UNDETERMINED).
     *
     * @return true if state is MATCHED or NOT_MATCHED
     */
    public boolean isDetermined() {
        return state != EvaluationState.UNDETERMINED;
    }

    @Override
    public String id(){
        return rule.id();
    }

    @Override
    public String reason() {
        return switch (state) {
            case MATCHED -> null;
            case UNDETERMINED -> failureReason != null ? failureReason :
                    (missingPaths.isEmpty() ? "Evaluation failed" : "Missing data at: " + String.join(", ", missingPaths));
            case NOT_MATCHED -> missingPaths.isEmpty() ?
                    String.format("Non-matching values at %s", rule.query()) :
                    "Missing data at: " + String.join(", ", missingPaths);
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rule ").append(rule).append("\n");
        sb.append("  State: ").append(state).append("\n");
        sb.append("  Matched: ").append(matched()).append("\n");

        if(!missingPaths.isEmpty()) {
            sb.append("  Missing paths: ").append(String.join(", ", missingPaths)).append("\n");
        }

        Optional.ofNullable(reason()).ifPresent(reason ->
            sb.append("  Reason: ").append(reason).append("\n")
        );
        return sb.toString();
    }

}
