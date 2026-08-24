package com.talkweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long id;
    private String username;
    private String nickname;
    private boolean hasCustomAvatar;
    private Integer avatarDefaultIcon;
    private boolean isAdmin;
    private boolean mustChangePassword;
}
