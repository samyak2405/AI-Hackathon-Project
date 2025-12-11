package com.loganalyser.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a single message in a chat conversation with the LLM.
 * Grouped by {@code chatId} so that previous questions and answers can be
 * re-used as context for follow-up queries.
 *
 * @author Himanshu Sehgal
 * @since 2025-12-10
 */
@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Logical chat identifier supplied by the client.
     */
    @Column(name = "chat_id", nullable = false)
    private String chatId;

    /**
     * Role of the message in the chat, e.g. "user" or "assistant".
     */
    @Column(name = "role", nullable = false)
    private String role;

    /**
     * Message content as plain text / markdown.
     */
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * Optional prompt category used when generating this message.
     */
    @Column(name = "prompt_category")
    private String promptCategory;

    /**
     * Optional transaction identifier related to this chat message.
     */
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPromptCategory() {
        return promptCategory;
    }

    public void setPromptCategory(String promptCategory) {
        this.promptCategory = promptCategory;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
