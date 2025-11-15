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

@Slf4j
public record SpecificationEvaluator(CriterionEvaluator evaluator) {

    public SpecificationEvaluator(){
        this(new CriterionEvaluator());
    }

    public EvaluationOutcome evaluate(Object doc, Specification specification) {
        log.info("Starting evaluation of specification '{}'", specification.id());

        // FIX: Use this.evaluator instead of creating new instance
        Map<String, EvaluationResult> ruleResults =
                specification.criteria().parallelStream()
                        .map(rule -> this.evaluator.evaluateRule(doc, rule))
                        .collect(Collectors.toMap(result -> result.criterion().id(), Function.identity()));

        log.debug("Evaluated {} criteria for specification '{}'", ruleResults.size(), specification.id());

        List<CriteriaGroupResult> results = specification.criteriaGroups().parallelStream().map(ruleSet -> {
            List<EvaluationResult> ruleSetResults = ruleSet.criteria().parallelStream()
                    .map(rule -> ruleResults.getOrDefault(rule.id(), this.evaluator.evaluateRule(doc, rule)))
                    .toList();

            boolean match = (Junction.AND == ruleSet.junction())
                    ? ruleSetResults.stream().allMatch(EvaluationResult::matched)
                    : ruleSetResults.stream().anyMatch(EvaluationResult::matched);
            return new CriteriaGroupResult(ruleSet.id(), ruleSet.junction(), ruleSetResults, match);
        }).toList();

        // Create summary
        EvaluationSummary summary = EvaluationSummary.from(ruleResults.values());

        log.info("Completed evaluation of specification '{}' - Total: {}, Matched: {}, Not Matched: {}, Undetermined: {}, Fully Determined: {}",
                   specification.id(), summary.totalRules(), summary.matchedRules(),
                   summary.notMatchedRules(), summary.undeterminedRules(), summary.fullyDetermined());

        return new EvaluationOutcome(specification.id(), new ArrayList<>(ruleResults.values()), results, summary);
    }
}
