package com.talkweb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private Long receiverId; // For 1-on-1 direct message
    private Long groupId;    // For group message

    @NotBlank(message = "訊息內容不得為空")
    private String content;

    @Builder.Default
    private String type = "TEXT"; // TEXT, EMOJI
}
