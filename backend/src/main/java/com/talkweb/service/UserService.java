package com.talkweb.service;

import com.talkweb.dto.CreateUserRequest;
import com.talkweb.dto.UpdateProfileRequest;
import com.talkweb.dto.UserDto;
import com.talkweb.entity.Friendship;
import com.talkweb.entity.User;
import com.talkweb.repository.FriendshipRepository;
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
    private final FriendshipRepository friendshipRepository;
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

    /**
     * 取得使用者的好友清單（僅回傳已建立好友關聯之用戶）
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllActiveContacts(Long currentUserId) {
        List<Friendship> friendships = friendshipRepository.findFriendsByUserId(currentUserId);
        return friendships.stream()
                .map(f -> {
                    UserDto dto = mapToDto(f.getFriend());
                    dto.setIsFriend(true);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 在好友清單中進行關鍵字過濾搜尋
     */
    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String keyword, Long currentUserId) {
        List<UserDto> friends = getAllActiveContacts(currentUserId);
        if (keyword == null || keyword.trim().isEmpty()) {
            return friends;
        }
        String cleanKw = keyword.trim().toLowerCase();
        return friends.stream()
                .filter(u -> (u.getNickname() != null && u.getNickname().toLowerCase().contains(cleanKw))
                        || (u.getUsername() != null && u.getUsername().toLowerCase().contains(cleanKw)))
                .collect(Collectors.toList());
    }

    /**
     * 檢查目標帳號是否存在，並回傳是否已為好友
     */
    @Transactional(readOnly = true)
    public UserDto checkUserByUsername(String username, Long currentUserId) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("請輸入欲搜尋的帳號");
        }
        String cleanUsername = username.trim().toLowerCase();
        User targetUser = userRepository.findByUsername(cleanUsername)
                .orElseThrow(() -> new IllegalArgumentException("找不到帳號為「" + cleanUsername + "」的使用者，請確認帳號是否正確"));

        if (targetUser.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("不能將自己新增為好友");
        }

        if (!"ACTIVE".equalsIgnoreCase(targetUser.getStatus())) {
            throw new IllegalArgumentException("此帳號已被停用，無法新增為好友");
        }

        boolean isFriend = friendshipRepository.existsByUserIdAndFriendId(currentUserId, targetUser.getId());

        UserDto dto = mapToDto(targetUser);
        dto.setIsFriend(isFriend);
        return dto;
    }

    /**
     * 新增好友（建立雙向好友關聯）
     */
    @Transactional
    public UserDto addFriend(Long currentUserId, String targetUsername) {
        UserDto checked = checkUserByUsername(targetUsername, currentUserId);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("找不到當前使用者"));
        User targetUser = userRepository.findById(checked.getId())
                .orElseThrow(() -> new IllegalArgumentException("找不到目標使用者"));

        // 建立雙向關聯 (A -> B 和 B -> A)
        if (!friendshipRepository.existsByUserIdAndFriendId(currentUserId, targetUser.getId())) {
            Friendship f1 = Friendship.builder()
                    .user(currentUser)
                    .friend(targetUser)
                    .build();
            friendshipRepository.save(f1);
        }

        if (!friendshipRepository.existsByUserIdAndFriendId(targetUser.getId(), currentUserId)) {
            Friendship f2 = Friendship.builder()
                    .user(targetUser)
                    .friend(currentUser)
                    .build();
            friendshipRepository.save(f2);
        }

        log.info("成功建立好友關聯: {} <-> {}", currentUser.getUsername(), targetUser.getUsername());
        UserDto result = mapToDto(targetUser);
        result.setIsFriend(true);
        return result;
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
