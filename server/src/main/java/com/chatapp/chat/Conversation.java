package com.chatapp.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Hội thoại chung cho cả chat 1-1 (DIRECT) và nhóm (GROUP).
 * Cố ý không map @OneToMany messages — lịch sử luôn được query có phân trang qua MessageRepository.
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    private String name;

    @Column(unique = true)
    private String directKey;

    private Instant lastMessageAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static Conversation group(String name) {
        Conversation c = new Conversation();
        c.type = ConversationType.GROUP;
        c.name = name;
        return c;
    }

    public static String directKey(Long userA, Long userB) {
        return Math.min(userA, userB) + "_" + Math.max(userA, userB);
    }
}
