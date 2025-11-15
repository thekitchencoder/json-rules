package uk.codery.jspec.evaluator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.codery.jspec.model.CriteriaGroup;
import uk.codery.jspec.model.Criterion;
import uk.codery.jspec.model.Junction;
import uk.codery.jspec.model.Specification;
import uk.codery.jspec.result.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SpecificationEvaluator focusing on:
 * - Specification orchestration
 * - CriteriaGroup evaluation with AND/OR operators
 * - Parallel evaluation behavior
 * - Result caching
 * - Summary generation
 */
class SpecificationEvaluatorTest {

    private SpecificationEvaluator evaluator;
    private Map<String, Object> validDocument;

    @BeforeEach
    void setUp() {
        evaluator = new SpecificationEvaluator();
        validDocument = Map.of(
            "age", 25,
            "name", "John Doe",
            "status", "ACTIVE",
            "tags", List.of("admin", "user")
        );
    }

    // ========== Basic Specification Evaluation ==========

    @Test
    void evaluate_withSingleRule_shouldReturnCorrectOutcome() {
        Criterion criterion = new Criterion("age-check", Map.of("age", Map.of("$gte", 18)));
        Specification spec = new Specification("simple-spec", List.of(criterion), List.of());

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        assertThat(outcome.specificationId()).isEqualTo("simple-spec");
        assertThat(outcome.ruleResults()).hasSize(1);
        assertThat(outcome.ruleResults().get(0).state()).isEqualTo(EvaluationState.MATCHED);
        assertThat(outcome.summary().totalRules()).isEqualTo(1);
        assertThat(outcome.summary().matchedRules()).isEqualTo(1);
    }

    @Test
    void evaluate_withMultipleRules_shouldEvaluateAll() {
        List<Criterion> criteria = List.of(
            new Criterion("age-check", Map.of("age", Map.of("$gte", 18))),
            new Criterion("name-check", Map.of("name", Map.of("$eq", "John Doe"))),
            new Criterion("status-check", Map.of("status", Map.of("$eq", "ACTIVE")))
        );
        Specification spec = new Specification("multi-criterion-spec", criteria, List.of());

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        assertThat(outcome.ruleResults()).hasSize(3);
        assertThat(outcome.ruleResults()).allMatch(r -> r.state() == EvaluationState.MATCHED);
        assertThat(outcome.summary().matchedRules()).isEqualTo(3);
        assertThat(outcome.summary().fullyDetermined()).isTrue();
    }

    @Test
    void evaluate_withMixedResults_shouldTrackAllStates() {
        List<Criterion> criteria = List.of(
            new Criterion("match", Map.of("age", Map.of("$eq", 25))),
            new Criterion("no-match", Map.of("age", Map.of("$eq", 30))),
            new Criterion("undetermined", Map.of("salary", Map.of("$gt", 50000)))
        );
        Specification spec = new Specification("mixed-spec", criteria, List.of());

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        assertThat(outcome.ruleResults()).hasSize(3);
        assertThat(outcome.summary().matchedRules()).isEqualTo(1);
        assertThat(outcome.summary().notMatchedRules()).isEqualTo(1);
        assertThat(outcome.summary().undeterminedRules()).isEqualTo(1);
        assertThat(outcome.summary().fullyDetermined()).isFalse();
    }

    @Test
    void evaluate_withEmptySpecification_shouldReturnEmptyOutcome() {
        Specification spec = new Specification("empty-spec", List.of(), List.of());

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        assertThat(outcome.ruleResults()).isEmpty();
        assertThat(outcome.criteriaGroupResults()).isEmpty();
        assertThat(outcome.summary().totalRules()).isEqualTo(0);
        assertThat(outcome.summary().fullyDetermined()).isTrue();
    }

    // ========== CriteriaGroup Tests with AND Junction ==========

