package com.loganalyser.repository;

import com.loganalyser.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for storing and retrieving chat messages grouped by chatId.
 *
 * @author Himanshu Sehgal
 * @since 2025-12-10
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * Returns the most recent chat messages for a given chatId, ordered from newest to oldest.
     *
     * @param chatId The logical chat identifier
     * @return List of chat messages in descending created order
     */
    List<ChatMessageEntity> findTop10ByChatIdOrderByCreatedAtDesc(String chatId);
}
