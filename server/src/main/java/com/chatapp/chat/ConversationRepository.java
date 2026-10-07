package com.chatapp.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByDirectKey(String directKey);

    /** Insert idempotent — an toàn khi 2 user cùng mở chat với nhau một lúc. */
    @Modifying
    @Query(value = """
            INSERT INTO conversations (type, direct_key, created_at)
            VALUES ('DIRECT', :directKey, now())
            ON CONFLICT (direct_key) DO NOTHING
            """, nativeQuery = true)
    void insertDirectIfAbsent(String directKey);

    /** Sidebar: group luôn hiện, DIRECT chỉ hiện khi đã có tin nhắn. */
    @Query("""
            SELECT c FROM Conversation c
            WHERE c.id IN (SELECT m.conversation.id FROM ConversationMember m WHERE m.user.id = :userId)
              AND (c.type = com.chatapp.chat.ConversationType.GROUP OR c.lastMessageAt IS NOT NULL)
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC
            """)
    List<Conversation> findSidebar(Long userId);

    @Modifying
    @Query("UPDATE Conversation c SET c.lastMessageAt = :at WHERE c.id = :id")
    void touch(Long id, Instant at);
}
