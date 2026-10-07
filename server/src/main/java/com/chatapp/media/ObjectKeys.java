package com.chatapp.media;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quy ước object key trong bucket:
 * <pre>
 *   avatars/{userId}/{uuid}
 *   conversations/{conversationId}/{uuid}/{fileName}
 * </pre>
 * Tên file gốc được làm sạch, UUID đảm bảo không đoán được và không ghi đè lẫn nhau.
 */
public final class ObjectKeys {

    private static final Pattern AVATAR = Pattern.compile("^avatars/(\\d+)/[0-9a-f-]{36}$");
    private static final Pattern ATTACHMENT = Pattern.compile("^conversations/(\\d+)/[0-9a-f-]{36}/[^/]+$");

    private ObjectKeys() {
    }

    public static String avatar(Long userId) {
        return "avatars/" + userId + "/" + UUID.randomUUID();
    }

    public static String attachment(Long conversationId, String fileName) {
        return "conversations/" + conversationId + "/" + UUID.randomUUID() + "/" + sanitize(fileName);
    }

    public static Optional<Long> avatarOwner(String key) {
        return group(AVATAR.matcher(key));
    }

    public static Optional<Long> attachmentConversation(String key) {
        return group(ATTACHMENT.matcher(key));
    }

    static String sanitize(String fileName) {
        String name = fileName == null ? "" : fileName;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[^\\p{L}\\p{N}._ -]", "_").strip();
        if (name.isEmpty() || name.chars().allMatch(c -> c == '.')) {
            name = "file";
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private static Optional<Long> group(Matcher m) {
        return m.matches() ? Optional.of(Long.valueOf(m.group(1))) : Optional.empty();
    }
}
