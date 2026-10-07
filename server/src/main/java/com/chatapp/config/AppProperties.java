package com.chatapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String corsOrigin, Jwt jwt, Cookie cookie, Storage storage) {

    public record Jwt(String secret, Duration ttl) {
    }

    public record Cookie(String name, boolean secure, String sameSite) {
    }

    public record Storage(
            String endpoint,
            String publicEndpoint,
            String region,
            String accessKey,
            String secretKey,
            String bucket,
            Duration uploadUrlTtl,
            Duration downloadUrlTtl,
            DataSize maxAvatarSize,
            DataSize maxAttachmentSize) {
    }
}
