package com.pradeepl.triage;

import com.pradeepl.triage.domain.Conversation;
import com.pradeepl.triage.domain.EvaluationResults;
import com.pradeepl.triage.domain.TriageState;
import com.pradeepl.triage.domain.TriageState.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriageStateTest {

    @Nested
    class EmptyState {

        @Test
        void hasInitiatedStatus() {
            TriageState state = TriageState.empty();
            assertThat(state.status()).isEqualTo(Status.INITIATED);
        }

        @Test
        void hasNullFields() {
            TriageState state = TriageState.empty();
            assertThat(state.workflowId()).isNull();
            assertThat(state.incident()).isNull();
            assertThat(state.classificationJson()).isNull();
            assertThat(state.evidenceLogs()).isNull();
            assertThat(state.evidenceMetrics()).isNull();
            assertThat(state.triageText()).isNull();
            assertThat(state.remediationText()).isNull();
            assertThat(state.summaryText()).isNull();
            assertThat(state.knowledgeBaseResult()).isNull();
        }

        @Test
        void hasEmptyContext() {
            TriageState state = TriageState.empty();
            assertThat(state.context()).isEmpty();
        }

        @Test
        void hasEmptyEvaluationResults() {
            TriageState state = TriageState.empty();
            assertThat(state.evaluationResults()).isNotNull();
            assertThat(state.evaluationResults().allPassed()).isTrue();
        }
    }

    @Nested
    class BuilderChain {

        @Test
        void buildsCompleteState() {
            TriageState state = TriageState.builder()
                .workflowId("wf-123")
                .status(Status.COMPLETED)
                .incident("Server down")
                .classificationJson("{\"severity\":\"P1\"}")
                .evidenceLogs("log data")
                .evidenceMetrics("metric data")
                .triageText("triage analysis")
                .remediationText("restart the service")
                .summaryText("incident summary")
                .knowledgeBaseResult("runbook info")
                .context(List.of(new Conversation("user", "help")))
                .build();

            assertThat(state.workflowId()).isEqualTo("wf-123");
            assertThat(state.status()).isEqualTo(Status.COMPLETED);
            assertThat(state.incident()).isEqualTo("Server down");
            assertThat(state.classificationJson()).isEqualTo("{\"severity\":\"P1\"}");
            assertThat(state.evidenceLogs()).isEqualTo("log data");
            assertThat(state.evidenceMetrics()).isEqualTo("metric data");
            assertThat(state.triageText()).isEqualTo("triage analysis");
            assertThat(state.remediationText()).isEqualTo("restart the service");
            assertThat(state.summaryText()).isEqualTo("incident summary");
            assertThat(state.knowledgeBaseResult()).isEqualTo("runbook info");
            assertThat(state.context()).hasSize(1);
        }

        @Test
        void defaultsEvaluationResultsWhenNull() {
            TriageState state = TriageState.builder()
                .status(Status.INITIATED)
                .build();
            assertThat(state.evaluationResults()).isNotNull();
            assertThat(state.evaluationResults()).isEqualTo(EvaluationResults.empty());
        }
    }

    @Nested
    class AddConversation {

        @Test
        void appendsToExistingContext() {
            TriageState state = TriageState.empty()
                .addConversation(new Conversation("user", "incident report"))
                .addConversation(new Conversation("assistant", "analyzing..."));

            assertThat(state.context()).hasSize(2);
            assertThat(state.context().get(0).role()).isEqualTo("user");
            assertThat(state.context().get(1).role()).isEqualTo("assistant");
        }

        @Test
        void preservesOtherFields() {
            TriageState state = TriageState.empty()
                .withIncident("Server crash")
                .addConversation(new Conversation("user", "help"));

            assertThat(state.incident()).isEqualTo("Server crash");
            assertThat(state.context()).hasSize(1);
        }
    }

    @Nested
    class WithMethods {

        @Test
        void withStatusReturnsNewInstance() {
            TriageState original = TriageState.empty();
            TriageState updated = original.withStatus(Status.CLASSIFIED);

            assertThat(updated.status()).isEqualTo(Status.CLASSIFIED);
            assertThat(original.status()).isEqualTo(Status.INITIATED);
        }

        @Test
        void withEvidenceSetsLogsAndMetrics() {
            TriageState state = TriageState.empty()
                .withEvidence("log data", "metric data");

            assertThat(state.evidenceLogs()).isEqualTo("log data");
            assertThat(state.evidenceMetrics()).isEqualTo("metric data");
        }
    }

    @Nested
    class ToBuilder {

        @Test
        void createsModifiableCopy() {
            TriageState original = TriageState.builder()
                .workflowId("wf-1")
                .status(Status.TRIAGED)
                .incident("original incident")
                .build();

            TriageState copy = original.toBuilder()
                .incident("modified incident")
                .build();

            assertThat(copy.workflowId()).isEqualTo("wf-1");
            assertThat(copy.status()).isEqualTo(Status.TRIAGED);
            assertThat(copy.incident()).isEqualTo("modified incident");
            assertThat(original.incident()).isEqualTo("original incident");
        }
    }

    @Nested
    class StatusEnum {

        @Test
        void hasAllExpectedValues() {
            assertThat(Status.values()).containsExactly(
                Status.INITIATED,
                Status.PREPARED,
                Status.CLASSIFIED,
                Status.EVIDENCE_COLLECTED,
                Status.TRIAGED,
                Status.KNOWLEDGE_BASE_SEARCHED,
                Status.REMEDIATION_PROPOSED,
                Status.SUMMARY_READY,
                Status.COMPLETED
            );
        }

        @Test
        void hasNineValues() {
            assertThat(Status.values()).hasSize(9);
        }
    }
}
