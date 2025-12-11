package com.loganalyser.controller;

import com.loganalyser.dto.AnalysisResult;
import com.loganalyser.enums.PromptCategory;
import com.loganalyser.service.LogAnalysisService;
import com.loganalyser.service.TransactionIdExtractorService;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for handling log analysis queries.
 * Supports category-based personalized prompts for different analysis types.
 *
 * @author Himanshu Sehgal
 * @since 2025-12-08
 */
@RestController
@RequestMapping("/api")
@Validated
public class QueryController {

    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    private final LogAnalysisService logAnalysisService;
    private final TransactionIdExtractorService transactionIdExtractorService;

    /**
     * Constructor for QueryController.
     *
     * @param logAnalysisService The log analysis service
     * @param transactionIdExtractorService The transaction ID extractor service
     */
    public QueryController(LogAnalysisService logAnalysisService,
                          TransactionIdExtractorService transactionIdExtractorService) {
        this.logAnalysisService = logAnalysisService;
        this.transactionIdExtractorService = transactionIdExtractorService;
    }

    /**
     * Endpoint for querying log analysis with optional category.
     * Returns the response as JSON (wrapped in QueryResponse).
     *
     * @param request The query request containing query string and optional category
     * @return Response entity with analysis results as JSON
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        try {
            logger.info("Received query request: {} with category: {}", 
                    request.getQuery(), request.getCategory());

            // Try to resolve transaction ID from existing chat history if chatId is provided
            String transactionId = null;
            if (request.getChatId() != null && !request.getChatId().isBlank()) {
                transactionId = logAnalysisService.resolveTransactionIdForChat(request.getChatId())
                        .orElse(null);
                if (transactionId != null) {
                    logger.info("Resolved transaction ID {} from chatId {}", transactionId, request.getChatId());
                }
            }

            // If not found in history, extract transaction ID from the current query string
            if (transactionId == null) {
                transactionId = transactionIdExtractorService.extractTransactionId(request.getQuery());
            }

            if (transactionId == null) {
                logger.warn("Could not resolve transaction ID from chat or query: {}", request.getQuery());
                return ResponseEntity.badRequest()
                        .body(new QueryResponse("Error: Could not extract transaction ID from the query or existing chat context. Please include a transaction ID in the format TX######### (e.g., TX651750504)", null));
            }

            logger.info("Using transaction ID: {} for query", transactionId);
            
            // Use provided category or default to GENERAL
            PromptCategory category = request.getCategory() != null 
                    ? request.getCategory() 
                    : PromptCategory.GENERAL;

            // Use chat-aware analysis to support follow-up questions by chatId
            AnalysisResult result = logAnalysisService.analyzeQueryWithChat(
                    transactionId,
                    request.getQuery(),
                    category,
                    request.getChatId());

            return ResponseEntity.ok(new QueryResponse(result.response(), result.chatId()));
        } catch (Exception e) {
            logger.error("Error processing query: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new QueryResponse("Error: " + e.getMessage(), null));
        }
    }

    /**
     * Endpoint for querying log analysis with optional category and receiving a Markdown response.
     * Returns the raw analysis text from OpenAI, which is structured as markdown (headings, bullets, etc.).
     *
     * @param request The query request containing query string and optional category
     * @return Response entity with analysis results as markdown
     */
    @PostMapping(value = "/query/markdown", produces = "text/markdown")
    public ResponseEntity<String> queryMarkdown(@RequestBody QueryRequest request) {
        try {
            logger.info("Received Markdown query request: {} with category: {}", 
                    request.getQuery(), request.getCategory());

            // Try to resolve transaction ID from existing chat history if chatId is provided
            String transactionId = null;
            if (request.getChatId() != null && !request.getChatId().isBlank()) {
                transactionId = logAnalysisService.resolveTransactionIdForChat(request.getChatId())
                        .orElse(null);
                if (transactionId != null) {
                    logger.info("Resolved transaction ID {} from chatId {} for markdown query",
                            transactionId, request.getChatId());
                }
            }

            // If not found in history, extract transaction ID from the current query string
            if (transactionId == null) {
                transactionId = transactionIdExtractorService.extractTransactionId(request.getQuery());
            }

            if (transactionId == null) {
                logger.warn("Could not resolve transaction ID from chat or query: {}", request.getQuery());
                String errorMd =
                        "## Error\n\n" +
                        "Could not extract transaction ID from the query or existing chat context. " +
                        "Please include a transaction ID in the format `TX#########` (e.g., `TX651750504`).\n";
                return ResponseEntity.badRequest()
                        .contentType(MediaType.parseMediaType("text/markdown"))
                        .body(errorMd);
            }

            logger.info("Using transaction ID {} for Markdown query", transactionId);

            // Use provided category or default to GENERAL
            PromptCategory category = request.getCategory() != null
                    ? request.getCategory()
                    : PromptCategory.GENERAL;

            // Call service method that returns plain text / markdown from OpenAI with chat history
            AnalysisResult result = logAnalysisService.analyzeQueryWithChat(
                    transactionId,
                    request.getQuery(),
                    category,
                    request.getChatId());
            String markdownResponse = result.response();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/markdown"))
                    .body(markdownResponse);

        } catch (Exception e) {
            logger.error("Error processing Markdown query: {}", e.getMessage(), e);
            String errorMd =
                    "## Internal Server Error\n\n" +
                    e.getMessage() + "\n";
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.parseMediaType("text/markdown"))
                    .body(errorMd);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }

    /**
     * Request DTO for query endpoint.
     */
    public static class QueryRequest {
        @NotBlank(message = "Query cannot be blank")
        private String query;
        
        /**
         * Optional prompt category. Defaults to GENERAL if not provided.
         */
        private PromptCategory category;

        /**
         * Optional chat identifier. When provided, the last few messages for this chatId
         * will be used as context for the new query. When not provided, a new chatId is created.
         */
        private String chatId;

        /**
         * Gets the query string.
         *
         * @return The query string
         */
        public String getQuery() {
            return query;
        }

        /**
         * Sets the query string.
         *
         * @param query The query string
         */
        public void setQuery(String query) {
            this.query = query;
        }

        /**
         * Gets the prompt category.
         *
         * @return The prompt category, or null for default
         */
        public PromptCategory getCategory() {
            return category;
        }

        /**
         * Sets the prompt category.
         *
         * @param category The prompt category
         */
        public void setCategory(PromptCategory category) {
            this.category = category;
        }

        /**
         * Gets the chat identifier.
         *
         * @return The chatId, or null to start a new chat
         */
        public String getChatId() {
            return chatId;
        }

        /**
         * Sets the chat identifier.
         *
         * @param chatId The chat identifier
         */
        public void setChatId(String chatId) {
            this.chatId = chatId;
        }
    }

    public static class QueryResponse {
        private String response;
        private String chatId;

        public QueryResponse(String response, String chatId) {
            this.response = response;
            this.chatId = chatId;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public String getChatId() {
            return chatId;
        }

        public void setChatId(String chatId) {
            this.chatId = chatId;
        }
    }
}

