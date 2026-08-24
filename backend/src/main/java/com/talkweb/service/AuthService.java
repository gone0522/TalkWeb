package com.talkweb.service;

import com.talkweb.dto.AuthRequest;
import com.talkweb.dto.AuthResponse;
import com.talkweb.dto.ChangePasswordRequest;
import com.talkweb.dto.RegisterRequest;
import com.talkweb.entity.User;
import com.talkweb.repository.UserRepository;
import com.talkweb.security.JwtTokenProvider;
import com.talkweb.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("使用者不存在"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BadCredentialsException("此帳號已被停用");
        }

        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .hasCustomAvatar(user.getAvatarData() != null && user.getAvatarData().length > 0)
                .avatarDefaultIcon(user.getAvatarDefaultIcon() != null ? user.getAvatarDefaultIcon() : 1)
                .isAdmin(Boolean.TRUE.equals(user.getIsAdmin()))
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("此帳號名稱 (" + username + ") 已被註冊，請更換其他帳號");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname().trim())
                .isAdmin(false)
                .mustChangePassword(false)
                .status("ACTIVE")
                .avatarDefaultIcon(1)
                .build();

        user = userRepository.save(user);
        log.info("新用戶自主註冊成功: {}", user.getUsername());

        String token = tokenProvider.generateTokenFromUser(user.getId(), user.getUsername(), false);

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .hasCustomAvatar(false)
                .avatarDefaultIcon(1)
                .isAdmin(false)
                .mustChangePassword(false)
                .build();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    public static String generateOneTimePassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
