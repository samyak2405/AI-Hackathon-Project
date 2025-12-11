package com.loganalyser.service;

import com.loganalyser.config.OpenAIConfig;
import com.loganalyser.enums.PromptCategory;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with OpenAI API to analyze logs.
 * Uses PromptRuleService to apply category-based personalized prompts.
 *
 * @author Himanshu Sehgal
 * @since 2025-12-08
 */
@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    private final OpenAiService openAiService;
    private final OpenAIConfig openAIConfig;
    private final PromptRuleService promptRuleService;

    /**
     * Constructor for OpenAIService.
     *
     * @param openAiService The OpenAI service instance
     * @param openAIConfig The OpenAI configuration
     * @param promptRuleService The prompt rule service for category-based prompts
     */
    public OpenAIService(OpenAiService openAiService, OpenAIConfig openAIConfig, PromptRuleService promptRuleService) {
        this.openAiService = openAiService;
        this.openAIConfig = openAIConfig;
        this.promptRuleService = promptRuleService;
    }

    /**
     * Sends query and logs to OpenAI and returns the response.
     * Uses default GENERAL category for backward compatibility.
     *
     * @param query The original query string
     * @param logs The log lines related to the transaction
     * @return OpenAI response
     */
    public String analyzeLogs(String query, List<String> logs) {
        return analyzeLogs(query, logs, PromptCategory.GENERAL);
    }

    /**
     * Sends query and logs to OpenAI with a specific prompt category and returns the response as plain text.
     *
     * @param query The original query string
     * @param logs The log lines related to the transaction
     * @param category The prompt category to use for personalized prompts
     * @return OpenAI response (plain text)
     */
    public String analyzeLogs(String query, List<String> logs, PromptCategory category) {
        return analyzeLogsInternal(query, logs, category, false, null);
    }

    /**
     * Sends query and logs to OpenAI with a specific prompt category and existing chat history
     * (previous user/assistant messages) and returns the response as plain text.
     *
     * @param query The original query string
     * @param logs The log lines related to the transaction
     * @param category The prompt category to use for personalized prompts
     * @param historyMessages Prior chat messages (user/assistant) to include as context
     * @return OpenAI response (plain text)
     */
    public String analyzeLogs(String query, List<String> logs, PromptCategory category, List<ChatMessage> historyMessages) {
        return analyzeLogsInternal(query, logs, category, false, historyMessages);
    }

    /**
     * Sends query and logs to OpenAI with a specific prompt category and returns the response as HTML.
     * This method instructs OpenAI to respond with a complete HTML document, which can be returned
     * directly from the /api/query endpoint with Content-Type: text/html.
     *
     * @param query The original query string
     * @param logs The log lines related to the transaction
     * @param category The prompt category to use for personalized prompts
     * @return OpenAI response formatted as HTML
     */
    public String analyzeLogsAsHtml(String query, List<String> logs, PromptCategory category) {
        return analyzeLogsInternal(query, logs, category, true, null);
    }

    /**
     * Internal method to send query and logs to OpenAI with a specific prompt category.
     * When htmlOutput is true, the SYSTEM message will instruct the model to return a complete HTML document.
     *
     * @param query The original query string
     * @param logs The log lines related to the transaction
     * @param category The prompt category to use for personalized prompts
     * @param htmlOutput Whether to request HTML output from OpenAI
     * @param historyMessages Optional prior chat messages (user/assistant) to include as context
     * @return OpenAI response as plain text or HTML
     */
    private String analyzeLogsInternal(String query, List<String> logs, PromptCategory category, boolean htmlOutput,
                                       List<ChatMessage> historyMessages) {
        try {
            logger.info("Analyzing logs with category: {} and htmlOutput: {}", category, htmlOutput);

            // Create chat messages with SYSTEM and USER roles (similar to role: "developer", role: "user" pattern)
            List<ChatMessage> messages = new ArrayList<>();
            
            // Add SYSTEM message with role/persona definition (equivalent to role: "developer")
            StringBuilder systemMessageBuilder = new StringBuilder(promptRuleService.buildSystemMessage(category));
            if (htmlOutput) {
                systemMessageBuilder
                        .append("\n\nOUTPUT FORMAT:\n")
                        .append("You MUST respond with a complete, valid HTML5 document. ")
                        .append("Include <!DOCTYPE html>, <html>, <head>, and <body> tags. ")
                        .append("Use semantic HTML elements like <h1>, <h2>, <h3>, <p>, <ul>, <li>, and <pre><code> ")
                        .append("to structure the response. Do NOT return JSON or markdown.");
            }
            String systemMessage = systemMessageBuilder.toString();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage));

            // Add prior chat history (user / assistant turns) if provided
            if (historyMessages != null && !historyMessages.isEmpty()) {
                messages.addAll(historyMessages);
            }

            // Build the user message with query and logs (equivalent to role: "user")
            String userMessage = promptRuleService.buildUserMessage(category, query, logs);
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), userMessage));

            // Create completion request
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                    .model(openAIConfig.getModel())
                    .messages(messages)
                    .maxTokens(openAIConfig.getMaxTokens())
                    .temperature(0.7)
                    .build();

            logger.info("Sending request to OpenAI with model: {}, category: {}, htmlOutput: {}", 
                    openAIConfig.getModel(), category, htmlOutput);

            // Get response
            String response = openAiService.createChatCompletion(chatCompletionRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            logger.info("Received response from OpenAI for category: {} with htmlOutput: {}", category, htmlOutput);
            return response;

        } catch (Exception e) {
            logger.error("Error calling OpenAI API with category {} and htmlOutput {}: {}", 
                    category, htmlOutput, e.getMessage(), e);
            throw new RuntimeException("Failed to get response from OpenAI: " + e.getMessage(), e);
        }
    }
}

