package com.talkweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDto {
    private Long id;
    private String name;
    private Integer icon;
    private String announcement;
    private Long createdBy;
    private String createdByNickname;
    private OffsetDateTime createdAt;
    private List<GroupMemberDto> members;
    private long unreadCount;
    private ChatMessageDto lastMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMemberDto {
        private Long userId;
        private String username;
        private String nickname;
        private boolean hasCustomAvatar;
        private Integer avatarDefaultIcon;
        private String role; // OWNER, ADMIN, MEMBER
        private boolean online;
        private OffsetDateTime joinedAt;
    }
}
