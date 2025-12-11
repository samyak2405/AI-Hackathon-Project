package com.loganalyser.service;

import com.loganalyser.dto.PromptRule;
import com.loganalyser.enums.PromptCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of PromptRuleService.
 * Manages prompt rules for different categories and builds personalized prompts.
 *
 * @author Himanshu Sehgal
 * @since 2025-12-08
 */
@Service
public class PromptRuleServiceImpl implements PromptRuleService {

    private static final Logger logger = LoggerFactory.getLogger(PromptRuleServiceImpl.class);

    private final Map<PromptCategory, PromptRule> promptRules = new HashMap<>();

    /**
     * Constructor that initializes default prompt rules for all categories.
     */
    public PromptRuleServiceImpl() {
        initializeDefaultRules();
    }

    /**
     * Initializes default prompt rules for all categories.
     */
    private void initializeDefaultRules() {
        // DEVELOPER_RCA - SRE/Backend Engineer persona
        String developerRCATemplate = """
                ROLE & GOAL:
                You are a senior Site Reliability Engineer (SRE) + Backend Engineer with deep expertise in distributed systems, Java/Spring Boot services, Kafka, databases, and payment/transaction systems.

                Your job is to:
                • Analyse the provided application logs and related context.
                • Identify the most likely root cause of the issue.
                • Explain the impact, timeline, and contributing factors.
                • Suggest concrete, actionable fixes that engineering and other teams can use immediately.
                • If the data is insufficient, clearly say so and list what additional info is needed.

                INPUT YOU WILL RECEIVE:
                • Free-form description of the problem and context.
                • Raw logs (application logs, infra logs, DB errors, stack traces, etc.).
                • Sometimes specific transaction IDs, user IDs, correlation IDs, or time ranges.

                HOW TO THINK & RESPOND:
                1. Read the logs carefully. Look for:
                • Error messages, stack traces, exception types.
                • Timestamps and ordering of events.
                • Correlation IDs / request IDs / transaction IDs.
                • Service names, hostnames, pod names, or components.
                • Timeouts, retries, circuit breaker logs, and HTTP status codes.

                2. Reconstruct a timeline of what happened leading up to the failure.

                3. Identify the most probable root cause, not just the symptom.
                For example: "DB connection pool exhausted due to slow queries" is better than "DB error".

                4. Be explicit about uncertainty.
                • If multiple root causes are possible, rank them with probabilities.
                • Do not invent log lines or facts that are not present.

                5. Map technical cause → business impact.
                • What user actions or business flows are impacted?
                • Example: "Load wallet transactions are failing for all users on shard-3."

                6. Always propose fixes and next steps.
                • Short-term mitigations (rollback, feature flag, restart, config change, etc.).
                • Long-term fixes (code changes, design changes, capacity changes, better alerts, etc.).
                • Hand-off guidance for other teams (DBA, DevOps, Network, etc.).

                OUTPUT FORMAT (VERY IMPORTANT):
                Always respond in the following structured format:

                1. High-Level Summary (1–3 lines)
                • Clear, non-ambiguous explanation of what went wrong.

                2. Impact
                • Who/what was impacted?
                • Example: number/percentage of failed requests, affected services or endpoints.

                3. Timeline (based on logs)
                • HH:MM:SS – Key event 1
                • HH:MM:SS – Key event 2
                • HH:MM:SS – Error / failure point
                • HH:MM:SS – Any retries or recovery actions

                4. Evidence from Logs
                • Quote key log lines (or paraphrase) that support your conclusion.
                • Include timestamps, error codes, exception names, and correlation/transaction IDs.
                • Example:
                • 12:05:31 – ERROR [load-service] TxnId=TXN123: DBTimeoutException: query exceeded 3000ms

                5. Root Cause Analysis
                • Primary Root Cause:
                • Clear explanation in 2–5 bullet points.
                • Contributing Factors (if any):
                • E.g., high traffic, misconfiguration, recent deployment, dependency failure, etc.

                6. Short-Term Mitigation (Do Now)
                • Concrete, step-by-step actions the on-call or engineer can take immediately.
                Examples:
                • "Increase DB connection pool size from X to Y temporarily."
                • "Rollback deployment version 1.4.3 → 1.4.2."
                • "Disable feature flag load_v2."

                7. Long-Term Fix / Engineering Action Items
                • Code-level changes (with rough ideas, pseudo-code, or patterns if helpful).
                • Config/infra changes (timeouts, retries, circuit breakers, indexes, capacity planning).
                • Alerting & monitoring improvements.
                • Example format:
                • [Backend] Optimize query for /load by adding index on (user_id, status).
                • [Infra] Add alert when DB latency > 1s for 5 minutes.

                8. Risk & Prevention
                • What risks remain after mitigation?
                • What should be implemented to prevent this class of issue in the future?

                9. If Information is Insufficient
                • Clearly say: "Logs are not sufficient to determine a single root cause."
                • Provide:
                • Top 2–3 possible root causes (ranked).
                • Additional data required, e.g.:
                • DB metrics (CPU, connections, slow queries)
                • Upstream/downstream service logs
                • Recent deployment/change log

                STYLE:
                • Be concise but very clear and structured.
                • Avoid vague phrases like "seems broken" — always tie claims back to log evidence.
                • Use bullet points and headings so SREs, backend engineers, and managers can quickly read and act.

                ---

                User Query: {QUERY}

                Relevant Logs:
                {LOGS}

                Please provide your analysis following the format above.
                """;

        promptRules.put(PromptCategory.DEVELOPER_RCA, new PromptRule(
                PromptCategory.DEVELOPER_RCA,
                "Senior Site Reliability Engineer (SRE) + Backend Engineer",
                "Analyse logs to identify root causes, explain impact, and suggest actionable fixes",
                developerRCATemplate
        ));

        // GENERAL - Basic log analysis
        String generalTemplate = """
                You are a log analysis assistant. Analyze the following logs and answer the user's question.

                User Query: {QUERY}

                Relevant Logs:
                {LOGS}

                Please provide a detailed analysis and answer to the user's question based on the logs above.
                """;

        promptRules.put(PromptCategory.GENERAL, new PromptRule(
                PromptCategory.GENERAL,
                "Log Analysis Assistant",
                "Analyze logs and answer user questions",
                generalTemplate
        ));

        // PERFORMANCE_ANALYSIS
        String performanceTemplate = """
                You are a performance analysis expert specializing in identifying bottlenecks, slow queries, 
                resource constraints, and optimization opportunities in distributed systems.

                Your goal is to:
                • Identify performance bottlenecks in the logs
                • Analyze response times, throughput, and resource usage
                • Suggest optimization strategies
                • Highlight slow queries, timeouts, or resource exhaustion

                User Query: {QUERY}

                Relevant Logs:
                {LOGS}

                Provide a detailed performance analysis with specific recommendations.
                """;

        promptRules.put(PromptCategory.PERFORMANCE_ANALYSIS, new PromptRule(
                PromptCategory.PERFORMANCE_ANALYSIS,
                "Performance Analysis Expert",
                "Identify performance bottlenecks and optimization opportunities",
                performanceTemplate
        ));

        // SECURITY_ANALYSIS
        String securityTemplate = """
                You are a security analyst specializing in identifying security vulnerabilities, 
                unauthorized access attempts, suspicious patterns, and security incidents in application logs.

                Your goal is to:
                • Identify security threats and vulnerabilities
                • Detect unauthorized access or suspicious activities
                • Analyze authentication and authorization failures
                • Suggest security remediation steps

                User Query: {QUERY}

                Relevant Logs:
                {LOGS}

                Provide a detailed security analysis with risk assessment and remediation steps.
                """;

        promptRules.put(PromptCategory.SECURITY_ANALYSIS, new PromptRule(
                PromptCategory.SECURITY_ANALYSIS,
                "Security Analyst",
                "Identify security threats and vulnerabilities in logs",
                securityTemplate
        ));

        // BUSINESS_IMPACT
        String businessImpactTemplate = """
                You are a business impact analyst specializing in translating technical issues 
                into business metrics and user impact.

                Your goal is to:
                • Translate technical errors into business impact
                • Identify affected user segments and features
                • Quantify the impact (users, transactions, revenue)
                • Prioritize issues based on business criticality

                User Query: {QUERY}

                Relevant Logs:
                {LOGS}

                Provide a detailed business impact analysis with quantified metrics and prioritization.
                """;

        promptRules.put(PromptCategory.BUSINESS_IMPACT, new PromptRule(
                PromptCategory.BUSINESS_IMPACT,
                "Business Impact Analyst",
                "Translate technical issues into business metrics and user impact",
                businessImpactTemplate
        ));

        logger.info("Initialized {} prompt rules for different categories", promptRules.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PromptRule getPromptRule(PromptCategory category) {
        PromptRule rule = promptRules.get(category);
        if (rule == null) {
            logger.warn("No prompt rule found for category: {}, falling back to GENERAL", category);
            return promptRules.get(PromptCategory.GENERAL);
        }
        return rule;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String buildPrompt(PromptCategory category, String query, List<String> logs) {
        PromptRule rule = getPromptRule(category);
        String template = rule.template();

        // Replace placeholders
        String logsText;
        if (logs == null || logs.isEmpty()) {
            logsText = "No logs found matching the transaction ID.\n";
        } else {
            StringBuilder logsBuilder = new StringBuilder();
            for (String log : logs) {
                logsBuilder.append(log).append("\n");
            }
            logsText = logsBuilder.toString();
        }

        return template.replace("{QUERY}", query != null ? query : "")
                      .replace("{LOGS}", logsText);
    }

    /**
     * Builds the system message (role/persona) for a given category.
     * This separates the role definition from the user query, similar to the pattern:
     * role: "developer" / role: "user"
     *
     * @param category The prompt category
     * @return The system message containing role and goal
     */
    public String buildSystemMessage(PromptCategory category) {
        PromptRule rule = getPromptRule(category);
        
        // For DEVELOPER_RCA, extract the full ROLE & GOAL section
        if (category == PromptCategory.DEVELOPER_RCA) {
            String template = rule.template();
            // Extract everything from "ROLE & GOAL:" to just before "User Query:"
            int roleStart = template.indexOf("ROLE & GOAL:");
            int userQueryStart = template.indexOf("User Query:");
            
            if (roleStart >= 0 && userQueryStart > roleStart) {
                return template.substring(roleStart, userQueryStart).trim();
            }
        }
        
        // For other categories, create a simple role/goal message
        return String.format("ROLE: %s\nGOAL: %s", rule.role(), rule.goal());
    }

    /**
     * Builds the user message (query + logs) for a given category.
     *
     * @param category The prompt category
     * @param query The user query
     * @param logs The log lines
     * @return The user message
     */
    public String buildUserMessage(PromptCategory category, String query, List<String> logs) {
        PromptRule rule = getPromptRule(category);
        String template = rule.template();

        // Extract the user content part (after ROLE & GOAL section if present)
        String userContent = template;
        
        // If template contains ROLE & GOAL section, extract only the user-facing part
        if (template.contains("ROLE & GOAL:") || template.contains("ROLE:")) {
            // Find where the actual user content starts
            int userStart = template.indexOf("User Query:");
            if (userStart < 0) {
                userStart = template.indexOf("User query:");
            }
            if (userStart < 0) {
                userStart = template.indexOf("{QUERY}");
            }
            
            if (userStart > 0) {
                userContent = template.substring(userStart);
            }
        }

        // Replace placeholders
        String logsText;
        if (logs == null || logs.isEmpty()) {
            logsText = "No logs found matching the transaction ID.\n";
        } else {
            StringBuilder logsBuilder = new StringBuilder();
            for (String log : logs) {
                logsBuilder.append(log).append("\n");
            }
            logsText = logsBuilder.toString();
        }

        return userContent.replace("{QUERY}", query != null ? query : "")
                         .replace("{LOGS}", logsText);
    }
}