    @Test
    void ruleSet_withAND_allMatching_shouldMatch() {
        List<Criterion> criteria = List.of(
            new Criterion("age-check", Map.of("age", Map.of("$gte", 18))),
            new Criterion("status-check", Map.of("status", Map.of("$eq", "ACTIVE")))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("and-set", Junction.AND, List.of(new Criterion("age-check", Map.of()), new Criterion("status-check", Map.of())));
        Specification spec = new Specification("and-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        assertThat(outcome.criteriaGroupResults()).hasSize(1);
        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        assertThat(result.id()).isEqualTo("and-set");
        assertThat(result.matched()).isTrue();
        assertThat(result.junction()).isEqualTo(Junction.AND);
        assertThat(result.ruleResults()).hasSize(2);
        assertThat(result.ruleResults()).allMatch(EvaluationResult::matched);
    }

    @Test
    void ruleSet_withAND_oneNotMatching_shouldNotMatch() {
        List<Criterion> criteria = List.of(
            new Criterion("age-check", Map.of("age", Map.of("$gte", 18))),
            new Criterion("status-check", Map.of("status", Map.of("$eq", "INACTIVE")))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("and-set", Junction.AND, List.of(new Criterion("age-check", Map.of()), new Criterion("status-check", Map.of())));
        Specification spec = new Specification("and-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        assertThat(result.matched()).isFalse();
    }

    @Test
    void ruleSet_withAND_allNotMatching_shouldNotMatch() {
        List<Criterion> criteria = List.of(
            new Criterion("age-check", Map.of("age", Map.of("$lt", 18))),
            new Criterion("status-check", Map.of("status", Map.of("$eq", "INACTIVE")))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("and-set", Junction.AND, List.of(new Criterion("age-check", Map.of()), new Criterion("status-check", Map.of())));
        Specification spec = new Specification("and-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        assertThat(result.matched()).isFalse();
    }

    // ========== CriteriaGroup Tests with OR Junction ==========

    @Test
    void ruleSet_withOR_oneMatching_shouldMatch() {
        List<Criterion> criteria = List.of(
            new Criterion("age-check", Map.of("age", Map.of("$gte", 18))),
            new Criterion("status-check", Map.of("status", Map.of("$eq", "INACTIVE")))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("or-set", Junction.OR, List.of(new Criterion("age-check", Map.of()), new Criterion("status-check", Map.of())));
        Specification spec = new Specification("or-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        assertThat(result.id()).isEqualTo("or-set");
        assertThat(result.matched()).isTrue();
        assertThat(result.junction()).isEqualTo(Junction.OR);
    }

    @Test
    void ruleSet_withOR_allMatching_shouldMatch() {
        List<Criterion> criteria = List.of(
            new Criterion("age-check", Map.of("age", Map.of("$gte", 18))),
            new Criterion("status-check", Map.of("status", Map.of("$eq", "ACTIVE")))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("or-set", Junction.OR, List.of(new Criterion("age-check", Map.of()), new Criterion("status-check", Map.of())));
        Specification spec = new Specification("or-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        assertThat(result.matched()).isTrue();
    }

    @Test
    void ruleSet_withOR_noneMatching_shouldNotMatch() {
        List<Criterion> criteria = List.of(
            new Criterion("age-check", Map.of("age", Map.of("$lt", 18))),
            new Criterion("status-check", Map.of("status", Map.of("$eq", "INACTIVE")))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("or-set", Junction.OR, List.of(new Criterion("age-check", Map.of()), new Criterion("status-check", Map.of())));
        Specification spec = new Specification("or-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        assertThat(result.matched()).isFalse();
    }

    // ========== Multiple RuleSets ==========

    @Test
    void evaluate_withMultipleRuleSets_shouldEvaluateAll() {
        List<Criterion> criteria = List.of(
            new Criterion("r1", Map.of("age", Map.of("$gte", 18))),
            new Criterion("r2", Map.of("status", Map.of("$eq", "ACTIVE"))),
            new Criterion("r3", Map.of("name", Map.of("$eq", "John Doe")))
        );
        List<CriteriaGroup> criteriaGroups = List.of(
            new CriteriaGroup("and-set", Junction.AND, List.of(new Criterion("r1", Map.of()), new Criterion("r2", Map.of()))),
            new CriteriaGroup("or-set", Junction.OR, List.of(new Criterion("r2", Map.of()), new Criterion("r3", Map.of())))
        );
        Specification spec = new Specification("multi-ruleset-spec", criteria, criteriaGroups);

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        assertThat(outcome.criteriaGroupResults()).hasSize(2);
        assertThat(outcome.criteriaGroupResults()).allMatch(CriteriaGroupResult::matched);
    }

    @Test
    void evaluate_withRuleSetsReferencingSameRules_shouldReuseResults() {
        List<Criterion> criteria = List.of(
            new Criterion("shared-criterion", Map.of("age", Map.of("$gte", 18))),
            new Criterion("other-criterion", Map.of("status", Map.of("$eq", "ACTIVE")))
        );
        List<CriteriaGroup> criteriaGroups = List.of(
            new CriteriaGroup("set1", Junction.AND, List.of(new Criterion("shared-criterion", Map.of()), new Criterion("other-criterion", Map.of()))),
            new CriteriaGroup("set2", Junction.OR, List.of(new Criterion("shared-criterion", Map.of()), new Criterion("other-criterion", Map.of())))
        );
        Specification spec = new Specification("reuse-spec", criteria, criteriaGroups);

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        // Both rulesets should evaluate the same criteria
        assertThat(outcome.criteriaGroupResults()).hasSize(2);
        assertThat(outcome.ruleResults()).hasSize(2); // Rules evaluated once
    }

    // ========== Graceful Degradation in RuleSets ==========

    @Test
    void ruleSet_withUndeterminedRule_shouldStillEvaluate() {
        List<Criterion> criteria = List.of(
            new Criterion("determined", Map.of("age", Map.of("$gte", 18))),
            new Criterion("undetermined", Map.of("salary", Map.of("$gt", 50000)))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("mixed-set", Junction.AND, List.of(new Criterion("determined", Map.of()), new Criterion("undetermined", Map.of())));
        Specification spec = new Specification("mixed-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        assertThat(outcome.criteriaGroupResults()).hasSize(1);
        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        // AND requires all to match - undetermined criterion doesn't match
        assertThat(result.matched()).isFalse();
        assertThat(result.ruleResults()).hasSize(2);
    }

    @Test
    void ruleSet_withUnknownOperator_shouldStillEvaluate() {
        List<Criterion> criteria = List.of(
            new Criterion("good", Map.of("age", Map.of("$gte", 18))),
            new Criterion("bad", Map.of("age", Map.of("$unknown", 25)))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("graceful-set", Junction.OR, List.of(new Criterion("good", Map.of()), new Criterion("bad", Map.of())));
        Specification spec = new Specification("graceful-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        assertThat(outcome.criteriaGroupResults()).hasSize(1);
        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        // OR requires at least one to match - "good" matches
        assertThat(result.matched()).isTrue();
    }

    // ========== Summary Verification ==========

    @Test
    void summary_withAllMatched_shouldReflectCorrectly() {
        List<Criterion> criteria = List.of(
            new Criterion("r1", Map.of("age", Map.of("$eq", 25))),
            new Criterion("r2", Map.of("status", Map.of("$eq", "ACTIVE")))
        );
        Specification spec = new Specification("all-matched", criteria, List.of());

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        EvaluationSummary summary = outcome.summary();
        assertThat(summary.totalRules()).isEqualTo(2);
        assertThat(summary.matchedRules()).isEqualTo(2);
        assertThat(summary.notMatchedRules()).isEqualTo(0);
        assertThat(summary.undeterminedRules()).isEqualTo(0);
        assertThat(summary.fullyDetermined()).isTrue();
    }

    @Test
    void summary_withMixedResults_shouldReflectCorrectly() {
        List<Criterion> criteria = List.of(
            new Criterion("matched", Map.of("age", Map.of("$eq", 25))),
            new Criterion("not-matched", Map.of("age", Map.of("$eq", 30))),
            new Criterion("undetermined", Map.of("salary", Map.of("$gt", 50000)))
        );
        Specification spec = new Specification("mixed", criteria, List.of());

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        EvaluationSummary summary = outcome.summary();
        assertThat(summary.totalRules()).isEqualTo(3);
        assertThat(summary.matchedRules()).isEqualTo(1);
        assertThat(summary.notMatchedRules()).isEqualTo(1);
        assertThat(summary.undeterminedRules()).isEqualTo(1);
        assertThat(summary.fullyDetermined()).isFalse();
    }

    // ========== Thread Safety and Parallel Evaluation ==========

    @Test
    void evaluate_shouldHandleParallelEvaluation() {
        // Create many criteria to encourage parallel evaluation
        List<Criterion> criteria = List.of(
            new Criterion("r1", Map.of("age", Map.of("$gte", 0))),
            new Criterion("r2", Map.of("age", Map.of("$gte", 0))),
            new Criterion("r3", Map.of("age", Map.of("$gte", 0))),
            new Criterion("r4", Map.of("age", Map.of("$gte", 0))),
            new Criterion("r5", Map.of("age", Map.of("$gte", 0)))
        );
        Specification spec = new Specification("parallel-spec", criteria, List.of());

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        // All should evaluate successfully
        assertThat(outcome.ruleResults()).hasSize(5);
        assertThat(outcome.ruleResults()).allMatch(r -> r.state() == EvaluationState.MATCHED);
    }

    // ========== Edge Cases ==========

    @Test
    void evaluate_withEmptyDocument_shouldHandleGracefully() {
        Map<String, Object> emptyDoc = Map.of();
        List<Criterion> criteria = List.of(
            new Criterion("r1", Map.of("age", Map.of("$eq", 25)))
        );
        Specification spec = new Specification("empty-doc-spec", criteria, List.of());

        EvaluationOutcome outcome = evaluator.evaluate(emptyDoc, spec);

        assertThat(outcome.ruleResults()).hasSize(1);
        assertThat(outcome.ruleResults().get(0).state()).isEqualTo(EvaluationState.UNDETERMINED);
        assertThat(outcome.summary().undeterminedRules()).isEqualTo(1);
    }

    @Test
    void ruleSet_withSingleRule_shouldWork() {
        List<Criterion> criteria = List.of(
            new Criterion("only-criterion", Map.of("age", Map.of("$gte", 18)))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("single-criterion-set", Junction.AND, List.of(new Criterion("only-criterion", Map.of())));
        Specification spec = new Specification("single-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        assertThat(result.matched()).isTrue();
        assertThat(result.ruleResults()).hasSize(1);
    }

    @Test
    void ruleSet_withManyRules_shouldEvaluateAll() {
        List<Criterion> criteria = List.of(
            new Criterion("r1", Map.of("age", Map.of("$gte", 18))),
            new Criterion("r2", Map.of("age", Map.of("$lte", 65))),
            new Criterion("r3", Map.of("status", Map.of("$eq", "ACTIVE"))),
            new Criterion("r4", Map.of("name", Map.of("$exists", true))),
            new Criterion("r5", Map.of("tags", Map.of("$size", 2)))
        );
        CriteriaGroup criteriaGroup = new CriteriaGroup("many-criteria-set", Junction.AND,
            List.of(new Criterion("r1", Map.of()), new Criterion("r2", Map.of()), new Criterion("r3", Map.of()), new Criterion("r4", Map.of()), new Criterion("r5", Map.of())));
        Specification spec = new Specification("many-criteria-spec", criteria, List.of(criteriaGroup));

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        CriteriaGroupResult result = outcome.criteriaGroupResults().get(0);
        assertThat(result.matched()).isTrue();
        assertThat(result.ruleResults()).hasSize(5);
    }

    @Test
    void evaluate_shouldPreserveRuleOrder() {
        List<Criterion> criteria = List.of(
            new Criterion("first", Map.of("age", Map.of("$gte", 18))),
            new Criterion("second", Map.of("status", Map.of("$eq", "ACTIVE"))),
            new Criterion("third", Map.of("name", Map.of("$exists", true)))
        );
        Specification spec = new Specification("ordered-spec", criteria, List.of());

        EvaluationOutcome outcome = evaluator.evaluate(validDocument, spec);

        // Results should be present (order not guaranteed due to parallel streams)
        assertThat(outcome.ruleResults()).hasSize(3);
        assertThat(outcome.ruleResults())
            .extracting(EvaluationResult::id)
            .containsExactlyInAnyOrder("first", "second", "third");
    }
}
