package com.chatapp.media;

import com.chatapp.chat.ConversationService;
import com.chatapp.common.ApiException;
import com.chatapp.config.AppProperties;
import com.chatapp.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Upload: client xin presigned URL → PUT file thẳng lên MinIO → gửi objectKey cho API
 * (PUT /api/users/me/avatar hoặc gửi tin nhắn FILE). File không đi qua app server.
 *
 * Download: GET /api/files/{key} kiểm tra quyền rồi redirect 302 sang presigned GET URL.
 * Thêm ?download=true để trình duyệt tải file về thay vì hiển thị.
 */
@RestController
@RequiredArgsConstructor
public class MediaController {

    private final StorageService storage;
    private final ConversationService conversationService;
    private final AppProperties props;

    public enum Purpose { AVATAR, ATTACHMENT }

    public record PresignRequest(
            @NotNull Purpose purpose,
            Long conversationId,
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank @Size(max = 100) String contentType,
            @NotNull @Positive Long size) {
    }

    /** Client PUT tới uploadUrl với đúng header Content-Type đã khai báo. */
    public record PresignResponse(String uploadUrl, String objectKey, String fileUrl) {
    }

    @PostMapping("/api/media/presign")
    public PresignResponse presign(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PresignRequest req) {
        AppProperties.Storage cfg = props.storage();
        String key = switch (req.purpose()) {
            case AVATAR -> {
                if (!req.contentType().startsWith("image/")) {
                    throw ApiException.badRequest("Avatar must be an image");
                }
                checkSize(req.size(), cfg.maxAvatarSize().toBytes());
                yield ObjectKeys.avatar(me.id());
            }
            case ATTACHMENT -> {
                if (req.conversationId() == null) {
                    throw ApiException.badRequest("conversationId is required for attachments");
                }
                conversationService.requireMember(req.conversationId(), me.id());
                checkSize(req.size(), cfg.maxAttachmentSize().toBytes());
                yield ObjectKeys.attachment(req.conversationId(), req.fileName());
            }
        };
        String uploadUrl = storage.presignUpload(key, req.contentType(), req.size()).toString();
        return new PresignResponse(uploadUrl, key, FileUrls.of(key));
    }

    @GetMapping("/api/files/**")
    public ResponseEntity<Void> download(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(defaultValue = "false") boolean download,
            HttpServletRequest request) {
        String rawPath = request.getRequestURI().substring(request.getContextPath().length());
        String key = UriUtils.decode(rawPath.substring(FileUrls.PREFIX.length()), StandardCharsets.UTF_8);

        // Avatar: mọi user đăng nhập đều xem được. Attachment: chỉ thành viên conversation.
        if (ObjectKeys.avatarOwner(key).isEmpty()) {
            Long conversationId = ObjectKeys.attachmentConversation(key)
                    .orElseThrow(() -> ApiException.notFound("File not found"));
            conversationService.requireMember(conversationId, me.id());
        }

        String fileName = key.substring(key.lastIndexOf('/') + 1);
        // Cache redirect ngắn hơn hạn của presigned URL
        Duration cacheFor = props.storage().downloadUrlTtl().dividedBy(2);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(storage.presignDownload(key, download ? fileName : null).toString()))
                .cacheControl(CacheControl.maxAge(cacheFor).cachePrivate())
                .build();
    }

    private static void checkSize(long size, long max) {
        if (size > max) {
            throw ApiException.badRequest("File too large (max " + max / (1024 * 1024) + "MB)");
        }
    }
}
