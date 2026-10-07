package com.chatapp.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * TEXT: cần content. FILE: cần objectKey đã upload qua presigned URL (content tuỳ chọn làm caption).
 */
public record SendMessageRequest(
        @NotNull UUID clientMsgId,
        @NotNull MessageType type,
        @Size(max = 4000) String content,
        @Size(max = 500) String objectKey) {
}
