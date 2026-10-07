package com.chatapp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket tại ws://host/ws.
 * Handshake là HTTP request đi qua Spring Security, nên cookie JWT được kiểm tra ở đó
 * và Principal của session chính là AuthUser.
 *
 * Simple broker (in-memory) đủ cho 1 instance. Khi scale nhiều instance: đổi sang
 * enableStompBrokerRelay(...) trỏ tới RabbitMQ — code nghiệp vụ không đổi.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AppProperties props;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(props.corsOrigin());
    }

    /**
     * Chỉ cho phép subscribe vào kênh riêng của chính mình (/user/...) và SEND tới /app/...
     * Chặn việc subscribe trực tiếp vào queue nội bộ của session khác.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }
                String destination = accessor.getDestination();
                boolean allowed = switch (accessor.getCommand()) {
                    case SUBSCRIBE -> destination != null && destination.startsWith("/user/");
                    case SEND -> destination != null && destination.startsWith("/app/");
                    default -> true;
                };
                if (!allowed || accessor.getUser() == null) {
                    throw new MessageDeliveryException("Forbidden destination: " + destination);
                }
                return message;
            }
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.enableSimpleBroker("/queue", "/topic");
    }
}
