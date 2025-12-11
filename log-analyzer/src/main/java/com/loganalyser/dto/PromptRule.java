package com.loganalyser.dto;

import com.loganalyser.enums.PromptCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for prompt rules.
 * Contains role, goal, and template for personalized LLM prompts based on category.
 *
 * @param category The category of the prompt (e.g., DEVELOPER_RCA)
 * @param role The role/persona of the AI assistant
 * @param goal The goal/objective of the AI assistant
 * @param template The full prompt template with placeholders for query and logs
 *
 * @author Himanshu Sehgal
 * @since 2025-12-08
 */
public record PromptRule(
        @NotNull(message = "Category cannot be null") PromptCategory category,
        @NotBlank(message = "Role cannot be blank") String role,
        @NotBlank(message = "Goal cannot be blank") String goal,
        @NotBlank(message = "Template cannot be blank") String template
) {
    /**
     * Compact constructor to validate input parameters.
     */
    public PromptRule {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be blank");
        }
        if (goal == null || goal.isBlank()) {
            throw new IllegalArgumentException("Goal cannot be blank");
        }
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("Template cannot be blank");
        }
    }
}