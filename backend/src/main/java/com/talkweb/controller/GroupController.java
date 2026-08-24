package com.talkweb.controller;

import com.talkweb.dto.AddGroupMemberRequest;
import com.talkweb.dto.ApiResponse;
import com.talkweb.dto.CreateGroupRequest;
import com.talkweb.dto.GroupDto;
import com.talkweb.security.UserPrincipal;
import com.talkweb.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ApiResponse<GroupDto> createGroup(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody CreateGroupRequest request) {
        GroupDto group = groupService.createGroup(principal.getId(), request);
        return ApiResponse.success("群組建立成功", group);
    }

    @GetMapping
    public ApiResponse<List<GroupDto>> getUserGroups(@AuthenticationPrincipal UserPrincipal principal) {
        List<GroupDto> groups = groupService.getUserGroups(principal.getId());
        return ApiResponse.success(groups);
    }

    @GetMapping("/{id}")
    public ApiResponse<GroupDto> getGroupDetails(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable("id") Long id) {
        GroupDto group = groupService.getGroupDetails(id, principal.getId());
        return ApiResponse.success(group);
    }

    @PutMapping("/{id}")
    public ApiResponse<GroupDto> updateGroup(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable("id") Long id,
                                             @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String announcement = (String) body.get("announcement");
        Integer icon = body.get("icon") != null ? ((Number) body.get("icon")).intValue() : null;

        GroupDto group = groupService.updateGroup(id, principal.getId(), name, announcement, icon);
        return ApiResponse.success("群組資訊更新成功", group);
    }

    @PostMapping("/{id}/members")
    public ApiResponse<Void> addMembers(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable("id") Long id,
                                        @Valid @RequestBody AddGroupMemberRequest request) {
        groupService.addGroupMembers(id, principal.getId(), request);
        return ApiResponse.success("已成功邀請成員", null);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ApiResponse<Void> removeMember(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable("id") Long id,
                                          @PathVariable("userId") Long userId) {
        groupService.removeGroupMember(id, principal.getId(), userId);
        return ApiResponse.success("已成功移除成員 / 退出群組", null);
    }
}
