package com.example.userservice.chat;

import com.example.userservice.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public Conversation createConversation(User user, String title) {
        List<Conversation> recent = conversationRepository.findTop10ByUserOrderByUpdatedAtDesc(user);
        if (!recent.isEmpty()) {
            Conversation latest = recent.get(0);
            long messageCount = messageRepository.countByConversation(latest);
            if (messageCount == 0) {
                return latest; // reuse empty conversation instead of creating a new one
            }
        }
        Conversation conversation = Conversation.builder()
                .user(user)
                .title(title)
                .build();
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getRecentConversations(User user) {
        return conversationRepository.findTop10ByUserOrderByUpdatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(User user, String externalId) {
        Conversation conversation = resolveConversation(user, externalId, false);
        if (conversation == null) {
            return List.of();
        }
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> findConversation(User user, String externalId) {
        if (externalId != null && !externalId.isBlank()) {
            return conversationRepository.findByExternalIdAndUser(externalId, user);
        }
        List<Conversation> recent = conversationRepository.findTop10ByUserOrderByUpdatedAtDesc(user);
        if (recent.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(recent.get(0));
    }

    @Transactional
    public Message saveUserAndAiMessages(User user, String externalConversationId, String userPrompt, String aiHtmlResponse) {
        Conversation conversation = resolveConversation(user, externalConversationId, true);

        if (conversation.getTitle() == null || conversation.getTitle().isBlank() || "New chat".equalsIgnoreCase(conversation.getTitle())) {
            conversation.setTitle(generateTitleFromPrompt(userPrompt));
        }

        Message userMsg = Message.builder()
                .conversation(conversation)
                .user(user)
                .role(Message.Role.USER)
                .content(userPrompt)
                .contentType(Message.ContentType.TEXT)
                .build();
        userMsg = messageRepository.save(userMsg);

        Message aiMsg = Message.builder()
                .conversation(conversation)
                .user(user)
                .role(Message.Role.AI)
                .content(aiHtmlResponse)
                .contentType(Message.ContentType.HTML)
                .build();
        aiMsg = messageRepository.save(aiMsg);

        conversation.setUpdatedAt(aiMsg.getCreatedAt());
        conversationRepository.save(conversation);

        return aiMsg;
    }

    @Transactional(readOnly = true)
    public List<Message> getLatestConversationMessages(User user) {
        return getConversationMessages(user, null);
    }

    @Transactional(readOnly = true)
    public Message getMessageById(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    }

    @Transactional
    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(Message message) {
        messageRepository.delete(message);
    }

    /**
     * Delete a user message and, if present, the immediate next AI message in the same conversation.
     */
    @Transactional
    public void deleteUserMessageAndFollowingAi(Message userMessage) {
        if (userMessage.getRole() != Message.Role.USER) {
            throw new IllegalArgumentException("Only user messages can trigger cascade delete.");
        }
        Conversation convo = userMessage.getConversation();
        // Delete user message
        messageRepository.delete(userMessage);
        // Find the immediate next AI message (chronologically) in the same conversation
        Message nextAi = messageRepository.findFirstByConversationAndCreatedAtAfterAndRoleOrderByCreatedAtAsc(
                convo,
                userMessage.getCreatedAt(),
                Message.Role.AI
        );
        if (nextAi != null) {
            messageRepository.delete(nextAi);
        }
    }

    private Conversation resolveConversation(User user, String externalId, boolean createIfMissing) {
        if (externalId != null && !externalId.isBlank()) {
            Optional<Conversation> existing = conversationRepository.findByExternalIdAndUser(externalId, user);
            if (existing.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
            }
            return existing.get();
        }
        List<Conversation> recent = conversationRepository.findTop10ByUserOrderByUpdatedAtDesc(user);
        if (!recent.isEmpty()) {
            return recent.get(0);
        }
        if (!createIfMissing) {
            return null;
        }
        return conversationRepository.save(Conversation.builder()
                .user(user)
                .title("New chat")
                .build());
    }

    private String generateTitleFromPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "New chat";
        }
        String cleaned = Pattern.compile("\\s+").matcher(prompt.trim()).replaceAll(" ");
        int maxLen = 60;
        if (cleaned.length() > maxLen) {
            cleaned = cleaned.substring(0, maxLen - 1).trim() + "…";
        }
        return cleaned;
    }
}


