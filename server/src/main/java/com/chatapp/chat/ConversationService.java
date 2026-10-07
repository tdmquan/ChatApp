package com.chatapp.chat;

import com.chatapp.common.ApiException;
import com.chatapp.user.User;
import com.chatapp.user.UserDto;
import com.chatapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    /** 404 cho cả trường hợp không tồn tại lẫn không phải thành viên — không lộ conversation của người khác. */
    @Transactional(readOnly = true)
    public void requireMember(Long conversationId, Long userId) {
        if (!members.existsByIdConversationIdAndIdUserId(conversationId, userId)) {
            throw ApiException.notFound("Conversation not found");
        }
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> sidebar(Long userId) {
        return toDtos(conversations.findSidebar(userId));
    }

    @Transactional(readOnly = true)
    public ConversationDto get(Long userId, Long conversationId) {
        requireMember(conversationId, userId);
        return toDto(conversations.getReferenceById(conversationId));
    }

    /** Lấy hoặc tạo conversation 1-1 giữa 2 user. */
    @Transactional
    public ConversationDto openDirect(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) {
            throw ApiException.badRequest("Cannot open a conversation with yourself");
        }
        if (!users.existsById(otherUserId)) {
            throw ApiException.notFound("User not found");
        }
        String key = Conversation.directKey(userId, otherUserId);
        Conversation conversation = conversations.findByDirectKey(key).orElseGet(() -> {
            conversations.insertDirectIfAbsent(key);
            Conversation created = conversations.findByDirectKey(key).orElseThrow();
            members.insertIfAbsent(created.getId(), userId);
            members.insertIfAbsent(created.getId(), otherUserId);
            return created;
        });
        return toDto(conversation);
    }

    @Transactional
    public ConversationDto createGroup(Long adminId, String name, List<Long> memberIds) {
        Set<Long> ids = new LinkedHashSet<>(memberIds);
        ids.remove(adminId);
        if (ids.isEmpty()) {
            throw ApiException.badRequest("A group needs at least one other member");
        }
        List<User> others = users.findAllById(ids);
        if (others.size() != ids.size()) {
            throw ApiException.badRequest("Some members are not valid users");
        }
        User admin = users.findById(adminId).orElseThrow(() -> ApiException.notFound("User not found"));

        Conversation conversation = conversations.save(Conversation.group(name.strip()));
        List<ConversationMember> rows = new ArrayList<>();
        rows.add(new ConversationMember(conversation, admin, MemberRole.ADMIN));
        others.forEach(u -> rows.add(new ConversationMember(conversation, u, MemberRole.MEMBER)));
        members.saveAll(rows);
        members.flush();

        ConversationDto dto = toDto(conversation);
        List<Long> recipients = rows.stream().map(m -> m.getUser().getId()).toList();
        events.publishEvent(new ChatEvents.ConversationCreated(dto, recipients));
        return dto;
    }

    private ConversationDto toDto(Conversation c) {
        return toDtos(List.of(c)).getFirst();
    }

    /** Load members của nhiều conversation trong 1 query để tránh N+1. */
    private List<ConversationDto> toDtos(List<Conversation> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ConversationMember>> byConversation = members
                .findWithUsers(list.stream().map(Conversation::getId).toList()).stream()
                .collect(Collectors.groupingBy(m -> m.getId().getConversationId()));

        return list.stream().map(c -> {
            List<ConversationMember> ms = byConversation.getOrDefault(c.getId(), List.of());
            Long adminId = ms.stream()
                    .filter(m -> m.getRole() == MemberRole.ADMIN)
                    .map(m -> m.getUser().getId())
                    .findFirst().orElse(null);
            return new ConversationDto(
                    c.getId(),
                    c.getType(),
                    c.getName(),
                    adminId,
                    ms.stream().map(m -> UserDto.from(m.getUser())).toList(),
                    c.getLastMessageAt(),
                    c.getCreatedAt());
        }).toList();
    }
}
