package com.talkweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String nickname;
    private boolean hasCustomAvatar;
    private Integer avatarDefaultIcon;
    private boolean isAdmin;
    private boolean mustChangePassword;
    private String status;
    private boolean online;
    private OffsetDateTime createdAt;
}
