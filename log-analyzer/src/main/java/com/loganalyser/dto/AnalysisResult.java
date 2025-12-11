package com.loganalyser.dto;

/**
 * Simple DTO holding the analysis result text and associated chatId.
 * Used to propagate both the LLM response and the conversation identifier
 * back to controllers.
 *
 * @param response The analysis / RCA text returned by OpenAI
 * @param chatId The logical chat identifier (existing or newly created)
 *
 * @author Himanshu Sehgal
 * @since 2025-12-10
 */
public record AnalysisResult(String response, String chatId) {
}
