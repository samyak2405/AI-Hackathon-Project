package com.example.userservice.chat;

import com.example.userservice.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findTop10ByUserOrderByUpdatedAtDesc(User user);

    Optional<Conversation> findByExternalIdAndUser(String externalId, User user);
}


