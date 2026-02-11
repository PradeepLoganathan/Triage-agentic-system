package com.pradeepl.triage;

import com.pradeepl.triage.domain.EvaluationResults;
import com.pradeepl.triage.domain.EvaluationResults.HallucinationResult;
import com.pradeepl.triage.domain.EvaluationResults.ToxicityResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationResultsTest {

    private static final ToxicityResult PASS_TOX = new ToxicityResult("clean", true);
    private static final ToxicityResult FAIL_TOX = new ToxicityResult("toxic content found", false);
    private static final HallucinationResult PASS_HALL = new HallucinationResult("grounded", true);
    private static final HallucinationResult FAIL_HALL = new HallucinationResult("hallucination detected", false);

    @Nested
    class EmptyResults {

        @Test
        void allFieldsAreNull() {
            EvaluationResults results = EvaluationResults.empty();
            assertThat(results.summaryToxicity()).isNull();
            assertThat(results.remediationToxicity()).isNull();
            assertThat(results.evidenceHallucination()).isNull();
            assertThat(results.triageHallucination()).isNull();
            assertThat(results.summaryHallucination()).isNull();
        }

        @Test
        void allPassedIsTrueWhenAllNull() {
            assertThat(EvaluationResults.empty().allPassed()).isTrue();
        }

        @Test
        void failureCountIsZeroWhenAllNull() {
            assertThat(EvaluationResults.empty().failureCount()).isEqualTo(0);
        }
    }

    @Nested
    class AllPassed {

        @Test
        void trueWhenAllEvaluationsPass() {
            EvaluationResults results = new EvaluationResults(
                PASS_TOX, PASS_TOX, PASS_HALL, PASS_HALL, PASS_HALL
            );
            assertThat(results.allPassed()).isTrue();
        }

        @Test
        void falseWhenOneToxicityFails() {
            EvaluationResults results = new EvaluationResults(
                FAIL_TOX, PASS_TOX, PASS_HALL, PASS_HALL, PASS_HALL
            );
            assertThat(results.allPassed()).isFalse();
        }

        @Test
        void falseWhenOneHallucinationFails() {
            EvaluationResults results = new EvaluationResults(
                PASS_TOX, PASS_TOX, FAIL_HALL, PASS_HALL, PASS_HALL
            );
            assertThat(results.allPassed()).isFalse();
        }

        @Test
        void trueWhenSomeAreNullAndRestPass() {
            EvaluationResults results = new EvaluationResults(
                PASS_TOX, null, null, PASS_HALL, null
            );
            assertThat(results.allPassed()).isTrue();
        }
    }

    @Nested
    class FailureCount {

        @Test
        void countsZeroWhenAllPass() {
            EvaluationResults results = new EvaluationResults(
                PASS_TOX, PASS_TOX, PASS_HALL, PASS_HALL, PASS_HALL
            );
            assertThat(results.failureCount()).isEqualTo(0);
        }

        @Test
        void countsOneFailure() {
            EvaluationResults results = new EvaluationResults(
                FAIL_TOX, PASS_TOX, PASS_HALL, PASS_HALL, PASS_HALL
            );
            assertThat(results.failureCount()).isEqualTo(1);
        }

        @Test
        void countsAllFiveFailures() {
            EvaluationResults results = new EvaluationResults(
                FAIL_TOX, FAIL_TOX, FAIL_HALL, FAIL_HALL, FAIL_HALL
            );
            assertThat(results.failureCount()).isEqualTo(5);
        }

        @Test
        void nullsAreNotCountedAsFailures() {
            EvaluationResults results = new EvaluationResults(
                null, null, FAIL_HALL, null, null
            );
            assertThat(results.failureCount()).isEqualTo(1);
        }
    }

    @Nested
    class WithMethods {

        @Test
        void withSummaryToxicitySetsField() {
            EvaluationResults results = EvaluationResults.empty()
                .withSummaryToxicity(PASS_TOX);
            assertThat(results.summaryToxicity()).isEqualTo(PASS_TOX);
            assertThat(results.remediationToxicity()).isNull();
        }

        @Test
        void withRemediationToxicitySetsField() {
            EvaluationResults results = EvaluationResults.empty()
                .withRemediationToxicity(FAIL_TOX);
            assertThat(results.remediationToxicity()).isEqualTo(FAIL_TOX);
            assertThat(results.summaryToxicity()).isNull();
        }

        @Test
        void withEvidenceHallucinationSetsField() {
            EvaluationResults results = EvaluationResults.empty()
                .withEvidenceHallucination(PASS_HALL);
            assertThat(results.evidenceHallucination()).isEqualTo(PASS_HALL);
        }

        @Test
        void withTriageHallucinationSetsField() {
            EvaluationResults results = EvaluationResults.empty()
                .withTriageHallucination(FAIL_HALL);
            assertThat(results.triageHallucination()).isEqualTo(FAIL_HALL);
        }

        @Test
        void withSummaryHallucinationSetsField() {
            EvaluationResults results = EvaluationResults.empty()
                .withSummaryHallucination(PASS_HALL);
            assertThat(results.summaryHallucination()).isEqualTo(PASS_HALL);
        }

        @Test
        void chainingWithMethodsBuildsCompleteResults() {
            EvaluationResults results = EvaluationResults.empty()
                .withSummaryToxicity(PASS_TOX)
                .withRemediationToxicity(PASS_TOX)
                .withEvidenceHallucination(PASS_HALL)
                .withTriageHallucination(PASS_HALL)
                .withSummaryHallucination(PASS_HALL);

            assertThat(results.allPassed()).isTrue();
            assertThat(results.failureCount()).isEqualTo(0);
        }
    }
}
