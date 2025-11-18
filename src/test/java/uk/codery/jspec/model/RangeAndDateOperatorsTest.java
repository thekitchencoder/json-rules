package uk.codery.jspec.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.codery.jspec.evaluator.CriterionEvaluator;
import uk.codery.jspec.result.EvaluationResult;
import uk.codery.jspec.result.EvaluationState;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for Tier 2 operators:
 * - Logical: $and, $or
 * - Range: $between
 * - Date: $dateBefore, $dateAfter
 */
class RangeAndDateOperatorsTest {

    private CriterionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CriterionEvaluator();
    }

    // ========== $and Operator Tests ==========

    @Test
    void and_allConditionsMatch_shouldMatch() {
        Map<String, Object> doc = Map.of("age", 25);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("age", Map.of("$and", List.of(
                Map.of("$gte", 18),
                Map.of("$lt", 65)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void and_oneConditionFails_shouldNotMatch() {
        Map<String, Object> doc = Map.of("age", 70);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("age", Map.of("$and", List.of(
                Map.of("$gte", 18),
                Map.of("$lt", 65)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void and_allConditionsFail_shouldNotMatch() {
        Map<String, Object> doc = Map.of("age", 10);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("age", Map.of("$and", List.of(
                Map.of("$gte", 18),
                Map.of("$lt", 65)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void and_withThreeConditions_shouldMatch() {
        Map<String, Object> doc = Map.of("price", 150);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("price", Map.of("$and", List.of(
                Map.of("$gt", 0),
                Map.of("$gte", 100),
                Map.of("$lte", 200)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void and_withNestedOperators_shouldMatch() {
        Map<String, Object> doc = Map.of("status", "ACTIVE");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("status", Map.of("$and", List.of(
                Map.of("$ne", "BANNED"),
                Map.of("$in", List.of("ACTIVE", "PENDING"))
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void and_emptyConditions_shouldMatch() {
        Map<String, Object> doc = Map.of("value", 42);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("value", Map.of("$and", List.of())));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void and_withInvalidOperand_shouldNotMatch() {
        Map<String, Object> doc = Map.of("value", 42);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("value", Map.of("$and", "not a list")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    // ========== $or Operator Tests ==========

    @Test
    void or_firstConditionMatches_shouldMatch() {
        Map<String, Object> doc = Map.of("score", 0);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("score", Map.of("$or", List.of(
                Map.of("$eq", 0),
                Map.of("$gte", 80)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void or_secondConditionMatches_shouldMatch() {
        Map<String, Object> doc = Map.of("score", 90);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("score", Map.of("$or", List.of(
                Map.of("$eq", 0),
                Map.of("$gte", 80)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void or_noConditionMatches_shouldNotMatch() {
        Map<String, Object> doc = Map.of("score", 50);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("score", Map.of("$or", List.of(
                Map.of("$eq", 0),
                Map.of("$gte", 80)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void or_allConditionsMatch_shouldMatch() {
        Map<String, Object> doc = Map.of("value", 100);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("value", Map.of("$or", List.of(
                Map.of("$gte", 50),
                Map.of("$lte", 150)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void or_withStringValues_shouldMatch() {
        Map<String, Object> doc = Map.of("status", "PENDING");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("status", Map.of("$or", List.of(
                Map.of("$eq", "ACTIVE"),
                Map.of("$eq", "PENDING"),
                Map.of("$eq", "PROCESSING")
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void or_emptyConditions_shouldNotMatch() {
        Map<String, Object> doc = Map.of("value", 42);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("value", Map.of("$or", List.of())));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void or_withInvalidOperand_shouldNotMatch() {
        Map<String, Object> doc = Map.of("value", 42);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("value", Map.of("$or", "not a list")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    // ========== $between Operator Tests ==========

    @Test
    void between_valueInRange_shouldMatch() {
        Map<String, Object> doc = Map.of("price", 150);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("price", Map.of("$between", List.of(100, 200))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void between_valueAtMinBoundary_shouldMatch() {
        Map<String, Object> doc = Map.of("price", 100);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("price", Map.of("$between", List.of(100, 200))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void between_valueAtMaxBoundary_shouldMatch() {
        Map<String, Object> doc = Map.of("price", 200);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("price", Map.of("$between", List.of(100, 200))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void between_valueBelowRange_shouldNotMatch() {
        Map<String, Object> doc = Map.of("price", 50);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("price", Map.of("$between", List.of(100, 200))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void between_valueAboveRange_shouldNotMatch() {
        Map<String, Object> doc = Map.of("price", 250);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("price", Map.of("$between", List.of(100, 200))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void between_withDoubles_shouldMatch() {
        Map<String, Object> doc = Map.of("rating", 4.5);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("rating", Map.of("$between", List.of(1.0, 5.0))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void between_withStrings_shouldMatch() {
        Map<String, Object> doc = Map.of("grade", "B");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("grade", Map.of("$between", List.of("A", "C"))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void between_withInvalidOperand_shouldNotMatch() {
        Map<String, Object> doc = Map.of("value", 42);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("value", Map.of("$between", "not a list")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void between_withWrongListSize_shouldNotMatch() {
        Map<String, Object> doc = Map.of("value", 42);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("value", Map.of("$between", List.of(1, 2, 3))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    // ========== $dateBefore Operator Tests ==========

    @Test
    void dateBefore_withIsoDateStrings_shouldMatch() {
        Map<String, Object> doc = Map.of("createdAt", "2024-01-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("createdAt", Map.of("$dateBefore", "2025-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void dateBefore_withIsoDateStrings_shouldNotMatch() {
        Map<String, Object> doc = Map.of("createdAt", "2025-06-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("createdAt", Map.of("$dateBefore", "2025-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void dateBefore_withIsoDateTime_shouldMatch() {
        Map<String, Object> doc = Map.of("timestamp", "2024-01-01T10:00:00Z");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("timestamp", Map.of("$dateBefore", "2024-01-01T12:00:00Z")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void dateBefore_withNowKeyword_pastDate_shouldMatch() {
        // Use a date from the past
        Map<String, Object> doc = Map.of("expiryDate", "2020-01-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("expiryDate", Map.of("$dateBefore", "now")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void dateBefore_withNowKeyword_futureDate_shouldNotMatch() {
        // Use a date in the future
        Map<String, Object> doc = Map.of("expiryDate", "2099-01-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("expiryDate", Map.of("$dateBefore", "now")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void dateBefore_withEpochMillis_shouldMatch() {
        // 2024-01-01T00:00:00Z = 1704067200000
        Map<String, Object> doc = Map.of("timestamp", 1704067200000L);
        // 2025-01-01T00:00:00Z = 1735689600000
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("timestamp", Map.of("$dateBefore", 1735689600000L)));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void dateBefore_sameDate_shouldNotMatch() {
        Map<String, Object> doc = Map.of("date", "2024-01-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("date", Map.of("$dateBefore", "2024-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void dateBefore_withInvalidDateFormat_shouldNotMatch() {
        Map<String, Object> doc = Map.of("date", "invalid-date");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("date", Map.of("$dateBefore", "2025-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    // ========== $dateAfter Operator Tests ==========

    @Test
    void dateAfter_withIsoDateStrings_shouldMatch() {
        Map<String, Object> doc = Map.of("createdAt", "2025-01-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("createdAt", Map.of("$dateAfter", "2024-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void dateAfter_withIsoDateStrings_shouldNotMatch() {
        Map<String, Object> doc = Map.of("createdAt", "2023-01-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("createdAt", Map.of("$dateAfter", "2024-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void dateAfter_withIsoDateTime_shouldMatch() {
        Map<String, Object> doc = Map.of("timestamp", "2024-01-01T14:00:00Z");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("timestamp", Map.of("$dateAfter", "2024-01-01T12:00:00Z")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void dateAfter_withNowKeyword_futureDate_shouldMatch() {
        // Use a date in the future
        Map<String, Object> doc = Map.of("scheduledDate", "2099-01-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("scheduledDate", Map.of("$dateAfter", "now")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void dateAfter_withNowKeyword_pastDate_shouldNotMatch() {
        // Use a date from the past
        Map<String, Object> doc = Map.of("scheduledDate", "2020-01-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("scheduledDate", Map.of("$dateAfter", "now")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void dateAfter_withEpochMillis_shouldMatch() {
        // 2025-01-01T00:00:00Z = 1735689600000
        Map<String, Object> doc = Map.of("timestamp", 1735689600000L);
        // 2024-01-01T00:00:00Z = 1704067200000
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("timestamp", Map.of("$dateAfter", 1704067200000L)));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void dateAfter_sameDate_shouldNotMatch() {
        Map<String, Object> doc = Map.of("date", "2024-01-01");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("date", Map.of("$dateAfter", "2024-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    @Test
    void dateAfter_withInvalidDateFormat_shouldNotMatch() {
        Map<String, Object> doc = Map.of("date", "invalid-date");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("date", Map.of("$dateAfter", "2024-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.NOT_MATCHED);
    }

    // ========== Combined Operator Tests ==========

    @Test
    void combinedAndOr_complexCondition_shouldMatch() {
        Map<String, Object> doc = Map.of("value", 50);
        // (value >= 0 AND value <= 100) OR value == -1
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("value", Map.of("$or", List.of(
                Map.of("$and", List.of(
                    Map.of("$gte", 0),
                    Map.of("$lte", 100)
                )),
                Map.of("$eq", -1)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void combinedBetweenAndNot_shouldMatch() {
        Map<String, Object> doc = Map.of("age", 30);
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("age", Map.of(
                "$between", List.of(18, 65),
                "$not", Map.of("$eq", 21)
            )));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    @Test
    void combinedDateOperators_dateRange_shouldMatch() {
        Map<String, Object> doc = Map.of("eventDate", "2024-06-15");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("eventDate", Map.of(
                "$dateAfter", "2024-01-01",
                "$dateBefore", "2025-01-01"
            )));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.MATCHED);
    }

    // ========== Missing Field Tests ==========

    @Test
    void and_withMissingField_shouldBeUndetermined() {
        Map<String, Object> doc = Map.of("other", "value");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("age", Map.of("$and", List.of(
                Map.of("$gte", 18),
                Map.of("$lt", 65)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.UNDETERMINED);
    }

    @Test
    void or_withMissingField_shouldBeUndetermined() {
        Map<String, Object> doc = Map.of("other", "value");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("score", Map.of("$or", List.of(
                Map.of("$eq", 0),
                Map.of("$gte", 80)
            ))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.UNDETERMINED);
    }

    @Test
    void between_withMissingField_shouldBeUndetermined() {
        Map<String, Object> doc = Map.of("other", "value");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("price", Map.of("$between", List.of(100, 200))));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.UNDETERMINED);
    }

    @Test
    void dateBefore_withMissingField_shouldBeUndetermined() {
        Map<String, Object> doc = Map.of("other", "value");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("expiryDate", Map.of("$dateBefore", "2025-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.UNDETERMINED);
    }

    @Test
    void dateAfter_withMissingField_shouldBeUndetermined() {
        Map<String, Object> doc = Map.of("other", "value");
        QueryCriterion criterion = new QueryCriterion("test",
            Map.of("createdAt", Map.of("$dateAfter", "2024-01-01")));

        EvaluationResult result = evaluator.evaluateQuery(doc, criterion);

        assertThat(result.state()).isEqualTo(EvaluationState.UNDETERMINED);
    }
}
