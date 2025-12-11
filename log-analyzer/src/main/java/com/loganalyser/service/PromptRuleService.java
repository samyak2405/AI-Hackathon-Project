package com.loganalyser.service;

import com.loganalyser.dto.PromptRule;
import com.loganalyser.enums.PromptCategory;

/**
 * Service interface for managing prompt rules based on categories.
 * Provides methods to retrieve personalized prompt templates.
 *
 * @author Himanshu Sehgal
 * @since 2025-12-08
 */
public interface PromptRuleService {
    
    /**
     * Retrieves the prompt rule for a given category.
     * 
     * @param category The category of the prompt
     * @return PromptRule containing role, goal, and template
     */
    PromptRule getPromptRule(PromptCategory category);
    
    /**
     * Builds the final prompt string by replacing placeholders in the template.
     * 
     * @param category The category of the prompt
     * @param query The user query
     * @param logs The list of log lines
     * @return The complete prompt string ready for LLM
     */
    String buildPrompt(PromptCategory category, String query, java.util.List<String> logs);
    
    /**
     * Builds the system message (role/persona) for a given category.
     * This separates the role definition from the user query, similar to:
     * role: "developer" / role: "user"
     *
     * @param category The prompt category
     * @return The system message containing role and goal
     */
    String buildSystemMessage(PromptCategory category);
    
    /**
     * Builds the user message (query + logs) for a given category.
     *
     * @param category The prompt category
     * @param query The user query
     * @param logs The log lines
     * @return The user message
     */
    String buildUserMessage(PromptCategory category, String query, java.util.List<String> logs);
}