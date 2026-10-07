package com.chatapp.chat;

import com.chatapp.media.FileUrls;
import com.chatapp.user.UserDto;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        Long id,
        Long conversationId,
        UUID clientMsgId,
        UserDto sender,
        MessageType type,
        String content,
        String fileUrl,
        String fileName,
        Long fileSize,
        String contentType,
        Instant createdAt) {

    public static MessageDto from(Message m) {
        return new MessageDto(
                m.getId(),
                m.getConversation().getId(),
                m.getClientMsgId(),
                UserDto.from(m.getSender()),
                m.getType(),
                m.getContent(),
                FileUrls.of(m.getFileKey()),
                m.getFileName(),
                m.getFileSize(),
                m.getContentType(),
                m.getCreatedAt());
    }
}
