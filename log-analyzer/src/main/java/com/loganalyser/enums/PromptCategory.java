package com.loganalyser.enums;

/**
 * Enum representing different categories of prompts for personalized LLM interactions.
 * Each category defines a specific role and goal for the AI assistant.
 *
 * @author Himanshu Sehgal
 * @since 2025-12-08
 */
public enum PromptCategory {
    /**
     * Developer Root Cause Analysis category.
     * Uses SRE/Backend Engineer persona for technical log analysis.
     */
    DEVELOPER_RCA,
    
    /**
     * General log analysis category.
     * Uses basic log analysis assistant persona.
     */
    GENERAL,
    
    /**
     * Performance analysis category.
     * Focuses on performance bottlenecks and optimization.
     */
    PERFORMANCE_ANALYSIS,
    
    /**
     * Security analysis category.
     * Focuses on security vulnerabilities and threats.
     */
    SECURITY_ANALYSIS,
    
    /**
     * Business impact analysis category.
     * Focuses on business metrics and user impact.
     */
    BUSINESS_IMPACT
}