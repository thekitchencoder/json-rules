package uk.codery.rules.result;

import java.util.List;

/**
 * The outcome of evaluating a specification against a document.
 *
 * <p>Contains the results of evaluating individual rules, rule sets,
 * and a summary showing evaluation completeness.
 */
public record EvaluationOutcome(
        String specificationId,
        List<EvaluationResult> ruleResults,
        List<RuleSetResult> ruleSetResults,
        EvaluationSummary summary) {
}
