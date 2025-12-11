package com.example.userservice.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    Message findFirstByConversationAndCreatedAtAfterAndRoleOrderByCreatedAtAsc(
            Conversation conversation,
            Instant createdAt,
            Message.Role role
    );

    long countByConversation(Conversation conversation);
}


