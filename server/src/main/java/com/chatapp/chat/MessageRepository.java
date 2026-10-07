package com.chatapp.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m JOIN FETCH m.sender
            WHERE m.conversation.id = :conversationId
            ORDER BY m.id DESC
            """)
    List<Message> findLatest(Long conversationId, Pageable pageable);

    /** Cursor pagination: lấy các tin cũ hơn beforeId (cuộn lên). */
    @Query("""
            SELECT m FROM Message m JOIN FETCH m.sender
            WHERE m.conversation.id = :conversationId AND m.id < :beforeId
            ORDER BY m.id DESC
            """)
    List<Message> findBefore(Long conversationId, Long beforeId, Pageable pageable);

    @Query("""
            SELECT m FROM Message m JOIN FETCH m.sender
            WHERE m.sender.id = :senderId AND m.clientMsgId = :clientMsgId
            """)
    Optional<Message> findBySenderAndClientMsgId(Long senderId, UUID clientMsgId);
}
