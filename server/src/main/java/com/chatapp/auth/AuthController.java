package com.chatapp.auth;

import com.chatapp.security.AuthCookies;
import com.chatapp.security.AuthUser;
import com.chatapp.security.JwtService;
import com.chatapp.user.User;
import com.chatapp.user.UserDto;
import com.chatapp.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthCookies authCookies;

    public record SignupRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 6, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@Valid @RequestBody SignupRequest req) {
        return withSession(HttpStatus.CREATED, authService.signup(req.email(), req.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody LoginRequest req) {
        return withSession(HttpStatus.OK, authService.login(req.email(), req.password()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthUser me) {
        return UserDto.from(userService.get(me.id()));
    }

    private ResponseEntity<UserDto> withSession(HttpStatus status, User user) {
        String token = jwtService.issue(new AuthUser(user.getId(), user.getEmail()));
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, authCookies.create(token, jwtService.ttl()).toString())
                .body(UserDto.from(user));
    }
}
