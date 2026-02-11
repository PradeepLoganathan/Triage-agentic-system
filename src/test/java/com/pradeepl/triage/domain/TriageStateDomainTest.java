package com.pradeepl.triage.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriageStateDomainTest {

    @Nested
    class ConversationTests {
        @Test
        void createsConversation() {
            var conv = new Conversation("user", "test message");
            assertThat(conv.role()).isEqualTo("user");
            assertThat(conv.content()).isEqualTo("test message");
        }
    }

    @Nested
    class TriageStateBuilderTests {
        @Test
        void buildsEmptyState() {
            var state = TriageState.empty();
            assertThat(state.status()).isEqualTo(TriageState.Status.INITIATED);
            assertThat(state.context()).isEmpty();
        }

        @Test
        void buildsStateWithAllFields() {
            var state = TriageState.builder()
                .workflowId("wf-123")
                .status(TriageState.Status.COMPLETED)
                .incident("test incident")
                .classificationJson("{\"service\":\"test\"}")
                .evidenceLogs("logs")
                .evidenceMetrics("metrics")
                .triageText("triage")
                .remediationText("remediation")
                .summaryText("summary")
                .knowledgeBaseResult("kb result")
                .build();

            assertThat(state.workflowId()).isEqualTo("wf-123");
            assertThat(state.status()).isEqualTo(TriageState.Status.COMPLETED);
            assertThat(state.incident()).isEqualTo("test incident");
        }

        @Test
        void toBuilderPreservesState() {
            var original = TriageState.builder()
                .workflowId("wf-456")
                .incident("original")
                .build();

            var modified = original.toBuilder()
                .incident("modified")
                .build();

            assertThat(modified.workflowId()).isEqualTo("wf-456");
            assertThat(modified.incident()).isEqualTo("modified");
        }
    }

    @Nested
    class TriageStateConversationTests {
        @Test
        void addsConversation() {
            var state = TriageState.empty();
            var updated = state.addConversation(new Conversation("user", "hello"));

            assertThat(updated.context()).hasSize(1);
            assertThat(updated.context().get(0).content()).isEqualTo("hello");
        }

        @Test
        void addsMultipleConversations() {
            var state = TriageState.empty()
                .addConversation(new Conversation("user", "first"))
                .addConversation(new Conversation("assistant", "second"));

            assertThat(state.context()).hasSize(2);
            assertThat(state.context().get(1).role()).isEqualTo("assistant");
        }

        @Test
        void preservesExistingConversations() {
            var state = TriageState.builder()
                .context(new ArrayList<>(List.of(new Conversation("system", "init"))))
                .build();

            var updated = state.addConversation(new Conversation("user", "new"));

            assertThat(updated.context()).hasSize(2);
            assertThat(updated.context().get(0).content()).isEqualTo("init");
        }
    }

    @Nested
    class TriageStateStatusTests {
        @Test
        void updatesStatus() {
            var state = TriageState.empty();
            var updated = state.withStatus(TriageState.Status.CLASSIFIED);

            assertThat(updated.status()).isEqualTo(TriageState.Status.CLASSIFIED);
        }

        @Test
        void statusProgression() {
            var state = TriageState.empty()
                .withStatus(TriageState.Status.PREPARED)
                .withStatus(TriageState.Status.CLASSIFIED)
                .withStatus(TriageState.Status.EVIDENCE_COLLECTED);

            assertThat(state.status()).isEqualTo(TriageState.Status.EVIDENCE_COLLECTED);
        }
    }

    @Nested
    class TriageStateFieldUpdateTests {
        @Test
        void updatesIncident() {
            var state = TriageState.empty().withIncident("service down");
            assertThat(state.incident()).isEqualTo("service down");
        }

        @Test
        void updatesClassification() {
            var state = TriageState.empty().withClassificationJson("{\"severity\":\"P1\"}");
            assertThat(state.classificationJson()).contains("P1");
        }

        @Test
        void updatesEvidence() {
            var state = TriageState.empty().withEvidence("log data", "metric data");
            assertThat(state.evidenceLogs()).isEqualTo("log data");
            assertThat(state.evidenceMetrics()).isEqualTo("metric data");
        }

        @Test
        void updatesTriage() {
            var state = TriageState.empty().withTriageText("analysis");
            assertThat(state.triageText()).isEqualTo("analysis");
        }

        @Test
        void updatesRemediation() {
            var state = TriageState.empty().withRemediationText("fix steps");
            assertThat(state.remediationText()).isEqualTo("fix steps");
        }

        @Test
        void updatesSummary() {
            var state = TriageState.empty().withSummaryText("summary");
            assertThat(state.summaryText()).isEqualTo("summary");
        }

        @Test
        void updatesKnowledgeBase() {
            var state = TriageState.empty().withKnowledgeBaseResult("kb data");
            assertThat(state.knowledgeBaseResult()).isEqualTo("kb data");
        }

        @Test
        void updatesEvaluationResults() {
            var results = EvaluationResults.empty();
            var state = TriageState.empty().withEvaluationResults(results);
            assertThat(state.evaluationResults()).isEqualTo(results);
        }
    }

    @Nested
    class EvaluationResultsTests {
        @Test
        void createsEmptyResults() {
            var results = EvaluationResults.empty();
            assertThat(results.summaryToxicity()).isNull();
            assertThat(results.allPassed()).isTrue();
            assertThat(results.failureCount()).isZero();
        }

        @Test
        void allPassedWithAllNull() {
            var results = new EvaluationResults(null, null, null, null, null);
            assertThat(results.allPassed()).isTrue();
        }

        @Test
        void allPassedWithAllPassing() {
            var results = new EvaluationResults(
                new EvaluationResults.ToxicityResult("ok", true),
                new EvaluationResults.ToxicityResult("ok", true),
                new EvaluationResults.HallucinationResult("ok", true),
                new EvaluationResults.HallucinationResult("ok", true),
                new EvaluationResults.HallucinationResult("ok", true)
            );
            assertThat(results.allPassed()).isTrue();
        }

        @Test
        void allPassedFailsWithOneFailing() {
            var results = new EvaluationResults(
                new EvaluationResults.ToxicityResult("failed", false),
                null, null, null, null
            );
            assertThat(results.allPassed()).isFalse();
        }

        @Test
        void countsFailures() {
            var results = new EvaluationResults(
                new EvaluationResults.ToxicityResult("fail", false),
                new EvaluationResults.ToxicityResult("fail", false),
                new EvaluationResults.HallucinationResult("ok", true),
                null, null
            );
            assertThat(results.failureCount()).isEqualTo(2);
        }

        @Test
        void countsAllFailures() {
            var results = new EvaluationResults(
                new EvaluationResults.ToxicityResult("fail", false),
                new EvaluationResults.ToxicityResult("fail", false),
                new EvaluationResults.HallucinationResult("fail", false),
                new EvaluationResults.HallucinationResult("fail", false),
                new EvaluationResults.HallucinationResult("fail", false)
            );
            assertThat(results.failureCount()).isEqualTo(5);
        }

        @Test
        void updatesSummaryToxicity() {
            var results = EvaluationResults.empty();
            var updated = results.withSummaryToxicity(
                new EvaluationResults.ToxicityResult("test", true)
            );
            assertThat(updated.summaryToxicity()).isNotNull();
            assertThat(updated.summaryToxicity().passed()).isTrue();
        }

        @Test
        void updatesRemediationToxicity() {
            var results = EvaluationResults.empty();
            var updated = results.withRemediationToxicity(
                new EvaluationResults.ToxicityResult("test", false)
            );
            assertThat(updated.remediationToxicity()).isNotNull();
            assertThat(updated.remediationToxicity().passed()).isFalse();
        }

        @Test
        void updatesEvidenceHallucination() {
            var results = EvaluationResults.empty();
            var updated = results.withEvidenceHallucination(
                new EvaluationResults.HallucinationResult("test", true)
            );
            assertThat(updated.evidenceHallucination()).isNotNull();
        }

        @Test
        void updatesTriageHallucination() {
            var results = EvaluationResults.empty();
            var updated = results.withTriageHallucination(
                new EvaluationResults.HallucinationResult("test", true)
            );
            assertThat(updated.triageHallucination()).isNotNull();
        }

        @Test
        void updatesSummaryHallucination() {
            var results = EvaluationResults.empty();
            var updated = results.withSummaryHallucination(
                new EvaluationResults.HallucinationResult("test", false)
            );
            assertThat(updated.summaryHallucination()).isNotNull();
            assertThat(updated.summaryHallucination().passed()).isFalse();
        }

        @Test
        void chainsMultipleUpdates() {
            var results = EvaluationResults.empty()
                .withSummaryToxicity(new EvaluationResults.ToxicityResult("ok", true))
                .withRemediationToxicity(new EvaluationResults.ToxicityResult("ok", true))
                .withEvidenceHallucination(new EvaluationResults.HallucinationResult("ok", true));

            assertThat(results.summaryToxicity()).isNotNull();
            assertThat(results.remediationToxicity()).isNotNull();
            assertThat(results.evidenceHallucination()).isNotNull();
            assertThat(results.allPassed()).isTrue();
        }
    }
}
