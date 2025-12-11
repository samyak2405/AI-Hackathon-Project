package com.example.userservice.chat;

import com.example.userservice.user.Role;
import com.example.userservice.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiAgentService {

    private final RestTemplate restTemplate;
    private final ChatService chatService;
    private final ResponseFormatterService responseFormatterService;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String openAiModel;

    @Value("${rca.service-url:http://localhost:8080/api/query/markdown}")
    private String rcaServiceUrl;

    @Value("${data.service-url:http://localhost:8083/api/v1/query}")
    private String dataServiceUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ROUTER_SYSTEM_PROMPT = """
            You are a Routing Decision Model. \s
            Your job is to decide which backend service should process the user’s query.
            
            You must output ONLY one of these exact strings:
            - "RCA_SERVICE"
            - "DB_SERVICE"
            
            Rules:
            1. Choose "RCA_SERVICE" if the user intent is related to logs or application log analysis.\s
               Keywords and intents: “log”, “logs”, “application logs”, “debug logs”,\s
               “trace”, “what do logs say”, “as per logs”, “from application perspective”,\s
               “RCA”, “failure analysis from logs”.
            
            2. Choose "DB_SERVICE" if the user intent is related to the database.
               Keywords and intents: “DB”, “database”, “what does the DB say”,\s
               “state in DB”, “record”, “table”, “transaction as per DB”.
            
            3. Default fallback: If the intent is unclear, ambiguous, or neither category is strongly indicated,
               return "DB_SERVICE".
            
            Output Format:
            Return ONLY the chosen service string with no explanation and no additional text.
            """;
//    private static final String ROUTER_SYSTEM_PROMPT = """
//
//            You are an AI Router for a backend system.
//            Your job: classify the user request and route it to the correct internal service.
//            You NEVER answer the question yourself; you ONLY route.
//
//            STRICT OUTPUT:
//            {
//              "target": "RCA_SERVICE" | "DATA_SERVICE" | "OUT_OF_SCOPE",
//              "reason": "<short explanation>"
//            }
//
//            ======================================================================
//            SERVICES
//            ======================================================================
//
//            1. RCA_SERVICE
//               Handles any request asking for:
//               - root-cause analysis
//               - explanations of failures, errors, exceptions
//               - unexpected behavior, timeouts, outages
//               - debugging or "why something failed"
//               - log analysis or investigation based on LOGS
//
//            2. DATA_SERVICE
//               Handles:
//               - fetch/list transactions
//               - aggregates, metrics, reports
//               - filtering, searching, querying
//               - reading data from DB or storage systems
//
//            3. OUT_OF_SCOPE
//               Anything unrelated to failures or data retrieval.
//
//            ======================================================================
//            DECISION ORDER (ALWAYS APPLY IN THIS ORDER)
//            ======================================================================
//
//            Step 1 – Normalize
//            - Convert the user query to lowercase for keyword checks.
//
//            Step 2 – HARD PRIORITY: logs vs DB
//            - If the query mentions any of:
//                "log", "logs", "log file", "log files"
//              → target = "RCA_SERVICE"
//              → reason: "User mentioned logs → treat as investigation / RCA"
//
//            - ELSE IF the query mentions any of:
//                "db", "database", "from db", "in db", "as per db", "query from db"
//              → target = "DATA_SERVICE"
//              → reason: "User mentioned DB → treat as data retrieval"
//
//            - If both DB and logs are mentioned:
//                - If the user clearly asks for cause / explanation (why, root cause, etc.)
//                  → RCA_SERVICE
//                - Otherwise, if the user clearly asks to fetch data / list / counts
//                  → DATA_SERVICE
//
//            If a target has already been chosen in Step 2, DO NOT override it in later steps.
//
//            Step 3 – ROUTING RULES (only when Step 2 did not decide)
//            ----------------------------------------------------------------
//
//            Choose RCA_SERVICE if the user's intent is:
//            - asking why something failed, errored, or misbehaved
//            - requesting an explanation or investigation
//            - asking for RCA of a past failed transaction
//            - using keywords: "why", "cause", "explain", "reason", "root cause"
//
//            Choose DATA_SERVICE if the user's intent is:
//            - querying data, listing transactions, fetching metrics
//            - requesting analytics or counts
//            - requesting failed transactions *without asking WHY*
//
//            Choose OUT_OF_SCOPE if:
//            - the request is unrelated to failures or data retrieval
//
//            ======================================================================
//            DISAMBIGUATION RULES
//            ======================================================================
//
//            1. Presence of “why / cause / reason / explain” → RCA_SERVICE
//               BUT ONLY IF:
//               - Step 2 did NOT already choose a service based on DB/log keywords.
//
//            2. Mentioning "failed" alone does NOT imply RCA.
//               - “Why did it fail?” → RCA_SERVICE
//               - “Get last 10 failed transactions” → DATA_SERVICE
//
//            3. If unclear AND intent looks like data → DATA_SERVICE.
//
//            4. If unclear but resembles debugging → RCA_SERVICE.
//
//            ======================================================================
//            EXAMPLES WITH PRIORITY
//            ======================================================================
//
//            1. “Fetch last 10 transactions from DB”
//                  → DATA_SERVICE (DB mentioned → Step 2 → data priority)
//
//            2. “Why did the transaction fail? Check logs.”
//                  → RCA_SERVICE (logs mentioned → Step 2 → RCA priority)
//
//            3. “As per DB the last transaction failed, why?”
//                  → DATA_SERVICE
//                    - DB mentioned → Step 2 → DATA_SERVICE
//                    - Even though "why" is present, Step 2 already decided.
//
//            4. “Get details of last failed transaction from DB”
//                  → DATA_SERVICE (DB + no ‘why’ → Step 2 → DATA)
//
//            5. “Look into the logs for the cause of delay”
//                  → RCA_SERVICE (logs mentioned → Step 2 → RCA)
//
//            6. “Show failed count from DB”
//                  → DATA_SERVICE (DB + analytics → Step 2 → DATA)
//
//            ======================================================================
//            END OF PROMPT
//            ======================================================================
//
//            """;

    public String getResponseForPrompt(User user, String prompt, String chatId, Integer requestedLimit) {
        if (isGreeting(prompt)) {
            return greetingMessage();
        }
        String routingPrompt = buildRoutingPrompt(user, prompt, chatId);
        int effectiveLimit = resolveLimit(prompt, requestedLimit);
        Agent agent = decideAgent(routingPrompt);
        log.info("Routing prompt for user {} to agent {}", user.getUsername(), agent);

        try {
            String raw = switch (agent) {
                case RCA_SERVICE -> callRcaService(user, prompt, chatId);
                case DATA_SERVICE -> callDataService(user, prompt, effectiveLimit);
                case OUT_OF_SCOPE -> outOfScopeMessage();
            };
            return responseFormatterService.formatHtml(raw); // return HTML-safe
        } catch (Exception ex) {
            log.error("Failed calling {} for user {}: {}", agent, user.getUsername(), ex.getMessage(), ex);
            // Fallback markdown so frontend can still render something
            return "Sorry, I could not reach the backend agent. Please try again later.";
        }
    }

    private Agent decideAgent(String prompt) {
        try {
            RouterDecision decision = routeWithRouterPrompt(prompt);
            log.info("Router decision target={} reason={}", decision.target(), decision.reason());
            return decision.target();
        } catch (Exception ex) {
            log.warn("Router model failed ({}); falling back to heuristic routing", ex.getMessage());
            return fallbackHeuristic(prompt);
        }
    }

    /**
     * RCA service – call downstream RCA Spring Boot service which returns markdown.
     */
    private String callRcaService(User user, String prompt, String chatId) throws Exception {
        if (rcaServiceUrl == null || rcaServiceUrl.isBlank()) {
            log.warn("RCA service URL is not configured; returning fallback message");
            return "<p>RCA service URL is not configured on the server.</p>";
        }

        Map<String, Object> body = new HashMap<>();
        body.put("query", prompt);
        body.put("category", resolveRcaCategory(user));
        if (chatId != null && !chatId.isBlank()) {
            body.put("chatId", chatId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String raw = restTemplate.postForObject(rcaServiceUrl, entity, String.class);
        return sanitizeServiceResponse(raw, "RCA service");
    }

    /**
     * Data service – call downstream Data Spring Boot service which returns markdown.
     */
    private String callDataService(User user, String prompt, int limit) throws Exception {
        if (dataServiceUrl == null || dataServiceUrl.isBlank()) {
            log.warn("Data service URL is not configured; returning fallback message");
            return "<p>Data service URL is not configured on the server.</p>";
        }

        Map<String, Object> body = Map.of(
                "query", prompt,
                "limit", limit
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String raw = restTemplate.postForObject(dataServiceUrl, entity, String.class);
        return extractDataServiceResponse(raw);
    }

    /**
     * Extracts choices[0].message.content from an OpenAI/Perplexity-style chat completion response.
     */
    private String extractChatMessageContent(String rawJson, String source) throws Exception {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("Empty response from " + source);
        }

        JsonNode root = objectMapper.readTree(rawJson);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("No choices in response from " + source);
        }

        JsonNode message = choices.get(0).path("message");
        String content = message.path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("No message.content in response from " + source);
        }
        return content;
    }

    /**
     * Ensure downstream service responses are non-empty markdown.
     */
    private String sanitizeServiceResponse(String raw, String source) {
        if (raw == null || raw.isBlank()) {
            log.warn("Empty response from {}", source);
            return "No content returned from " + source + ".";
        }
        return raw;
    }

    private String extractDataServiceResponse(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            String formatted = node.path("formatted_output").asText(null);
            if (formatted != null && !formatted.isBlank()) {
                return formatted;
            }
            JsonNode results = node.path("results");
            if (results.isArray() && results.size() > 0) {
                return renderResultsTable(results);
            }
            String sql = node.path("sql_query").asText(null);
            if (sql != null && !sql.isBlank()) {
                return "<p><strong>SQL:</strong></p><pre>" + HtmlUtils.htmlEscape(sql) + "</pre>";
            }
        } catch (Exception ex) {
            log.warn("Could not parse data service response as JSON; returning raw", ex);
        }
        return raw;
    }

    private String renderResultsTable(JsonNode results) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><thead><tr>");
        JsonNode first = results.get(0);
        var fields = first.fieldNames();
        java.util.List<String> headers = new java.util.ArrayList<>();
        while (fields.hasNext()) {
            String f = fields.next();
            headers.add(f);
            sb.append("<th>").append(HtmlUtils.htmlEscape(f)).append("</th>");
        }
        sb.append("</tr></thead><tbody>");
        for (JsonNode row : results) {
            sb.append("<tr>");
            for (String h : headers) {
                JsonNode cell = row.path(h);
                String val = cell.isTextual() ? cell.asText() : cell.isNull() ? "" : cell.toString();
                sb.append("<td>").append(HtmlUtils.htmlEscape(val)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private int resolveLimit(String prompt, Integer requestedLimit) {
        int DEFAULT_LIMIT = 5;
        int MAX_LIMIT = 1000;

        if (requestedLimit != null && requestedLimit > 0) {
            return Math.min(requestedLimit, MAX_LIMIT);
        }
        if (prompt != null && !prompt.isBlank()) {
            String lower = prompt.toLowerCase();
            // Look for patterns like "top 5", "last 10", "limit 20"
            Pattern keyed = Pattern.compile("\\b(top|limit|last|first|recent)\\s+(\\d{1,4})\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = keyed.matcher(lower);
            if (m.find()) {
                int parsed = Integer.parseInt(m.group(2));
                if (parsed > 0) {
                    return Math.min(parsed, MAX_LIMIT);
                }
            }
            // Fallback: first standalone number up to 3 digits to reduce false matches
            Pattern anyNumber = Pattern.compile("\\b(\\d{1,3})\\b");
            Matcher m2 = anyNumber.matcher(lower);
            if (m2.find()) {
                int parsed = Integer.parseInt(m2.group(1));
                if (parsed > 0) {
                    return Math.min(parsed, MAX_LIMIT);
                }
            }
        }
        return DEFAULT_LIMIT;
    }

    /**
     * Call OpenAI with the router prompt to classify the target service.
     */
    private RouterDecision routeWithRouterPrompt(String userPrompt) throws Exception {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key missing for router");
        }

        String url = openAiBaseUrl + "/chat/completions";

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", ROUTER_SYSTEM_PROMPT
        );
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", userPrompt
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", openAiModel);
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("temperature", 0);
        body.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String raw = restTemplate.postForObject(url, entity, String.class);
        String content = extractChatMessageContent(raw, "OpenAI router");
        return parseRouterDecision(content);
    }

    private RouterDecision parseRouterDecision(String content) throws Exception {
        String cleaned = cleanJsonContent(content);
        JsonNode node = objectMapper.readTree(cleaned);

        String target = node.path("target").asText(null);
        String reason = node.path("reason").asText("");

        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Router response missing 'target'");
        }

        Agent agent = switch (target.trim().toUpperCase()) {
            case "RCA_SERVICE" -> Agent.RCA_SERVICE;
            case "DATA_SERVICE" -> Agent.DATA_SERVICE;
            case "OUT_OF_SCOPE" -> Agent.OUT_OF_SCOPE;
            default -> throw new IllegalArgumentException("Unknown router target: " + target);
        };

        return new RouterDecision(agent, reason);
    }

    private String cleanJsonContent(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstBrace = trimmed.indexOf('{');
            int lastBrace = trimmed.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace >= firstBrace) {
                return trimmed.substring(firstBrace, lastBrace + 1);
            }
        }
        return trimmed;
    }

    private Agent fallbackHeuristic(String prompt) {
        String lower = prompt.toLowerCase();

        boolean wantsData = lower.contains("fetch")
                || lower.contains("report")
                || lower.contains("stats")
                || lower.contains("data")
                || lower.contains("analytics")
                || lower.contains("count")
                || lower.contains("list")
                || lower.contains("view");

        boolean mentionsFailure = lower.contains("fail")
                || lower.contains("error")
                || lower.contains("exception")
                || lower.contains("500")
                || lower.contains("timeout")
                || lower.contains("bug")
                || lower.contains("outage")
                || lower.contains("issue");

        if (mentionsFailure && !wantsData) {
            return Agent.RCA_SERVICE;
        }
        if (wantsData && !mentionsFailure) {
            return Agent.DATA_SERVICE;
        }
        if (!mentionsFailure && !wantsData) {
            return Agent.OUT_OF_SCOPE;
        }
        // Ambiguous cases favor data as per instruction
        return wantsData ? Agent.DATA_SERVICE : Agent.RCA_SERVICE;
    }

    private record RouterDecision(Agent target, String reason) {
    }

    private String resolveRcaCategory(User user) {
        Role role = user.getRole();
        if (role == null) {
            return "GENERAL_RCA";
        }
        return switch (role) {
            case DEVELOPER -> "DEVELOPER_RCA";
            case PRODUCT_MANAGER -> "PRODUCT_MANAGER_RCA";
            case BUSINESS_MANAGER -> "BUSINESS_MANAGER_RCA";
            case CUSTOMER -> "CUSTOMER_RCA";
        };
    }

    private String outOfScopeMessage() {
        return """
                <p><strong>Hi! I'm PayU Sensei.</strong></p>
                <p>I can help with:</p>
                <ul>
                  <li>Investigating failures and providing RCAs.</li>
                  <li>Fetching or summarizing transaction/data insights on request.</li>
                </ul>
                <p>I stay focused on these two areas to be most helpful. Ask me about an incident/failure or a data/metrics request, and I'll jump in.</p>
                """;
    }

    private boolean isGreeting(String prompt) {
        if (prompt == null) {
            return false;
        }
        String lower = prompt.trim().toLowerCase();
        return lower.matches("^(hi|hello|hey|greetings|good\\s+(morning|afternoon|evening))(\\W.*)?$");
    }

    /**
     * Build a richer routing prompt by including up to the last 5 user prompts
     * from the same chat (if chatId is provided). This gives the router more
     * context about the user's ongoing intent.
     */
    private String buildRoutingPrompt(User user, String prompt, String chatId) {
        List<String> previous = fetchLastUserPrompts(user, chatId, 5);
        if (previous.isEmpty()) {
            return prompt;
        }
        StringBuilder sb = new StringBuilder(prompt == null ? "" : prompt);
        sb.append("\n\nPrevious user prompts in this chat (most recent first):\n");
        for (String p : previous) {
            sb.append("- ").append(p).append("\n");
        }
        return sb.toString();
    }

    private List<String> fetchLastUserPrompts(User user, String chatId, int limit) {
        try {
            if (chatId == null || chatId.isBlank()) {
                return List.of();
            }
            List<Message> messages = chatService.getConversationMessages(user, chatId);
            if (messages == null || messages.isEmpty()) {
                return List.of();
            }
            List<String> userPrompts = new ArrayList<>();
            for (Message m : messages) {
                if (m.getRole() == Message.Role.USER) {
                    userPrompts.add(m.getContent());
                }
            }

            if (userPrompts.isEmpty()) {
                return List.of();
            }
            int start = Math.max(userPrompts.size() - limit, 0);
            List<String> slice = new ArrayList<>(userPrompts.subList(start, userPrompts.size()));
            Collections.reverse(slice); // most recent first
            return slice;
        } catch (Exception ex) {
            log.warn("Unable to load chat history for routing (chatId={}): {}", chatId, ex.getMessage());
            return List.of();
        }
    }

    private String greetingMessage() {
        return """
                <p><strong>Hi! I'm PayU Sensei.</strong></p>
                <p>I can help with:</p>
                <ul>
                  <li>Investigating failures and providing RCAs.</li>
                  <li>Fetching or summarizing transaction/data insights on request.</li>
                </ul>
                <p>How can I help you today? Ask about an incident/failure or a data/metrics request.</p>
                """;
    }

    private enum Agent {
        RCA_SERVICE,
        DATA_SERVICE,
        OUT_OF_SCOPE
    }
}


