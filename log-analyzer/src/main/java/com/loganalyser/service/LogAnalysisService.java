package com.loganalyser.service;

import com.loganalyser.dto.AnalysisResult;
import com.loganalyser.entity.ChatMessageEntity;
import com.loganalyser.entity.Transaction;
import com.loganalyser.enums.PromptCategory;
import com.loganalyser.repository.ChatMessageRepository;
import com.loganalyser.repository.TransactionRepository;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for orchestrating log analysis workflow.
 * Coordinates transaction lookup, log retrieval, and OpenAI analysis.
 *
 * @author Himanshu Sehgal
 * @since 2025-12-08
 */
@Service
public class LogAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(LogAnalysisService.class);

    private final TransactionRepository transactionRepository;
    private final ElasticsearchService elasticsearchService;
    private final OpenAIService openAIService;
    private final HtmlFormatterService htmlFormatterService;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${elasticsearch.page.size:100}")
    private int pageSize;

    /**
     * Constructor for LogAnalysisService.
     *
     * @param transactionRepository The transaction repository
     * @param elasticsearchService The Elasticsearch service
     * @param openAIService The OpenAI service
     * @param htmlFormatterService The HTML formatter service
     * @param chatMessageRepository The chat message repository
     */
    public LogAnalysisService(
            TransactionRepository transactionRepository,
            ElasticsearchService elasticsearchService,
            OpenAIService openAIService,
            HtmlFormatterService htmlFormatterService,
            ChatMessageRepository chatMessageRepository) {
        this.transactionRepository = transactionRepository;
        this.elasticsearchService = elasticsearchService;
        this.openAIService = openAIService;
        this.htmlFormatterService = htmlFormatterService;
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * Resolves the transaction ID for a given chatId by inspecting previously stored
     * chat messages. It looks for the oldest message in the recent history that has
     * a non-null, non-blank transactionId.
     *
     * @param chatId The logical chat identifier
     * @return Optional containing the resolved transactionId, if any
     */
    public Optional<String> resolveTransactionIdForChat(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return Optional.empty();
        }

        // Get recent messages in descending order (newest first)
        List<ChatMessageEntity> recent = chatMessageRepository.findTop10ByChatIdOrderByCreatedAtDesc(chatId);
        if (recent.isEmpty()) {
            return Optional.empty();
        }

        // Iterate from oldest to newest to find the first message with a transactionId
        for (int i = recent.size() - 1; i >= 0; i--) {
            ChatMessageEntity msg = recent.get(i);
            String txnId = msg.getTransactionId();
            if (txnId != null && !txnId.isBlank()) {
                return Optional.of(txnId);
            }
        }

        return Optional.empty();
    }

    /**
     * Main orchestration method that processes the query.
     * Uses default GENERAL category for backward compatibility.
     *
     * @param transactionId The transaction ID
     * @param query The query string
     * @return Analysis result from OpenAI
     */
    public String analyzeQuery(String transactionId, String query) {
        return analyzeQuery(transactionId, query, PromptCategory.GENERAL);
    }

    /**
     * Main orchestration method that processes the query with a specific prompt category.
     * This delegates to {@link #analyzeQueryWithChat(String, String, PromptCategory, String)}
     * without supplying a chatId, so a new chat context is created if needed.
     *
     * @param transactionId The transaction ID
     * @param query The query string
     * @param category The prompt category to use for personalized analysis
     * @return Analysis result from OpenAI (plain text)
     */
    public String analyzeQuery(String transactionId, String query, PromptCategory category) {
        AnalysisResult result = analyzeQueryWithChat(transactionId, query, category, null);
        return result.response();
    }

    /**
     * Main orchestration method that processes the query with a specific prompt category and chatId.
     * Uses the last few user/assistant messages for the given chatId as additional context for OpenAI.
     *
     * @param transactionId The transaction ID
     * @param query The query string
     * @param category The prompt category to use for personalized analysis
     * @param chatId The logical chat identifier (may be null to start a new chat)
     * @return Analysis result from OpenAI (plain text) and the effective chatId
     */
    public AnalysisResult analyzeQueryWithChat(String transactionId, String query, PromptCategory category, String chatId) {
        logger.info("Processing query with chat - Transaction ID: {}, Query: {}, chatId: {}", transactionId, query, chatId);

        // Resolve or create chatId
        String effectiveChatId = (chatId == null || chatId.isBlank())
                ? UUID.randomUUID().toString()
                : chatId;

        // Step 1: Search database for transaction ID and get UUID/service ID
        Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);

        if (transactionOpt.isEmpty()) {
            logger.warn("Transaction ID not found in database: {}", transactionId);
            String error = "Error: Transaction ID '" + transactionId + "' not found in database.";
            return new AnalysisResult(error, effectiveChatId);
        }

        Transaction transaction = transactionOpt.get();
        String uuid = transaction.getUuid();
        String serviceId = transaction.getServiceId();

        // Print UUID and Service ID
        logger.info("=== Transaction Details ===");
        logger.info("Transaction ID: {}", transactionId);
        if (uuid != null && !uuid.isEmpty()) {
            logger.info("UUID: {}", uuid);
            System.out.println("UUID: " + uuid);
        }
        if (serviceId != null && !serviceId.isEmpty()) {
            logger.info("Service ID: {}", serviceId);
            System.out.println("Service ID: " + serviceId);
        }
        logger.info("===========================");

        // Step 2: Get logs from Elasticsearch with pagination
        logger.info("Fetching logs from Elasticsearch for transaction ID: {}", transactionId);
        List<String> logs = elasticsearchService.getAllLogsByTransactionId(transactionId, pageSize);

        if (logs.isEmpty()) {
            logger.warn("No logs found in Elasticsearch for transaction ID: {}", transactionId);

            // Try searching by UUID as well
            if (uuid != null && !uuid.isEmpty()) {
                logger.info("Trying to fetch logs by UUID: {}", uuid);
                logs = elasticsearchService.getAllLogsByTransactionId(uuid, pageSize);
            }

            if (logs.isEmpty()) {
                String noLogsMessage = "No logs found for transaction ID: " + transactionId +
                        (uuid != null ? " (UUID: " + uuid + ")" : "") +
                        (serviceId != null ? " (Service ID: " + serviceId + ")" : "");
                return new AnalysisResult(noLogsMessage, effectiveChatId);
            }
        }

        logger.info("Found {} log lines for transaction ID: {} (fetched in paginated form)",
                logs.size(), transactionId);

        // Select the most relevant logs to stay within OpenAI context window (tokens limit)
        logs = selectRelevantLogsForOpenAI(logs);

        // Build chat history messages from previous stored messages
        List<ChatMessage> historyMessages = buildHistoryMessages(effectiveChatId);

        // Step 3: Send query and logs to OpenAI with category (plain text) and history
        String response = openAIService.analyzeLogs(query, logs, category, historyMessages);

        // Persist the new user and assistant messages
        persistChatMessage(effectiveChatId, ChatMessageRole.USER.value(), query, category, transactionId);
        persistChatMessage(effectiveChatId, ChatMessageRole.ASSISTANT.value(), response, category, transactionId);

        logger.info("Successfully processed query with category: {} and chatId: {}", category, effectiveChatId);
        return new AnalysisResult(response, effectiveChatId);
    }

    /**
     * Selects the most relevant log lines (errors, warnings, exceptions, failures, etc.)
     * and a small amount of surrounding context, then caps overall size by characters.
     * This gives OpenAI the highest-signal logs while staying under the model context limit.
     *
     * @param logs Full list of log lines
     * @return Filtered list of log lines
     */
    private List<String> selectRelevantLogsForOpenAI(List<String> logs) {
        if (logs == null || logs.isEmpty()) {
            return logs;
        }

        int n = logs.size();
        boolean[] keep = new boolean[n];

        // Always keep a bit of start and end for timeline context
        int headCount = Math.min(15, n);
        int tailCount = Math.min(15, n);
        for (int i = 0; i < headCount; i++) {
            keep[i] = true;
        }
        for (int i = n - tailCount; i < n; i++) {
            if (i >= 0) {
                keep[i] = true;
            }
        }

        // Keywords that usually indicate important events
        String[] keywords = {
                "ERROR", "WARN", "FATAL", "Exception", "exception", "timeout",
                "TIMEOUT", "failed", "FAILED", "failure", "FAILURE", "rollback", "ROLLBACK"
        };

        // Mark important lines and a small window around them
        int contextBefore = 5;
        int contextAfter = 3;

        for (int i = 0; i < n; i++) {
            String line = logs.get(i);
            if (line == null) {
                continue;
            }
            boolean matches = false;
            for (String kw : keywords) {
                if (line.contains(kw)) {
                    matches = true;
                    break;
                }
            }
            if (matches) {
                int start = Math.max(0, i - contextBefore);
                int end = Math.min(n - 1, i + contextAfter);
                for (int j = start; j <= end; j++) {
                    keep[j] = true;
                }
            }
        }

        // Now build final list with a global char cap
        final int maxChars = 10_000; // ~2500 tokens, safe with prompt + 400-800 completion
        int totalChars = 0;
        List<String> result = new java.util.ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (!keep[i]) {
                continue;
            }
            String line = logs.get(i);
            if (line == null) {
                continue;
            }
            int len = line.length();
            if (totalChars + len > maxChars) {
                break;
            }
            result.add(line);
            totalChars += len;
        }

        logger.info("Selected relevant logs for OpenAI: original lines = {}, kept = {}, approx chars = {}",
                logs.size(), result.size(), totalChars);

        return result;
    }

    /**
     * Builds a list of ChatMessage objects from the last few stored chat messages for the given chatId.
     *
     * @param chatId The logical chat identifier
     * @return List of chat messages in chronological order
     */
    private List<ChatMessage> buildHistoryMessages(String chatId) {
        List<ChatMessageEntity> lastMessagesDesc =
                chatMessageRepository.findTop10ByChatIdOrderByCreatedAtDesc(chatId);
        if (lastMessagesDesc.isEmpty()) {
            return new ArrayList<>();
        }

        // Reverse to chronological order (oldest first)
        java.util.Collections.reverse(lastMessagesDesc);

        List<ChatMessage> history = new ArrayList<>();
        for (ChatMessageEntity entity : lastMessagesDesc) {
            if (entity.getRole() == null || entity.getContent() == null) {
                continue;
            }
            if (ChatMessageRole.USER.value().equals(entity.getRole())
                    || ChatMessageRole.ASSISTANT.value().equals(entity.getRole())) {
                history.add(new ChatMessage(entity.getRole(), entity.getContent()));
            }
        }
        return history;
    }

    /**
     * Persists a single chat message to the database.
     *
     * @param chatId        The logical chat identifier
     * @param role          The chat role (user/assistant)
     * @param content       The message content
     * @param category      The prompt category
     * @param transactionId The related transaction id
     */
    private void persistChatMessage(String chatId, String role, String content,
                                    PromptCategory category, String transactionId) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setChatId(chatId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setPromptCategory(category != null ? category.name() : null);
        entity.setTransactionId(transactionId);
        entity.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(entity);
    }

    /**
     * Main orchestration method that processes the query with a specific prompt category
     * and requests HTML output from OpenAI.
     *
     * @param transactionId The transaction ID
     * @param query The query string
     * @param category The prompt category to use for personalized analysis
     * @return Analysis result from OpenAI as HTML
     */
    public String analyzeQueryHtml(String transactionId, String query, PromptCategory category) {
        logger.info("Processing HTML query - Transaction ID: {}, Query: {}", transactionId, query);

        // Reuse the existing logic to fetch transaction, logs and get text response
        String textResponse = analyzeQuery(transactionId, query, category);

        // Wrap the text response into a nicely styled HTML page
        return htmlFormatterService.formatToHtml(textResponse);
    }
}

