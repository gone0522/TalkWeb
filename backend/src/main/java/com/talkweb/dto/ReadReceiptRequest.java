package com.talkweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptRequest {
    private List<Long> messageIds;
    private Long senderId; // For 1-on-1: mark all messages from this sender as read
    private Long groupId;  // For group: mark all messages in this group as read
}
