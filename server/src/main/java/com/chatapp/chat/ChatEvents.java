package com.chatapp.chat;

import java.util.List;

/**
 * Domain event — được phát trong transaction và chỉ đẩy ra WebSocket sau khi commit
 * (xem RealtimeNotifier). Muốn chạy nhiều instance thì chỉ cần thay listener bằng Redis/RabbitMQ relay.
 */
public final class ChatEvents {

    private ChatEvents() {
    }

    public record MessageCreated(MessageDto message, List<Long> recipientIds) {
    }

    public record ConversationCreated(ConversationDto conversation, List<Long> recipientIds) {
    }
}
