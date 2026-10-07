package com.chatapp.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Đẩy event ra WebSocket sau khi transaction commit, để client nhận được tin
 * thì gọi lại API cũng chắc chắn thấy dữ liệu trong DB.
 *
 * Client subscribe:
 *   /user/queue/messages       — tin nhắn mới trong mọi conversation của mình
 *   /user/queue/conversations  — được thêm vào group mới
 */
@Component
@RequiredArgsConstructor
public class RealtimeNotifier {

    public static final String MESSAGES_QUEUE = "/queue/messages";
    public static final String CONVERSATIONS_QUEUE = "/queue/conversations";

    private final SimpMessagingTemplate messaging;

    @TransactionalEventListener
    public void onMessageCreated(ChatEvents.MessageCreated event) {
        event.recipientIds().forEach(userId ->
                messaging.convertAndSendToUser(userId.toString(), MESSAGES_QUEUE, event.message()));
    }

    @TransactionalEventListener
    public void onConversationCreated(ChatEvents.ConversationCreated event) {
        event.recipientIds().forEach(userId ->
                messaging.convertAndSendToUser(userId.toString(), CONVERSATIONS_QUEUE, event.conversation()));
    }
}
