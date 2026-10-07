package com.chatapp.chat;

import com.chatapp.user.UserDto;

import java.time.Instant;
import java.util.List;

/**
 * @param members với DIRECT gồm 2 user (client lấy người còn lại để hiển thị tên/avatar)
 * @param adminId chỉ có với GROUP
 */
public record ConversationDto(
        Long id,
        ConversationType type,
        String name,
        Long adminId,
        List<UserDto> members,
        Instant lastMessageAt,
        Instant createdAt) {
}
