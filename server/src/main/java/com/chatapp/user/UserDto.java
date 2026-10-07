package com.chatapp.user;

import com.chatapp.media.FileUrls;

public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        Short color,
        boolean profileSetup) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                FileUrls.of(user.getAvatarKey()),
                user.getColor(),
                user.isProfileSetup());
    }
}
