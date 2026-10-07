package com.chatapp.chat;

import com.chatapp.common.ApiException;
import com.chatapp.media.ObjectKeys;
import com.chatapp.media.StorageService;
import com.chatapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    public static final int DEFAULT_PAGE_SIZE = 30;
    public static final int MAX_PAGE_SIZE = 100;

    private final MessageRepository messages;
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ConversationService conversationService;
    private final UserRepository users;
    private final StorageService storage;
    private final ApplicationEventPublisher events;

    /** Trả về tối đa `limit` tin, sắp xếp cũ → mới. Truyền beforeId = id của tin cũ nhất đang có để tải thêm. */
    @Transactional(readOnly = true)
    public List<MessageDto> history(Long userId, Long conversationId, Long beforeId, Integer limit) {
        conversationService.requireMember(conversationId, userId);
        int size = limit == null ? DEFAULT_PAGE_SIZE : Math.clamp(limit, 1, MAX_PAGE_SIZE);
        PageRequest page = PageRequest.of(0, size);
        List<Message> result = beforeId == null
                ? messages.findLatest(conversationId, page)
                : messages.findBefore(conversationId, beforeId, page);
        return result.reversed().stream().map(MessageDto::from).toList();
    }

    @Transactional
    public MessageDto send(Long senderId, Long conversationId, SendMessageRequest req) {
        conversationService.requireMember(conversationId, senderId);

        // Client gửi lại (mất mạng, retry) → trả về bản đã lưu, không tạo trùng
        var existing = messages.findBySenderAndClientMsgId(senderId, req.clientMsgId());
        if (existing.isPresent()) {
            Message m = existing.get();
            if (!m.getConversation().getId().equals(conversationId)) {
                throw ApiException.conflict("clientMsgId already used");
            }
            return MessageDto.from(m);
        }

        Message message = new Message();
        message.setConversation(conversations.getReferenceById(conversationId));
        message.setSender(users.getReferenceById(senderId));
        message.setClientMsgId(req.clientMsgId());
        message.setType(req.type());

        switch (req.type()) {
            case TEXT -> {
                if (req.content() == null || req.content().isBlank()) {
                    throw ApiException.badRequest("content is required for TEXT messages");
                }
                message.setContent(req.content());
            }
            case FILE -> attachFile(message, conversationId, req);
        }

        messages.saveAndFlush(message);
        conversations.touch(conversationId, message.getCreatedAt());

        MessageDto dto = MessageDto.from(message);
        events.publishEvent(new ChatEvents.MessageCreated(dto, members.findUserIds(conversationId)));
        return dto;
    }

    private void attachFile(Message message, Long conversationId, SendMessageRequest req) {
        String key = req.objectKey();
        if (key == null || !ObjectKeys.attachmentConversation(key).map(conversationId::equals).orElse(false)) {
            throw ApiException.badRequest("Invalid objectKey for this conversation");
        }
        HeadObjectResponse head = storage.head(key)
                .orElseThrow(() -> ApiException.badRequest("File has not been uploaded"));
        message.setFileKey(key);
        message.setFileName(key.substring(key.lastIndexOf('/') + 1));
        message.setFileSize(head.contentLength());
        message.setContentType(head.contentType());
        if (req.content() != null && !req.content().isBlank()) {
            message.setContent(req.content());
        }
    }
}
