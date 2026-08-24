package com.talkweb.service;

import com.talkweb.dto.CreateUserRequest;
import com.talkweb.dto.UpdateProfileRequest;
import com.talkweb.dto.UserDto;
import com.talkweb.entity.User;
import com.talkweb.repository.UserRepository;
import com.talkweb.websocket.PresenceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PresenceManager presenceManager;

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));
        return mapToDto(user);
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));

        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getAvatarDefaultIcon() != null) {
            user.setAvatarDefaultIcon(request.getAvatarDefaultIcon());
        }
        user = userRepository.save(user);
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllActiveContacts(Long currentUserId) {
        List<User> activeUsers = userRepository.findByStatusOrderByNicknameAsc("ACTIVE");
        return activeUsers.stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String keyword, Long currentUserId) {
        List<User> matchedUsers = (keyword == null || keyword.trim().isEmpty())
                ? userRepository.findByStatusOrderByNicknameAsc("ACTIVE")
                : userRepository.searchActiveUsers(keyword.trim());

        return matchedUsers.stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto checkUserByUsername(String username, Long currentUserId) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("請輸入欲搜尋的帳號");
        }
        String cleanUsername = username.trim().toLowerCase();
        User user = userRepository.findByUsername(cleanUsername)
                .orElseThrow(() -> new IllegalArgumentException("找不到帳號為「" + cleanUsername + "」的使用者，請確認帳號是否正確"));

        if (user.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("不能將自己新增為好友");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("此帳號已被停用，無法新增為好友");
        }

        return mapToDto(user);
    }

    // Admin Operations
    @Transactional
    public String createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("該使用者名稱 (" + request.getUsername() + ") 已存在");
        }

        String otp = AuthService.generateOneTimePassword();

        User user = User.builder()
                .username(request.getUsername().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(otp))
                .nickname(request.getNickname().trim())
                .isAdmin(request.isAdmin())
                .avatarDefaultIcon(request.getDefaultAvatarIcon() != null ? request.getDefaultAvatarIcon() : 1)
                .mustChangePassword(true)
                .status("ACTIVE")
                .build();

        userRepository.save(user);
        log.info("管理員建立了新使用者: {}, is_admin: {}", user.getUsername(), user.getIsAdmin());
        return otp;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsersForAdmin() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));
        user.setStatus(status.toUpperCase());
        userRepository.save(user);
    }

    @Transactional
    public String resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));

        String otp = AuthService.generateOneTimePassword();
        user.setPasswordHash(passwordEncoder.encode(otp));
        user.setMustChangePassword(true);
        userRepository.save(user);
        return otp;
    }

    public UserDto mapToDto(User user) {
        boolean online = presenceManager != null && presenceManager.isUserOnline(user.getId());
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .hasCustomAvatar(user.getAvatarData() != null && user.getAvatarData().length > 0)
                .avatarDefaultIcon(user.getAvatarDefaultIcon() != null ? user.getAvatarDefaultIcon() : 1)
                .isAdmin(Boolean.TRUE.equals(user.getIsAdmin()))
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .status(user.getStatus())
                .online(online)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
