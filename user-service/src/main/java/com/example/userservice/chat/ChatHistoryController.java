package com.example.userservice.chat;

import com.example.userservice.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatService chatService;

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public HistoryResponse history(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "chatId", required = false) String chatId
    ) {
        Conversation conversation = chatService.findConversation(user, chatId).orElse(null);
        List<Message> messages = conversation != null
                ? chatService.getConversationMessages(user, conversation.getExternalId())
                : List.of();
        String resolvedChatId = conversation != null ? conversation.getExternalId() : chatId;

        List<MessageDto> payload = messages.stream()
                .map(m -> new MessageDto(
                        m.getId(),
                        m.getRole().name(),
                        m.getContent(),
                        m.getContentType().name(),
                        m.getCreatedAt().toString()
                ))
                .toList();

        return new HistoryResponse(resolvedChatId, payload);
    }

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public List<ConversationDto> conversations(@AuthenticationPrincipal User user) {
        return chatService.getRecentConversations(user).stream()
                .map(c -> new ConversationDto(
                        c.getExternalId(),
                        c.getTitle(),
                        c.getUpdatedAt().toString()
                ))
                .toList();
    }

    @PostMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ConversationDto createConversation(@AuthenticationPrincipal User user) {
        Conversation conversation = chatService.createConversation(user, "New chat");
        return new ConversationDto(
                conversation.getExternalId(),
                conversation.getTitle(),
                conversation.getUpdatedAt().toString()
        );
    }

    @PatchMapping("/messages/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateMessage(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id,
            @RequestBody UpdateMessageRequest request
    ) {
        Message message = chatService.getMessageById(id);
        // Ensure the message belongs to the authenticated user's conversation
        if (!message.getConversation().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        if (request.content() != null && !request.content().isBlank()) {
            message.setContent(request.content());
        }
        if (request.contentType() != null && !request.contentType().isBlank()) {
            message.setContentType(Message.ContentType.valueOf(request.contentType()));
        }

        chatService.saveMessage(message);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id
    ) {
        Message message = chatService.getMessageById(id);
        // Ensure the message belongs to the authenticated user's conversation
        if (!message.getConversation().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        if (message.getRole() != Message.Role.USER) {
            return ResponseEntity.status(403).build();
        }
        chatService.deleteUserMessageAndFollowingAi(message);
        return ResponseEntity.noContent().build();
    }

    public record HistoryResponse(
            String chatId,
            List<MessageDto> messages
    ) {
    }

    public record MessageDto(
            Long id,
            String role,
            String content,
            String contentType,
            String createdAt
    ) {
    }

    public record ConversationDto(
            String chatId,
            String title,
            String updatedAt
    ) {
    }

    public record UpdateMessageRequest(
            String content,
            String contentType
    ) {
    }
}


