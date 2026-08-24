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
public class ChatMessageDto {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderNickname;
    private boolean senderHasCustomAvatar;
    private Integer senderDefaultAvatarIcon;

    private Long receiverId;
    private Long groupId;

    private String content;
    private String type; // TEXT, EMOJI, SYSTEM
    private boolean read; // For direct messages: whether recipient has read it
    private long readCount; // For group messages
    private OffsetDateTime createdAt;

    // Optional link preview attached if detected
    private LinkPreviewDto linkPreview;
}
