package com.pradeepl.triage.guardrails;

import akka.javasdk.agent.GuardrailContext;
import akka.javasdk.agent.TextGuardrail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * PromptInjectionGuardrail detects and blocks prompt injection attacks
 * in agent inputs.
 *
 * Detects common prompt injection patterns like:
 * - "Ignore previous instructions"
 * - "You are now..."
 * - System prompt overrides
 */
public class PromptInjectionGuardrail implements TextGuardrail {

    private static final Logger logger = LoggerFactory.getLogger(PromptInjectionGuardrail.class);

    private static final Pattern SYSTEM_OVERRIDE_PATTERN =
        Pattern.compile("(?m)^\\s*system:\\s*.*");

    private static final List<String> INJECTION_PATTERNS = List.of(
        "ignore previous instructions",
        "ignore all previous",
        "disregard previous",
        "forget previous",
        "you are now",
        "new instructions:",
        "override instructions",
        "act as if",
        "pretend you are"
    );

    private final GuardrailContext context;

    public PromptInjectionGuardrail(GuardrailContext context) {
        this.context = context;
        logger.info("PromptInjectionGuardrail initialized: {}", context.name());
    }

    @Override
    public Result evaluate(String text) {
        if (text == null || text.isEmpty()) {
            return Result.OK;
        }

        String lowerText = text.toLowerCase();

        for (String pattern : INJECTION_PATTERNS) {
            if (lowerText.contains(pattern)) {
                logger.warn("Prompt injection detected: pattern '{}' found", pattern);
                return new Result(false, "Potential prompt injection detected: " + pattern);
            }
        }

        // Check for system: only at start of line (more specific)
        if (SYSTEM_OVERRIDE_PATTERN.matcher(lowerText).find()) {
            logger.warn("Prompt injection detected: system override attempt");
            return new Result(false, "Potential prompt injection detected: system override");
        }

        return Result.OK;
    }
}
