package com.chatapp.security;

import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Principal của request/WebSocket session. getName() trả về userId để
 * SimpMessagingTemplate.convertAndSendToUser(userId, ...) định tuyến đúng.
 */
public record AuthUser(Long id, String email) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return id.toString();
    }
}
