package com.talkweb.controller;

import com.talkweb.dto.ApiResponse;
import com.talkweb.dto.ChatMessageDto;
import com.talkweb.dto.ReadReceiptRequest;
import com.talkweb.dto.SendMessageRequest;
import com.talkweb.security.UserPrincipal;
import com.talkweb.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatMessageService messageService;

    @PostMapping("/direct")
    public ApiResponse<ChatMessageDto> sendDirectMessage(@AuthenticationPrincipal UserPrincipal principal,
                                                         @Valid @RequestBody SendMessageRequest request) {
        ChatMessageDto msg = messageService.sendDirectMessage(principal.getId(), request);
        return ApiResponse.success(msg);
    }

    @PostMapping("/group")
    public ApiResponse<ChatMessageDto> sendGroupMessage(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody SendMessageRequest request) {
        ChatMessageDto msg = messageService.sendGroupMessage(principal.getId(), request);
        return ApiResponse.success(msg);
    }

    @GetMapping("/direct/{userId}")
    public ApiResponse<List<ChatMessageDto>> getDirectMessages(@AuthenticationPrincipal UserPrincipal principal,
                                                               @PathVariable("userId") Long userId,
                                                               @RequestParam(value = "before", required = false) Long beforeId,
                                                               @RequestParam(value = "limit", defaultValue = "50") int limit) {
        List<ChatMessageDto> messages = messageService.getDirectMessages(principal.getId(), userId, beforeId, limit);
        return ApiResponse.success(messages);
    }

    @GetMapping("/group/{groupId}")
    public ApiResponse<List<ChatMessageDto>> getGroupMessages(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable("groupId") Long groupId,
                                                              @RequestParam(value = "before", required = false) Long beforeId,
                                                              @RequestParam(value = "limit", defaultValue = "50") int limit) {
        List<ChatMessageDto> messages = messageService.getGroupMessages(groupId, principal.getId(), beforeId, limit);
        return ApiResponse.success(messages);
    }

    @PostMapping("/read")
    public ApiResponse<Void> markAsRead(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestBody ReadReceiptRequest request) {
        messageService.markMessagesAsRead(principal.getId(), request);
        return ApiResponse.success(null);
    }
}
