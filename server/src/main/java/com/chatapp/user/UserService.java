package com.chatapp.user;

import com.chatapp.common.ApiException;
import com.chatapp.media.ObjectKeys;
import com.chatapp.media.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int SEARCH_LIMIT = 20;

    private final UserRepository users;
    private final StorageService storage;

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest req) {
        User user = get(userId);
        user.setFirstName(req.firstName().strip());
        user.setLastName(req.lastName().strip());
        user.setColor(req.color());
        user.setProfileSetup(true);
        return UserDto.from(user);
    }

    /** Gắn avatar đã được client upload lên MinIO qua presigned URL. */
    @Transactional
    public UserDto setAvatar(Long userId, String objectKey) {
        if (!ObjectKeys.avatarOwner(objectKey).map(userId::equals).orElse(false)) {
            throw ApiException.badRequest("Invalid avatar key");
        }
        if (storage.head(objectKey).isEmpty()) {
            throw ApiException.badRequest("Avatar has not been uploaded");
        }
        User user = get(userId);
        String old = user.getAvatarKey();
        user.setAvatarKey(objectKey);
        if (old != null && !old.equals(objectKey)) {
            storage.delete(old);
        }
        return UserDto.from(user);
    }

    @Transactional
    public UserDto removeAvatar(Long userId) {
        User user = get(userId);
        if (user.getAvatarKey() != null) {
            storage.delete(user.getAvatarKey());
            user.setAvatarKey(null);
        }
        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> search(Long currentUserId, String term) {
        String trimmed = term == null ? "" : term.strip();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        String escaped = trimmed.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return users.search(currentUserId, "%" + escaped + "%", PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(UserDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> listOthers(Long currentUserId) {
        return users.findByIdNotOrderByFirstNameAscLastNameAscEmailAsc(currentUserId).stream()
                .map(UserDto::from)
                .toList();
    }
}
