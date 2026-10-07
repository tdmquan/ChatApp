package com.chatapp.auth;

import com.chatapp.common.ApiException;
import com.chatapp.user.User;
import com.chatapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(String email, String password) {
        String normalized = normalize(email);
        if (users.existsByEmail(normalized)) {
            throw ApiException.conflict("Email is already registered");
        }
        return users.save(new User(normalized, passwordEncoder.encode(password)));
    }

    @Transactional(readOnly = true)
    public User login(String email, String password) {
        return users.findByEmail(normalize(email))
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
