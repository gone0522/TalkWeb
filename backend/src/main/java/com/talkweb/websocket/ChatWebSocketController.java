package com.talkweb.websocket;

import com.talkweb.dto.ReadReceiptRequest;
import com.talkweb.dto.SendMessageRequest;
import com.talkweb.security.UserPrincipal;
import com.talkweb.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.sendDirect")
    public void handleDirectMessage(@Payload SendMessageRequest request, Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            chatMessageService.sendDirectMessage(userPrincipal.getId(), request);
        }
    }

    @MessageMapping("/chat.sendGroup")
    public void handleGroupMessage(@Payload SendMessageRequest request, Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            chatMessageService.sendGroupMessage(userPrincipal.getId(), request);
        }
    }

    @MessageMapping("/chat.read")
    public void handleReadReceipt(@Payload ReadReceiptRequest request, Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            chatMessageService.markMessagesAsRead(userPrincipal.getId(), request);
        }
    }
}
