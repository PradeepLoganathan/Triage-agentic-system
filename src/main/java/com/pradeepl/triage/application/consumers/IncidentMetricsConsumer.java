package com.pradeepl.triage.application.consumers;

import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.pradeepl.triage.application.AgentUtils;
import com.pradeepl.triage.application.IncidentMetrics;
import com.pradeepl.triage.application.IncidentRegistry;
import com.pradeepl.triage.application.TriageWorkflow;
import com.pradeepl.triage.domain.TriageState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * IncidentMetricsConsumer updates the incident metrics store as workflows progress.
 *
 * Listens to TriageWorkflow state changes and maintains a queryable
 * incident dashboard.
 */
@Consume.FromWorkflow(TriageWorkflow.class)
@akka.javasdk.annotations.Component(id="incident-metrics-consumer")
public class IncidentMetricsConsumer extends Consumer {

    private static final Logger logger = LoggerFactory.getLogger(IncidentMetricsConsumer.class);
    private static final String METRICS_ENTITY_ID = "global-metrics";

    private final ComponentClient componentClient;

    public IncidentMetricsConsumer(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    public Effect onStateChanged(TriageState state) {
        if (state == null) {
            return effects().done();
        }

        // Get workflow ID from metadata
        String workflowId = messageContext().metadata().asCloudEvent().subject().orElse("unknown");
        
        logger.debug("Updating incident metrics for: {}", workflowId);

        // Extract incident details using shared AgentUtils for consistency
        String service = AgentUtils.extractServiceFromClassification(state.classificationJson());
        String severity = AgentUtils.extractSeverity(state.classificationJson());
        double confidence = AgentUtils.extractConfidenceScore(state.classificationJson(), "overall");
        if (confidence == 0.0) confidence = 0.85; // Default if not extractable
        boolean escalation = AgentUtils.requiresImmediateEscalation(state.classificationJson(), state.evidenceLogs());
        int progress = calculateProgress(state.status());
        String title = extractTitle(state.incident());
        String team = determineTeam(service, severity);
        boolean isActive = state.status() != TriageState.Status.COMPLETED;

        var incidentRecord = new IncidentMetrics.IncidentRecord(
            workflowId,
            state.status().name(),
            service,
            severity,
            title,
            Instant.now(),
            Instant.now(),
            confidence,
            escalation,
            progress,
            team,
            isActive
        );

        // Update individual metrics entity (keep for backward compatibility)
        componentClient
            .forKeyValueEntity(workflowId)
            .method(IncidentMetrics::updateIncident)
            .invoke(incidentRecord);

        // Update central incident registry (for dashboard)
        componentClient
            .forKeyValueEntity("global")
            .method(IncidentRegistry::updateIncident)
            .invoke(incidentRecord);

        logger.info("Updated metrics: incident={}, service={}, severity={}, progress={}/7",
                   workflowId, service, severity, progress);

        return effects().done();
    }


    private int calculateProgress(TriageState.Status status) {
        return switch (status) {
            case INITIATED -> 0;
            case PREPARED -> 1;
            case CLASSIFIED -> 2;
            case EVIDENCE_COLLECTED -> 3;
            case TRIAGED -> 4;
            case KNOWLEDGE_BASE_SEARCHED -> 5;
            case REMEDIATION_PROPOSED -> 6;
            case SUMMARY_READY, COMPLETED -> 7;
        };
    }

    private String extractTitle(String incident) {
        if (incident == null || incident.isBlank()) {
            return "Unknown incident";
        }
        String title = incident.substring(0, Math.min(100, incident.length()));
        return title.contains("\n") ? title.split("\n")[0] : title;
    }

    private String determineTeam(String service, String severity) {
        if ("P1".equals(severity)) {
            return "SRE-ONCALL";
        }
        return switch (service) {
            case "payment-service" -> "PAYMENTS-TEAM";
            case "auth-service" -> "SECURITY-TEAM";
            case "database-service" -> "DATABASE-TEAM";
            default -> "PLATFORM-TEAM";
        };
    }
}
