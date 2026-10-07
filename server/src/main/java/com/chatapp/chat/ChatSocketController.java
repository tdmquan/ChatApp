package com.chatapp.chat;

import com.chatapp.common.ApiException;
import com.chatapp.common.GlobalExceptionHandler.ErrorResponse;
import com.chatapp.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP: client SEND tới /app/conversations/{id}/send.
 * Người gửi luôn lấy từ Principal của session (đã xác thực bằng cookie lúc handshake),
 * không bao giờ tin senderId trong payload.
 * Tin đã lưu được phát tới mọi thành viên (kể cả người gửi) qua /user/queue/messages.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final MessageService messageService;

    @MessageMapping("/conversations/{conversationId}/send")
    public void send(@DestinationVariable Long conversationId,
                     @Valid @Payload SendMessageRequest req,
                     Principal principal) {
        messageService.send(currentUser(principal).id(), conversationId, req);
    }

    @MessageExceptionHandler(ApiException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse handleApi(ApiException ex) {
        return new ErrorResponse(ex.getStatus().value(), ex.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse handleInvalid(MethodArgumentNotValidException ex) {
        return new ErrorResponse(400, "Invalid message payload");
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse handleUnexpected(Exception ex) {
        log.error("Unhandled WebSocket error", ex);
        return new ErrorResponse(500, "Internal server error");
    }

    private static AuthUser currentUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token && token.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        throw ApiException.unauthorized("Not authenticated");
    }
}
