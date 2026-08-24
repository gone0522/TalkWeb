package com.talkweb.service;

import com.talkweb.dto.ChatMessageDto;
import com.talkweb.dto.LinkPreviewDto;
import com.talkweb.dto.ReadReceiptRequest;
import com.talkweb.dto.SendMessageRequest;
import com.talkweb.entity.Group;
import com.talkweb.entity.Message;
import com.talkweb.entity.MessageReadStatus;
import com.talkweb.entity.User;
import com.talkweb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final MessageRepository messageRepository;
    private final MessageReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final LinkPreviewService linkPreviewService;
    private final @Lazy SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageDto sendDirectMessage(Long senderId, SendMessageRequest request) {
        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("接收者 ID 不得為空");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到發送者"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("找不到接收者"));

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent().trim())
                .type(request.getType() != null ? request.getType() : "TEXT")
                .build();

        message = messageRepository.save(message);

        // Sender automatically marks their own message as read
        MessageReadStatus selfRead = MessageReadStatus.builder()
                .message(message)
                .user(sender)
                .build();
        readStatusRepository.save(selfRead);

        ChatMessageDto dto = mapToDto(message, senderId);

        // Push to receiver & sender
        try {
            messagingTemplate.convertAndSendToUser(
                    receiver.getUsername(),
                    "/queue/messages",
                    dto
            );
            messagingTemplate.convertAndSendToUser(
                    sender.getUsername(),
                    "/queue/messages",
                    dto
            );
        } catch (Exception e) {
            log.error("推播私聊訊息失敗: {}", e.getMessage());
        }

        return dto;
    }

    @Transactional
    public ChatMessageDto sendGroupMessage(Long senderId, SendMessageRequest request) {
        if (request.getGroupId() == null) {
            throw new IllegalArgumentException("群組 ID 不得為空");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到發送者"));
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("找不到群組"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), senderId)) {
            throw new SecurityException("您不是該群組成員，無法發送訊息");
        }

        Message message = Message.builder()
                .sender(sender)
                .group(group)
                .content(request.getContent().trim())
                .type(request.getType() != null ? request.getType() : "TEXT")
                .build();

        message = messageRepository.save(message);

        // Sender automatically marks their own message as read
        MessageReadStatus selfRead = MessageReadStatus.builder()
                .message(message)
                .user(sender)
                .build();
        readStatusRepository.save(selfRead);

        ChatMessageDto dto = mapToDto(message, senderId);

        // Push to group topic
        try {
            messagingTemplate.convertAndSend("/topic/group." + group.getId(), dto);
        } catch (Exception e) {
            log.error("推播群組訊息失敗: {}", e.getMessage());
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getDirectMessages(Long userA, Long userB, Long beforeId, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        List<Message> messages = messageRepository.findDirectMessages(userA, userB, beforeId, PageRequest.of(0, pageSize));
        
        // Reverse to chronological order (oldest -> newest) for client view
        List<ChatMessageDto> result = messages.stream()
                .map(m -> mapToDto(m, userA))
                .collect(Collectors.toList());
        Collections.reverse(result);
        return result;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getGroupMessages(Long groupId, Long userId, Long beforeId, int limit) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new SecurityException("您不是該群組成員");
        }

        int pageSize = Math.min(Math.max(limit, 1), 100);
        List<Message> messages = messageRepository.findGroupMessages(groupId, beforeId, PageRequest.of(0, pageSize));

        List<ChatMessageDto> result = messages.stream()
                .map(m -> mapToDto(m, userId))
                .collect(Collectors.toList());
        Collections.reverse(result);
        return result;
    }

    @Transactional
    public void markMessagesAsRead(Long currentUserId, ReadReceiptRequest request) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));

        List<Long> messageIdsToMark = new ArrayList<>();

        if (request.getMessageIds() != null && !request.getMessageIds().isEmpty()) {
            messageIdsToMark.addAll(request.getMessageIds());
        } else if (request.getSenderId() != null) {
            // Mark all unread from this sender
            List<Message> unread = messageRepository.findDirectMessages(request.getSenderId(), currentUserId, null, PageRequest.of(0, 100));
            for (Message m : unread) {
                if (m.getSender().getId().equals(request.getSenderId())) {
                    messageIdsToMark.add(m.getId());
                }
            }
        } else if (request.getGroupId() != null) {
            List<Message> unread = messageRepository.findGroupMessages(request.getGroupId(), null, PageRequest.of(0, 100));
            for (Message m : unread) {
                if (!m.getSender().getId().equals(currentUserId)) {
                    messageIdsToMark.add(m.getId());
                }
            }
        }

        List<Long> newlyMarkedMessageIds = new ArrayList<>();
        for (Long msgId : messageIdsToMark) {
            if (!readStatusRepository.existsByMessageIdAndUserId(msgId, currentUserId)) {
                messageRepository.findById(msgId).ifPresent(msg -> {
                    MessageReadStatus status = MessageReadStatus.builder()
                            .message(msg)
                            .user(currentUser)
                            .readAt(OffsetDateTime.now())
                            .build();
                    readStatusRepository.save(status);
                    newlyMarkedMessageIds.add(msgId);
                });
            }
        }

        if (!newlyMarkedMessageIds.isEmpty()) {
            Map<String, Object> payload = Map.of(
                    "readByUserId", currentUserId,
                    "messageIds", newlyMarkedMessageIds,
                    "senderId", request.getSenderId() != null ? request.getSenderId() : -1L,
                    "groupId", request.getGroupId() != null ? request.getGroupId() : -1L
            );

            // Broadcast read receipt
            if (request.getSenderId() != null) {
                userRepository.findById(request.getSenderId()).ifPresent(sender -> {
                    messagingTemplate.convertAndSendToUser(sender.getUsername(), "/queue/read-receipts", payload);
                });
            }
            if (request.getGroupId() != null) {
                messagingTemplate.convertAndSend("/topic/group." + request.getGroupId() + ".read", payload);
            }
        }
    }

    public ChatMessageDto mapToDto(Message message, Long viewerUserId) {
        User sender = message.getSender();
        boolean read = false;
        long readCount = 0;

        if (message.getReceiver() != null) {
            // Direct message: check if receiver has read it
            read = readStatusRepository.existsByMessageIdAndUserId(message.getId(), message.getReceiver().getId());
        } else if (message.getGroup() != null) {
            readCount = readStatusRepository.countReadCountByMessageId(message.getId());
        }

        // Link Preview resolution
        LinkPreviewDto preview = null;
        String firstUrl = linkPreviewService.extractFirstUrl(message.getContent());
        if (firstUrl != null) {
            preview = linkPreviewService.fetchPreview(firstUrl);
        }

        return ChatMessageDto.builder()
                .id(message.getId())
                .senderId(sender.getId())
                .senderUsername(sender.getUsername())
                .senderNickname(sender.getNickname())
                .senderHasCustomAvatar(sender.getAvatarData() != null && sender.getAvatarData().length > 0)
                .senderDefaultAvatarIcon(sender.getAvatarDefaultIcon())
                .receiverId(message.getReceiver() != null ? message.getReceiver().getId() : null)
                .groupId(message.getGroup() != null ? message.getGroup().getId() : null)
                .content(message.getContent())
                .type(message.getType())
                .read(read)
                .readCount(readCount)
                .createdAt(message.getCreatedAt())
                .linkPreview(preview)
                .build();
    }
}
