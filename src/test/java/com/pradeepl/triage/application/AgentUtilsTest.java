package com.pradeepl.triage.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentUtilsTest {

    @Nested
    class ExtractServiceFromClassification {
        @Test
        void extractsFromNestedClassificationObject() {
            String json = """
                {"classification": {"service": "payment-service", "severity": "P1"}}
                """;
            assertThat(AgentUtils.extractServiceFromClassification(json))
                .isEqualTo("payment-service");
        }

        @Test
        void extractsFromTopLevelServiceField() {
            String json = """
                {"service": "auth-service", "severity": "P2"}
                """;
            assertThat(AgentUtils.extractServiceFromClassification(json))
                .isEqualTo("auth-service");
        }

        @Test
        void prefersNestedClassificationOverTopLevel() {
            String json = """
                {"classification": {"service": "nested-svc"}, "service": "top-svc"}
                """;
            assertThat(AgentUtils.extractServiceFromClassification(json))
                .isEqualTo("nested-svc");
        }

        @Test
        void fallsBackToRegexForInvalidJson() {
            String text = "The affected \"service\": \"order-service\" is down";
            assertThat(AgentUtils.extractServiceFromClassification(text))
                .isEqualTo("order-service");
        }

        @Test
        void returnsUnknownForNull() {
            assertThat(AgentUtils.extractServiceFromClassification(null))
                .isEqualTo("unknown");
        }

        @Test
        void returnsUnknownForEmpty() {
            assertThat(AgentUtils.extractServiceFromClassification(""))
                .isEqualTo("unknown");
        }

        @Test
        void returnsUnknownForJsonWithoutService() {
            String json = """
                {"classification": {"category": "infrastructure"}}
                """;
            assertThat(AgentUtils.extractServiceFromClassification(json))
                .isEqualTo("unknown");
        }
    }

    @Nested
    class ExtractSeverity {
        @Test
        void extractsFromNestedClassification() {
            String json = """
                {"classification": {"severity": "P1"}}
                """;
            assertThat(AgentUtils.extractSeverity(json)).isEqualTo("P1");
        }

        @Test
        void extractsFromTopLevel() {
            String json = """
                {"severity": "P2"}
                """;
            assertThat(AgentUtils.extractSeverity(json)).isEqualTo("P2");
        }

        @Test
        void fallsBackToRegexForBrokenJson() {
            String text = "severity is \"severity\": \"P4\" here";
            assertThat(AgentUtils.extractSeverity(text)).isEqualTo("P4");
        }

        @Test
        void defaultsToP3ForNull() {
            assertThat(AgentUtils.extractSeverity(null)).isEqualTo("P3");
        }

        @Test
        void defaultsToP3ForJsonWithoutSeverity() {
            String json = """
                {"service": "auth-service"}
                """;
            assertThat(AgentUtils.extractSeverity(json)).isEqualTo("P3");
        }
    }

    @Nested
    class ExtractConfidenceScore {
        @Test
        void extractsFromRootCauseAnalysis() {
            String json = """
                {"root_cause_analysis": {"primary_hypothesis": {"confidence": 0.85}}}
                """;
            assertThat(AgentUtils.extractConfidenceScore(json, "overall"))
                .isEqualTo(0.85);
        }

        @Test
        void extractsFromDirectConfidenceNumber() {
            String json = """
                {"confidence": 0.92}
                """;
            assertThat(AgentUtils.extractConfidenceScore(json, "overall"))
                .isEqualTo(0.92);
        }

        @Test
        void extractsFromConfidenceObjectByScoreType() {
            String json = """
                {"confidence": {"classification": 0.75, "overall": 0.80}}
                """;
            assertThat(AgentUtils.extractConfidenceScore(json, "classification"))
                .isEqualTo(0.75);
        }

        @Test
        void fallsBackToConfidenceOverall() {
            String json = """
                {"confidence": {"overall": 0.88}}
                """;
            assertThat(AgentUtils.extractConfidenceScore(json, "missing_type"))
                .isEqualTo(0.88);
        }

        @Test
        void extractsFromConfidenceAssessment() {
            String json = """
                {"confidence_assessment": {"triage": 0.65}}
                """;
            assertThat(AgentUtils.extractConfidenceScore(json, "triage"))
                .isEqualTo(0.65);
        }

        @Test
        void returnsZeroForNull() {
            assertThat(AgentUtils.extractConfidenceScore(null, "overall"))
                .isEqualTo(0.0);
        }

        @Test
        void returnsZeroForJsonWithoutConfidence() {
            String json = """
                {"service": "auth-service"}
                """;
            assertThat(AgentUtils.extractConfidenceScore(json, "overall"))
                .isEqualTo(0.0);
        }
    }

    @Nested
    class RequiresImmediateEscalation {
        @Test
        void p1AlwaysEscalates() {
            String json = """
                {"severity": "P1"}
                """;
            assertThat(AgentUtils.requiresImmediateEscalation(json, null)).isTrue();
        }

        @Test
        void securityKeywordEscalates() {
            String json = """
                {"severity": "P3"}
                """;
            assertThat(AgentUtils.requiresImmediateEscalation(json, "security breach detected"))
                .isTrue();
        }

        @Test
        void dataLossKeywordEscalates() {
            assertThat(AgentUtils.requiresImmediateEscalation(
                "{\"severity\": \"P4\"}", "potential data loss in database"))
                .isTrue();
        }

        @Test
        void paymentKeywordEscalates() {
            assertThat(AgentUtils.requiresImmediateEscalation(
                "{\"severity\": \"P4\"}", "payment processing failure"))
                .isTrue();
        }

        @Test
        void lowSeverityNoKeywordsDoesNotEscalate() {
            String json = """
                {"severity": "P4"}
                """;
            assertThat(AgentUtils.requiresImmediateEscalation(json, "disk usage at 70%"))
                .isFalse();
        }

        @Test
        void p3WithNullEvidenceDoesNotEscalate() {
            assertThat(AgentUtils.requiresImmediateEscalation(
                "{\"severity\": \"P3\"}", null))
                .isFalse();
        }
    }

    @Nested
    class ExtractLogsAndMetrics {
        @Test
        void extractsFromEvidenceSummary() {
            String json = """
                {"evidence_summary": {"logs": {"error_count": 5}, "metrics": {"cpu": 90}}}
                """;
            String[] result = AgentUtils.extractLogsAndMetrics(json);
            assertThat(result[0]).contains("error_count");
            assertThat(result[1]).contains("cpu");
        }

        @Test
        void extractsFromTopLevel() {
            String json = """
                {"logs": "error log text", "metrics": "cpu=90%"}
                """;
            String[] result = AgentUtils.extractLogsAndMetrics(json);
            assertThat(result[0]).isEqualTo("error log text");
            assertThat(result[1]).isEqualTo("cpu=90%");
        }

        @Test
        void returnsNullsForNonJson() {
            String[] result = AgentUtils.extractLogsAndMetrics("plain text response");
            assertThat(result[0]).isNull();
            assertThat(result[1]).isNull();
        }

        @Test
        void returnsNullsForNull() {
            String[] result = AgentUtils.extractLogsAndMetrics(null);
            assertThat(result[0]).isNull();
            assertThat(result[1]).isNull();
        }

        @Test
        void returnsNullsForBlank() {
            String[] result = AgentUtils.extractLogsAndMetrics("  ");
            assertThat(result[0]).isNull();
            assertThat(result[1]).isNull();
        }

        @Test
        void prefersEvidenceSummaryOverTopLevel() {
            String json = """
                {"evidence_summary": {"logs": "nested"}, "logs": "top-level"}
                """;
            String[] result = AgentUtils.extractLogsAndMetrics(json);
            assertThat(result[0]).isEqualTo("nested");
        }
    }

    @Nested
    class ExtractKeyFindings {
        @Test
        void extractsFromAnalysisKeyFindings() {
            String json = """
                {"analysis": {"key_findings": ["finding1", "finding2", "finding3"]}}
                """;
            List<String> findings = AgentUtils.extractKeyFindings(json);
            assertThat(findings).hasSize(3);
            assertThat(findings).contains("finding1", "finding2", "finding3");
        }

        @Test
        void returnsEmptyForNull() {
            List<String> findings = AgentUtils.extractKeyFindings(null);
            assertThat(findings).isEmpty();
        }

        @Test
        void returnsEmptyForJsonWithoutFindings() {
            String json = """
                {"service": "test"}
                """;
            List<String> findings = AgentUtils.extractKeyFindings(json);
            assertThat(findings).isEmpty();
        }
    }

    @Nested
    class IsValidJson {
        @Test
        void validJsonReturnsTrue() {
            assertThat(AgentUtils.isValidJson("{\"key\": \"value\"}")).isTrue();
        }

        @Test
        void validArrayReturnsTrue() {
            assertThat(AgentUtils.isValidJson("[1, 2, 3]")).isTrue();
        }

        @Test
        void invalidJsonReturnsFalse() {
            assertThat(AgentUtils.isValidJson("{invalid}")).isFalse();
        }

        @Test
        void nullReturnsFalse() {
            assertThat(AgentUtils.isValidJson(null)).isFalse();
        }

        @Test
        void emptyReturnsFalse() {
            assertThat(AgentUtils.isValidJson("")).isFalse();
        }

        @Test
        void plainTextReturnsFalse() {
            assertThat(AgentUtils.isValidJson("plain text")).isFalse();
        }
    }

    @Nested
    class FormatTimestamp {
        @Test
        void formatsTimestamp() {
            LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
            String formatted = AgentUtils.formatTimestamp(dt);
            assertThat(formatted).isEqualTo("2024-01-15T14:30:45");
        }
    }

    @Nested
    class CleanResponse {
        @Test
        void removesExcessiveWhitespace() {
            String result = AgentUtils.cleanResponse("text   with    spaces");
            assertThat(result).isEqualTo("text with spaces");
        }

        @Test
        void removesBoldMarkdown() {
            String result = AgentUtils.cleanResponse("**bold** text");
            assertThat(result).isEqualTo("bold text");
        }

        @Test
        void removesItalicMarkdown() {
            String result = AgentUtils.cleanResponse("*italic* text");
            assertThat(result).isEqualTo("italic text");
        }

        @Test
        void trimsWhitespace() {
            String result = AgentUtils.cleanResponse("  text  ");
            assertThat(result).isEqualTo("text");
        }

        @Test
        void handlesNull() {
            String result = AgentUtils.cleanResponse(null);
            assertThat(result).isEmpty();
        }
    }
}
