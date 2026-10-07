package com.chatapp.user;

import com.chatapp.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    public record SetAvatarRequest(@NotBlank String objectKey) {
    }

    /** Danh sách mọi user khác — dùng để chọn thành viên khi tạo group. */
    @GetMapping
    public List<UserDto> listOthers(@AuthenticationPrincipal AuthUser me) {
        return userService.listOthers(me.id());
    }

    @GetMapping("/search")
    public List<UserDto> search(@AuthenticationPrincipal AuthUser me, @RequestParam("q") String q) {
        return userService.search(me.id(), q);
    }

    @PutMapping("/me")
    public UserDto updateProfile(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileRequest req) {
        return userService.updateProfile(me.id(), req);
    }

    @PutMapping("/me/avatar")
    public UserDto setAvatar(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SetAvatarRequest req) {
        return userService.setAvatar(me.id(), req.objectKey());
    }

    @DeleteMapping("/me/avatar")
    public UserDto removeAvatar(@AuthenticationPrincipal AuthUser me) {
        return userService.removeAvatar(me.id());
    }
}
