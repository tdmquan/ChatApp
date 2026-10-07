package com.chatapp.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.Id> {

    boolean existsByIdConversationIdAndIdUserId(Long conversationId, Long userId);

    @Query("SELECT m.id.userId FROM ConversationMember m WHERE m.id.conversationId = :conversationId")
    List<Long> findUserIds(Long conversationId);

    @Query("""
            SELECT m FROM ConversationMember m JOIN FETCH m.user
            WHERE m.id.conversationId IN :conversationIds
            ORDER BY m.joinedAt
            """)
    List<ConversationMember> findWithUsers(Collection<Long> conversationIds);

    @Modifying
    @Query(value = """
            INSERT INTO conversation_members (conversation_id, user_id, role, joined_at)
            VALUES (:conversationId, :userId, 'MEMBER', now())
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(Long conversationId, Long userId);
}
