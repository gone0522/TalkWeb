package com.talkweb.service;

import com.talkweb.dto.AddGroupMemberRequest;
import com.talkweb.dto.ChatMessageDto;
import com.talkweb.dto.CreateGroupRequest;
import com.talkweb.dto.GroupDto;
import com.talkweb.entity.Group;
import com.talkweb.entity.GroupMember;
import com.talkweb.entity.Message;
import com.talkweb.entity.User;
import com.talkweb.repository.GroupMemberRepository;
import com.talkweb.repository.GroupRepository;
import com.talkweb.repository.MessageRepository;
import com.talkweb.repository.UserRepository;
import com.talkweb.websocket.PresenceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final PresenceManager presenceManager;

    @Transactional
    public GroupDto createGroup(Long currentUserId, CreateGroupRequest request) {
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));

        Group group = Group.builder()
                .name(request.getName().trim())
                .icon(request.getIcon() != null ? request.getIcon() : 1)
                .announcement(request.getAnnouncement())
                .createdBy(creator)
                .members(new ArrayList<>())
                .build();

        Group savedGroup = groupRepository.save(group);

        // Add creator as OWNER
        GroupMember ownerMember = GroupMember.builder()
                .group(savedGroup)
                .user(creator)
                .role("OWNER")
                .build();
        groupMemberRepository.save(ownerMember);

        // Add other invited members
        Set<Long> uniqueMemberIds = new HashSet<>(request.getMemberIds() != null ? request.getMemberIds() : List.of());
        uniqueMemberIds.remove(currentUserId);

        for (Long memberId : uniqueMemberIds) {
            userRepository.findById(memberId).ifPresent(user -> {
                GroupMember member = GroupMember.builder()
                        .group(savedGroup)
                        .user(user)
                        .role("MEMBER")
                        .build();
                groupMemberRepository.save(member);
            });
        }

        return getGroupDetails(savedGroup.getId(), currentUserId);
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getUserGroups(Long userId) {
        List<Group> groups = groupRepository.findGroupsByUserId(userId);
        return groups.stream()
                .map(g -> mapToGroupDto(g, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupDto getGroupDetails(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("找不到群組"));

        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
        if (!isMember) {
            throw new SecurityException("您不是該群組的成員");
        }

        return mapToGroupDto(group, userId);
    }

    @Transactional
    public GroupDto updateGroup(Long groupId, Long currentUserId, String name, String announcement, Integer icon) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("找不到群組"));

        GroupMember currentMember = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new SecurityException("您不是該群組的成員"));

        if (!"OWNER".equalsIgnoreCase(currentMember.getRole()) && !"ADMIN".equalsIgnoreCase(currentMember.getRole())) {
            throw new SecurityException("僅群組管理員或群主可修改群組資訊");
        }

        if (name != null && !name.trim().isEmpty()) {
            group.setName(name.trim());
        }
        if (announcement != null) {
            group.setAnnouncement(announcement.trim());
        }
        if (icon != null) {
            group.setIcon(icon);
        }

        group = groupRepository.save(group);
        return mapToGroupDto(group, currentUserId);
    }

    @Transactional
    public void addGroupMembers(Long groupId, Long currentUserId, AddGroupMemberRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("找不到群組"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)) {
            throw new SecurityException("您不是該群組的成員");
        }

        for (Long memberId : request.getUserIds()) {
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, memberId)) {
                userRepository.findById(memberId).ifPresent(user -> {
                    GroupMember member = GroupMember.builder()
                            .group(group)
                            .user(user)
                            .role("MEMBER")
                            .build();
                    groupMemberRepository.save(member);
                });
            }
        }
    }

    @Transactional
    public void removeGroupMember(Long groupId, Long currentUserId, Long targetUserId) {
        GroupMember currentMember = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new SecurityException("您不是該群組的成員"));

        // If leaving themselves or if admin/owner kicking someone else
        boolean isSelf = currentUserId.equals(targetUserId);
        boolean isPrivileged = "OWNER".equalsIgnoreCase(currentMember.getRole()) || "ADMIN".equalsIgnoreCase(currentMember.getRole());

        if (!isSelf && !isPrivileged) {
            throw new SecurityException("您沒有權限移除此成員");
        }

        groupMemberRepository.deleteByGroupIdAndUserId(groupId, targetUserId);
    }

    public GroupDto mapToGroupDto(Group group, Long currentUserId) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
        List<GroupDto.GroupMemberDto> memberDtos = members.stream().map(m -> {
            User u = m.getUser();
            return GroupDto.GroupMemberDto.builder()
                    .userId(u.getId())
                    .username(u.getUsername())
                    .nickname(u.getNickname())
                    .hasCustomAvatar(u.getAvatarData() != null && u.getAvatarData().length > 0)
                    .avatarDefaultIcon(u.getAvatarDefaultIcon())
                    .role(m.getRole())
                    .online(presenceManager != null && presenceManager.isUserOnline(u.getId()))
                    .joinedAt(m.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());

        long unread = messageRepository.countUnreadGroupMessages(group.getId(), currentUserId);

        List<Message> lastMsgs = messageRepository.findGroupMessages(group.getId(), null, PageRequest.of(0, 1));
        ChatMessageDto lastMsgDto = null;
        if (!lastMsgs.isEmpty()) {
            Message m = lastMsgs.get(0);
            lastMsgDto = ChatMessageDto.builder()
                    .id(m.getId())
                    .senderId(m.getSender().getId())
                    .senderUsername(m.getSender().getUsername())
                    .senderNickname(m.getSender().getNickname())
                    .content(m.getContent())
                    .type(m.getType())
                    .createdAt(m.getCreatedAt())
                    .build();
        }

        return GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .icon(group.getIcon())
                .announcement(group.getAnnouncement())
                .createdBy(group.getCreatedBy() != null ? group.getCreatedBy().getId() : null)
                .createdByNickname(group.getCreatedBy() != null ? group.getCreatedBy().getNickname() : null)
                .createdAt(group.getCreatedAt())
                .members(memberDtos)
                .unreadCount(unread)
                .lastMessage(lastMsgDto)
                .build();
    }
}
